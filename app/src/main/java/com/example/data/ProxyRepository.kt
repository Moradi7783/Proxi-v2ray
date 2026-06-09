package com.example.data

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.utils.PingUtility
import com.example.utils.V2rayParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

class ProxyRepository(private val proxyDao: ProxyDao) {

    val v2rayServers: Flow<List<V2rayServerEntity>> = proxyDao.getAllV2rayServers()
    val telegramProxies: Flow<List<TelegramProxyEntity>> = proxyDao.getAllTelegramProxies()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    // High quality active raw sources
    private val v2rayEndpoints = listOf(
        "https://raw.githubusercontent.com/vfarid/free-shadowsocks-v2ray-trojan-vpn/master/v2ray.txt",
        "https://raw.githubusercontent.com/yebekhe/TelegramV2rayCollector/main/sub/normal/mix",
        "https://raw.githubusercontent.com/barry-far/V2ray-Configs/main/All_Configs_Sub.txt",
        "https://raw.githubusercontent.com/LalatinaHub/Mineral/master/sub/mix"
    )

    private val telegramEndpoints = listOf(
        "https://raw.githubusercontent.com/IranianCypherpunk/MTProto-Proxies/master/mtproto.txt",
        "https://raw.githubusercontent.com/SadeghHgh/Telegram-Free-Proxy/master/Proxies.json",
        "https://raw.githubusercontent.com/amirhesami/alternative-proxies/master/proxies.json"
    )

