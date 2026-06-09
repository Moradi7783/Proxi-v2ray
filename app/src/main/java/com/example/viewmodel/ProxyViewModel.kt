package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.*
import com.example.utils.PingUtility
import com.example.utils.V2rayParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProxyViewModel(application: Application) : AndroidViewModel(application) {

    private val database = ProxyDatabase.getDatabase(application)
    private val repository = ProxyRepository(database.proxyDao())

    // --- State Managers ---
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTab = MutableStateFlow(0) // 0: V2ray, 1: Telegram, 2: AI Assist, 3: Add Config
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _selectedCountryFilter = MutableStateFlow("ALL") // "ALL", "US", "DE", "IR", etc.
    val selectedCountryFilter: StateFlow<String> = _selectedCountryFilter.asStateFlow()

    // --- AI Generator States ---
    private val _aiResultList = MutableStateFlow<List<String>>(emptyList())
    val aiResultList: StateFlow<List<String>> = _aiResultList.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    // Raw Room database streams
    private val _allV2ray = repository.v2rayServers
    private val _allTelegram = repository.telegramProxies

    // Filtered / Searched V2ray Servers
    val v2rayServers: StateFlow<List<V2rayServerEntity>> = combine(
        _allV2ray,
        _searchQuery,
        _selectedCountryFilter
    ) { list, query, country ->
        var filtered = list
        if (query.isNotEmpty()) {
            filtered = filtered.filter { 
                it.name.contains(query, ignoreCase = true) || 
                it.address.contains(query, ignoreCase = true) || 
                it.type.contains(query, ignoreCase = true)
            }
        }
        if (country != "ALL") {
            filtered = filtered.filter { it.countryCode == country }
        }
        filtered
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered / Searched Telegram Proxies
    val telegramProxies: StateFlow<List<TelegramProxyEntity>> = combine(
        _allTelegram,
        _searchQuery,
        _selectedCountryFilter
    ) { list, query, country ->
        var filtered = list
        if (query.isNotEmpty()) {
            filtered = filtered.filter { 
                it.name.contains(query, ignoreCase = true) || 
                it.server.contains(query, ignoreCase = true)
            }
        }
        if (country != "ALL") {
            filtered = filtered.filter { it.countryCode == country }
        }
        filtered
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Tracks all active countries in the cached database for dynamic dynamic filters
    val uniqueCountries: StateFlow<List<String>> = combine(_allV2ray, _allTelegram) { vLists, tLists ->
        val vCodes = vLists.map { it.countryCode }
        val tCodes = tLists.map { it.countryCode }
        (vCodes + tCodes).distinct().filter { it != "UN" }.sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Run first retrieval if database is completely empty
        viewModelScope.launch {
            _allV2ray.first().let {
                if (it.isEmpty()) {
                    triggerDailyRefresh()
                }
            }
        }
    }

    fun setTab(index: Int) {
        _selectedTab.value = index
    }

    fun setCountryFilter(country: String) {
        _selectedCountryFilter.value = country
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Downloads and parses all configuration files
     */
    fun triggerDailyRefresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            _statusMessage.value = "در حال بارگیری کانفینگ‌های امروز فیلترشکن..."
            try {
                val totalLoaded = repository.fetchAndRefreshServers { status ->
                    _statusMessage.value = status
                }
                _statusMessage.value = "مجموعاً $totalLoaded سرور با موفقیت دریافت شد"
                
                // Automatically run ping optimization to separate active/dead configurations
                pingOptimizeAll()
            } catch (e: Exception) {
                _statusMessage.value = "خطا در بروزرسانی خودکار سرورها"
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    /**
     * Optimizes V2ray configurations by pinging healthy addresses
     */
    fun pingOptimizeAll() {
        viewModelScope.launch {
            val v2rayList = v2rayServers.value
            _statusMessage.value = "در حال اندازه‌گیری سرعت سرورها..."
            
            // Ping elements in parallel batches
            v2rayList.chunked(15).forEach { batch ->
                batch.onEach { server ->
                    launch(Dispatchers.Default) {
                        val ping = PingUtility.pingAddress(server.address, server.port)
                        repository.updateV2rayPing(server, ping)
                    }
                }
            }

            val tgList = telegramProxies.value
            tgList.chunked(15).forEach { batch ->
                batch.onEach { proxy ->
                    launch(Dispatchers.Default) {
                        val ping = PingUtility.pingAddress(proxy.server, proxy.port)
                        repository.updateTelegramPing(proxy, ping)
                    }
                }
            }
            
            _statusMessage.value = "بهینه‌سازی پینگ سرورها با موفقیت انجام شد"
        }
    }

    /**
     * Triggers clean local lookup via direct Gemini REST endpoint
     */
    fun searchWithGeminiAI(userPrompt: String) {
        if (_isAiLoading.value) return
        viewModelScope.launch {
            _isAiLoading.value = true
            _statusMessage.value = "هوش مصنوعی در حال شناسایی سرورهای سالم..."
            _aiResultList.value = emptyList()
            try {
                val results = repository.fetchFromGeminiAI(userPrompt)
                _aiResultList.value = results
                if (results.isEmpty()) {
                    val hasKey = BuildConfig.GEMINI_API_KEY.isNotEmpty() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY"
                    if (!hasKey) {
                        _statusMessage.value = "کلید API هوش مصنوعی در تنظیمات برنامه ثبت نشده است"
                    } else {
                        _statusMessage.value = "هوش مصنوعی پاسخی ارسال نکرد، لطفاً دوباره تلاش کنید"
                    }
                } else {
                    _statusMessage.value = "دریافت موفقیت‌آمیز ${results.size} سرور از هوش مصنوعی"
                    
                    // Automatically add results to client list so user can test them
                    results.forEach { link ->
                        val vEntity = V2rayParser.parseV2rayLink(link)
                        if (vEntity != null) {
                            repository.addV2rayServer(vEntity.copy(isUserAdded = true, name = "[هوش مصنوعی] " + vEntity.name))
                        } else {
                            val tgEntity = V2rayParser.parseTelegramProxy(link)
                            if (tgEntity != null) {
                                repository.addTelegramProxy(tgEntity.copy(isUserAdded = true, name = "[هوش مصنوعی] " + tgEntity.name))
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _statusMessage.value = "خطا در ارتباط با سرور هوش مصنوعی"
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    /**
     * Checks if Gemini API Key is configured in settings secret panel
     */
    fun isGeminiKeyConfigured(): Boolean {
        return BuildConfig.GEMINI_API_KEY.isNotEmpty() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY"
    }

    // --- User Crud Controls ---
    fun toggleV2rayFavorite(server: V2rayServerEntity) {
        viewModelScope.launch {
            repository.updateV2rayPing(server, server.ping) // Can be used as updates helper
            val updated = server.copy(isFavorite = !server.isFavorite)
            database.proxyDao().updateV2rayServer(updated)
        }
    }

    fun toggleTelegramFavorite(proxy: TelegramProxyEntity) {
        viewModelScope.launch {
            val updated = proxy.copy(isFavorite = !proxy.isFavorite)
            database.proxyDao().updateTelegramProxy(updated)
        }
    }

    fun addManualConfig(configStr: String): Boolean {
        val clean = configStr.trim()
        if (clean.startsWith("tg://") || clean.contains("t.me/proxy")) {
            val entity = V2rayParser.parseTelegramProxy(clean)
            if (entity != null) {
                viewModelScope.launch {
                    repository.addTelegramProxy(entity.copy(isUserAdded = true))
                }
                return true
            }
        } else {
            val entity = V2rayParser.parseV2rayLink(clean)
            if (entity != null) {
                viewModelScope.launch {
                    repository.addV2rayServer(entity.copy(isUserAdded = true))
                }
                return true
            }
        }
        return false
    }

    fun clearAllServers() {
        viewModelScope.launch {
            repository.clearAll()
            _statusMessage.value = "لیست سرورها با موفقیت پاک شد"
        }
    }

    fun deleteV2rayServer(server: V2rayServerEntity) {
        viewModelScope.launch {
            repository.deleteV2rayServer(server)
        }
    }

    fun deleteTelegramProxy(proxy: TelegramProxyEntity) {
        viewModelScope.launch {
            repository.deleteTelegramProxy(proxy)
        }
    }
}