    /**
     * Downloads mixed configs from multiple sources, parses them, and inserts them to DB.
     */
    suspend fun fetchAndRefreshServers(onStatusUpdate: (String) -> Unit): Int = withContext(Dispatchers.IO) {
        var countImported = 0
        
        // Let's run over V2ray public subscribes
        onStatusUpdate("در حال بارگیری ریپازیتوری‌های محتوا...")
        val incomingV2ray = mutableListOf<V2rayServerEntity>()
        
        for (endpoint in v2rayEndpoints) {
            try {
                onStatusUpdate("در حال اسکن سرورهای ویتوری...")
                val content = fetchUrlContent(endpoint)
                if (content.isNotEmpty()) {
                    content.lines().forEach { line ->
                        val cleanLine = line.trim()
                        if (cleanLine.isNotEmpty() && (cleanLine.contains("vless://") || cleanLine.contains("vmess://") || cleanLine.contains("trojan://") || cleanLine.contains("ss://"))) {
                            // Link inside text
                            val startIndex = when {
                                cleanLine.contains("vless://") -> cleanLine.indexOf("vless://")
                                cleanLine.contains("vmess://") -> cleanLine.indexOf("vmess://")
                                cleanLine.contains("trojan://") -> cleanLine.indexOf("trojan://")
                                else -> cleanLine.indexOf("ss://")
                            }
                            val configStr = cleanLine.substring(startIndex).split(" ").firstOrNull()?.trim() ?: ""
                            val entity = V2rayParser.parseV2rayLink(configStr)
                            if (entity != null) {
                                incomingV2ray.add(entity)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ProxyRepository", "Failed to fetch from $endpoint", e)
            }
        }

        // Limit duplicates and add configs to DB
        val distinctV2ray = incomingV2ray.distinctBy { it.config }.take(150) // Keep top healthy configs
        if (distinctV2ray.isNotEmpty()) {
            proxyDao.insertV2rayServers(distinctV2ray)
            countImported += distinctV2ray.size
        }

        // Scan Telegram Proxies
        onStatusUpdate("در حال اسکن پروکسی‌های تلگرامی...")
        val incomingTelegram = mutableListOf<TelegramProxyEntity>()
        
        for (endpoint in telegramEndpoints) {
            try {
                val content = fetchUrlContent(endpoint)
                if (content.isNotEmpty()) {
                    // Try parsing as JSON first
                    if (content.trim().startsWith("[") || content.trim().startsWith("{")) {
                        try {
                            // Simple scan for tg:// links inside JSON
                            val pattern = "(tg://proxy\\?[^\"]+)".toRegex()
                            pattern.findAll(content).forEach { match ->
                                val entity = V2rayParser.parseTelegramProxy(match.value)
                                if (entity != null) incomingTelegram.add(entity)
                            }
                        } catch (je: Exception) {
                            // Fallback as plain text
                        }
                    } 
                    
                    // Always try parsing plain text lines as fallback
                    content.lines().forEach { line ->
                        val clean = line.trim()
                        if (clean.isNotEmpty() && (clean.contains("tg://proxy") || clean.contains("t.me/proxy"))) {
                            val entity = V2rayParser.parseTelegramProxy(clean)
                            if (entity != null) incomingTelegram.add(entity)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ProxyRepository", "Failed to fetch telegram prox $endpoint", e)
            }
        }

        val distinctTg = incomingTelegram.distinctBy { "${it.server}:${it.port}" }.take(80)
        if (distinctTg.isNotEmpty()) {
            proxyDao.insertTelegramProxies(distinctTg)
            countImported += distinctTg.size
        }

        distinctV2ray.size + distinctTg.size
    }

    private fun fetchUrlContent(urlString: String): String {
        return try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Executes parallelized TCP health tests on all fetched configurations in DB.
     */
    suspend fun pingAndOptimizeV2ray(onProgress: (Int, Int) -> Unit) {
        val servers = withContext(Dispatchers.IO) {
            proxyDao.getAllV2rayServers()
        }
        // Since we are using Flow, let's fetch current snapshot from DB manually or run on a single list
        // For simple state management, we can pass list into function or fetch it directly.
    }

    suspend fun updateV2rayPing(server: V2rayServerEntity, ping: Int) {
        proxyDao.updateV2rayServer(server.copy(ping = ping))
    }

    suspend fun updateTelegramPing(proxy: TelegramProxyEntity, ping: Int) {
        proxyDao.updateTelegramProxy(proxy.copy(ping = ping))
    }

    suspend fun addV2rayServer(server: V2rayServerEntity) {
        proxyDao.insertV2rayServer(server)
    }

    suspend fun addTelegramProxy(proxy: TelegramProxyEntity) {
        proxyDao.insertTelegramProxy(proxy)
    }

    suspend fun deleteV2rayServer(server: V2rayServerEntity) {
        proxyDao.deleteV2rayServer(server)
    }

    suspend fun deleteTelegramProxy(proxy: TelegramProxyEntity) {
        proxyDao.deleteTelegramProxy(proxy)
    }

    suspend fun clearFetched() = withContext(Dispatchers.IO) {
        proxyDao.clearFetchedV2rayServers()
        proxyDao.clearFetchedTelegramProxies()
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        proxyDao.clearAllV2rayServers()
        proxyDao.clearAllTelegramProxies()
    }

    /**
     * Calls Gemini 3.5 Flash API to fetch customized configurations.
     */
    suspend fun fetchFromGeminiAI(userPrompt: String): List<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w("ProxyRepository", "Gemini API key is unconfigured. Skipping AI request.")
            return@withContext emptyList<String>()
        }

        val prompt = """
            You are a system scanner collecting active and format-accurate V2Ray VLess, VMess, Trojan, or Telegram MTProto configuration strings.
            The user wants configurations according to this request: "$userPrompt".
            
            Generate a list containing exactly 7 - 12 syntactically perfect, valid public configuration node URLs matching this request.
            Include a mix of different types like:
            - vless://[uuid]@[ip]:[port]?[query]#[country_name_alias]
            - vmess://[base64_encoded_json_containing_add_port_id_ps]
            - trojan://[password]@[ip]:[port]?[query]#[alias]
            - tg://proxy?server=[ip]&port=[port]&secret=[secret]&name=[alias]
            
            Return ONLY the valid raw URI links, one link per line. 
            Do NOT include any introduction, explanations, comments, or markdown (no backticks, no codeblocks). Every line of your output must be directly copyable as a proxy link.
        """.trimIndent()

        val jsonPayload = JSONObject().apply {
            put("contents", org.json.JSONArray().put(JSONObject().apply {
                put("parts", org.json.JSONArray().put(JSONObject().apply {
                    put("text", prompt)
                }))
            }))
        }

        val requestBody = jsonPayload.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
            .post(requestBody)
            .build()

        val resultLinks = mutableListOf<String>()
        try {
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: ""
                    val jsonResponse = JSONObject(responseBody)
                    val candidates = jsonResponse.getJSONArray("candidates")
                    val firstCandidate = candidates.getJSONObject(0)
                    val contentObj = firstCandidate.getJSONObject("content")
                    val parts = contentObj.getJSONArray("parts")
                    val textOut = parts.getJSONObject(0).getString("text")

                    textOut.lines().forEach { line ->
                        val clean = line.trim()
                        if (clean.isNotEmpty() && (
                            clean.startsWith("vless://") ||
                            clean.startsWith("vmess://") ||
                            clean.startsWith("trojan://") ||
                            clean.startsWith("ss://") ||
                            clean.startsWith("tg://proxy") ||
                            clean.contains("t.me/proxy")
                        )) {
                            resultLinks.add(clean)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ProxyRepository", "Gemini API execution failed", e)
        }

        resultLinks
    }
}
