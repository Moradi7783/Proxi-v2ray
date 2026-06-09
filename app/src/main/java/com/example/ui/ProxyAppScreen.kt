package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.TelegramProxyEntity
import com.example.data.V2rayServerEntity
import com.example.ui.theme.*
import com.example.utils.V2rayParser
import com.example.viewmodel.ProxyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProxyAppScreen(
    viewModel: ProxyViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val selectedCountry by viewModel.selectedCountryFilter.collectAsStateWithLifecycle()
    
    val v2rayServers by viewModel.v2rayServers.collectAsStateWithLifecycle()
    val telegramProxies by viewModel.telegramProxies.collectAsStateWithLifecycle()
    val uniqueCountries by viewModel.uniqueCountries.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBg),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "SmartProxy ",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 18.sp,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "HUB",
                                fontWeight = FontWeight.ExtraBold,
                                color = CyberPrimary,
                                fontSize = 18.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "هوشمند • بروزرسانی خودکار",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = CyberTextSecondary,
                            letterSpacing = 0.5.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = CyberBg,
                    titleContentColor = CyberTextPrimary
                ),
                actions = {
                    IconButton(
                        onClick = { viewModel.triggerDailyRefresh() },
                        modifier = Modifier.testTag("refresh_action_button")
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                color = CyberPrimary,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = CyberPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = CyberSurface.copy(alpha = 0.95f),
                tonalElevation = 8.dp,
                windowInsets = WindowInsets.navigationBars
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { viewModel.setTab(0) },
                    icon = { Icon(Icons.Default.FlashOn, "V2Ray") },
                    label = { Text("ویتوری V2ray", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberBg,
                        selectedTextColor = CyberPrimary,
                        indicatorColor = CyberPrimary,
                        unselectedIconColor = CyberTextSecondary,
                        unselectedTextColor = CyberTextSecondary
                    ),
                    modifier = Modifier.testTag("v2ray_nav_tab")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { viewModel.setTab(1) },
                    icon = { Icon(Icons.Default.Send, "Telegram") },
                    label = { Text("پروکسی تلگرام", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberBg,
                        selectedTextColor = CyberPrimary,
                        indicatorColor = CyberPrimary,
                        unselectedIconColor = CyberTextSecondary,
                        unselectedTextColor = CyberTextSecondary
                    ),
                    modifier = Modifier.testTag("telegram_nav_tab")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { viewModel.setTab(2) },
                    icon = { Icon(Icons.Default.AutoAwesome, "AI Assist") },
                    label = { Text("جستجوگر AI", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberBg,
                        selectedTextColor = CyberPrimary,
                        indicatorColor = CyberPrimary,
                        unselectedIconColor = CyberTextSecondary,
                        unselectedTextColor = CyberTextSecondary
                    ),
                    modifier = Modifier.testTag("ai_nav_tab")
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { viewModel.setTab(3) },
                    icon = { Icon(Icons.Default.AddCircleOutline, "Add Manual") },
                    label = { Text("ثبت دستی کانفینگ", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberBg,
                        selectedTextColor = CyberPrimary,
                        indicatorColor = CyberPrimary,
                        unselectedIconColor = CyberTextSecondary,
                        unselectedTextColor = CyberTextSecondary
                    ),
                    modifier = Modifier.testTag("add_manual_nav_tab")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CyberBg)
                .padding(innerPadding)
        ) {
            // Quick Stats / AI Status Chip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CyberPrimary.copy(alpha = 0.08f))
                        .border(1.dp, CyberPrimary.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        // Pulse Dot
                        val transition = rememberInfiniteTransition(label = "pulse")
                        val scale by transition.animateFloat(
                            initialValue = 0.4f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ), label = "pulse_scale"
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(CyberTertiary.copy(alpha = scale))
                        )
                        
                        Text(
                            text = if (statusMessage.isNotEmpty()) statusMessage else "هوشمند • آماده اتصال به سرورها",
                            color = CyberTertiary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("status_indicator_text"),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    val totalActive = v2rayServers.size + telegramProxies.size
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberPrimary.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$totalActive سرور",
                            color = CyberTextAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Quick Controller Card (Only shown in V2ray and Telegram views)
            if (selectedTab == 0 || selectedTab == 1) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, CyberSurfaceGlass),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (selectedTab == 0) "سرورهای فعال ویتوری (${v2rayServers.size})" else "پروکسی‌های تلگرام (${telegramProxies.size})",
                                fontWeight = FontWeight.Bold,
                                color = CyberTextPrimary,
                                fontSize = 14.sp
                            )
                            Row {
                                TextButton(
                                    onClick = { viewModel.pingOptimizeAll() },
                                    colors = ButtonDefaults.textButtonColors(contentColor = CyberPrimary),
                                    modifier = Modifier.testTag("speed_optimize_button")
                                ) {
                                    Icon(Icons.Default.Bolt, "Optimize Speed", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تست سرعت", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                TextButton(
                                    onClick = { viewModel.clearAllServers() },
                                    colors = ButtonDefaults.textButtonColors(contentColor = PingRed),
                                    modifier = Modifier.testTag("clear_all_button")
                                ) {
                                    Icon(Icons.Default.DeleteSweep, "Clear Items", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("حذف لیست", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Search Input (Pill Shaped Elegant Input)
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("جستجو در بین سرورها...", color = CyberTextSecondary, fontSize = 13.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("main_search_input"),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberPrimary,
                                unfocusedBorderColor = CyberSurfaceGlass,
                                focusedTextColor = CyberTextPrimary,
                                unfocusedTextColor = CyberTextPrimary,
                                focusedContainerColor = CyberBg.copy(alpha = 0.8f),
                                unfocusedContainerColor = CyberBg.copy(alpha = 0.6f)
                            ),
                            leadingIcon = { Icon(Icons.Default.Search, null, tint = CyberPrimary, modifier = Modifier.size(18.dp)) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(Icons.Outlined.Cancel, null, tint = CyberTextSecondary, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Dynamic Countries Filter Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CountryFilterChip(
                                label = "همه",
                                emoji = "🌐",
                                isSelected = selectedCountry == "ALL",
                                onClick = { viewModel.setCountryFilter("ALL") }
                            )

                            uniqueCountries.forEach { countryCode ->
                                val emoji = V2rayParser.getFlagEmoji(countryCode)
                                CountryFilterChip(
                                    label = countryCode,
                                    emoji = emoji,
                                    isSelected = selectedCountry == countryCode,
                                    onClick = { viewModel.setCountryFilter(countryCode) }
                                )
                            }
                        }
                    }
                }
            }

            // Main Content Area based on Selected Tab
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    0 -> V2rayServersListView(
                        servers = v2rayServers,
                        onCopy = { config ->
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("V2ray Config", config))
                            Toast.makeText(context, "کانفینگ با موفقیت کپی شد! آماده استفاده در برنامه فیلترشکن.", Toast.LENGTH_SHORT).show()
                        },
                        onFavoriteToggle = { server -> viewModel.toggleV2rayFavorite(server) },
                        onDelete = { server -> viewModel.deleteV2rayServer(server) }
                    )
                    1 -> TelegramProxiesListView(
                        proxies = telegramProxies,
                        onConnect = { proxy ->
                            try {
                                val intentUri = Uri.parse("tg://proxy?server=${proxy.server}&port=${proxy.port}&secret=${proxy.secret}")
                                val intent = Intent(Intent.ACTION_VIEW, intentUri)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "تلگرام روی دستگاه شما نصب نیست!", Toast.LENGTH_LONG).show()
                            }
                        },
                        onCopy = { proxy ->
                            val link = "tg://proxy?server=${proxy.server}&port=${proxy.port}&secret=${proxy.secret}"
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Telegram Proxy", link))
                            Toast.makeText(context, "پروکسی کپی شد! می‌توانید آن را در تلگرام خود جای‌گذاری (Paste) کنید.", Toast.LENGTH_SHORT).show()
                        },
                        onFavoriteToggle = { proxy -> viewModel.toggleTelegramFavorite(proxy) },
                        onDelete = { proxy -> viewModel.deleteTelegramProxy(proxy) }
                    )
                    2 -> AiAssistView(
                        viewModel = viewModel,
                        onCopyAll = { list ->
                            val joined = list.joinToString("\n")
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("AI Proxies", joined))
                            Toast.makeText(context, "تمام کانفینگ‌های دریافت شده کپی شدند!", Toast.LENGTH_SHORT).show()
                        }
                    )
                    3 -> AddManualConfigView(
                        onAdd = { config ->
                            val success = viewModel.addManualConfig(config)
                            if (success) {
                                Toast.makeText(context, "کانفینگ با موفقیت اضافه شد!", Toast.LENGTH_SHORT).show()
                                viewModel.setTab(0) // Go to listing
                            } else {
                                Toast.makeText(context, "فرمت کانفینگ نامعتبر است! لطفاً vless یا پروکسی تلگرام سالم وارد کنید.", Toast.LENGTH_LONG).show()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CountryFilterChip(
    label: String,
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        color = if (isSelected) CyberPrimary else CyberSurface,
        border = if (isSelected) null else BorderStroke(1.dp, CyberSurfaceGlass)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) CyberBg else CyberTextPrimary
            )
        }
    }
}

@Composable
fun V2rayServersListView(
    servers: List<V2rayServerEntity>,
    onCopy: (String) -> Unit,
    onFavoriteToggle: (V2rayServerEntity) -> Unit,
    onDelete: (V2rayServerEntity) -> Unit
) {
    if (servers.isEmpty()) {
        EmptyListPlaceholder(
            icon = Icons.Default.FlashOff,
            title = "سروری یافت نشد",
            desc = "بروزرسانی روزانه را از دکمه بالا اجرا کنید یا به اینترنت متصل شوید."
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(servers, key = { it.id }) { server ->
                V2rayServerCard(
                    server = server,
                    onCopy = { onCopy(server.config) },
                    onFavoriteToggle = { onFavoriteToggle(server) },
                    onDelete = { onDelete(server) }
                )
            }
        }
    }
}

@Composable
fun V2rayServerCard(
    server: V2rayServerEntity,
    onCopy: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("v2ray_server_card_${server.id}"),
        colors = CardDefaults.cardColors(containerColor = CyberSurface.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, CyberSurfaceGlass),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = server.type,
                        color = CyberPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Flag Indicator
                val flag = V2rayParser.getFlagEmoji(server.countryCode)
                Text(flag, fontSize = 20.sp)

                Spacer(modifier = Modifier.width(6.dp))

                // Name
                Text(
                    text = server.name,
                    color = CyberTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Favorite Toggle
                IconButton(onClick = onFavoriteToggle) {
                    Icon(
                        imageVector = if (server.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Save Server",
                        tint = if (server.isFavorite) CyberPrimary else CyberTextSecondary
                    )
                }

                // Delete Toggle
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove Server",
                        tint = CyberTextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Latency & Copy Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Address description
                Text(
                    text = "${server.address}:${server.port}",
                    color = CyberTextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(0.6f)
                )

                // Latency Indicator
                LatencyIndicator(ping = server.ping)

                Spacer(modifier = Modifier.width(8.dp))

                // Copy Action Core button
                Button(
                    onClick = onCopy,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = "Copy Config",
                        tint = CyberBg,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "کپی کانفینگ",
                        color = CyberBg,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun TelegramProxiesListView(
    proxies: List<TelegramProxyEntity>,
    onConnect: (TelegramProxyEntity) -> Unit,
    onCopy: (TelegramProxyEntity) -> Unit,
    onFavoriteToggle: (TelegramProxyEntity) -> Unit,
    onDelete: (TelegramProxyEntity) -> Unit
) {
    if (proxies.isEmpty()) {
        EmptyListPlaceholder(
            icon = Icons.Default.FlashOff,
            title = "پروکسی یافت نشد",
            desc = "دریافت روزانه را از دکمه تازه سازی فعال کنید یا به اینترنت متصل شوید."
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(proxies, key = { it.id }) { proxy ->
                TelegramProxyCard(
                    proxy = proxy,
                    onConnect = { onConnect(proxy) },
                    onCopy = { onCopy(proxy) },
                    onFavoriteToggle = { onFavoriteToggle(proxy) },
                    onDelete = { onDelete(proxy) }
                )
            }
        }
    }
}

@Composable
fun TelegramProxyCard(
    proxy: TelegramProxyEntity,
    onConnect: () -> Unit,
    onCopy: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CyberSurface.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, CyberSurfaceGlass),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Telegram Badge icon
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(CyberBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = null,
                        tint = CyberPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Flag Indicator
                val flag = V2rayParser.getFlagEmoji(proxy.countryCode)
                Text(flag, fontSize = 20.sp)

                Spacer(modifier = Modifier.width(6.dp))

                // Name Details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = proxy.name,
                        color = CyberTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${proxy.server}:${proxy.port}",
                        color = CyberTextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Favorite Toggle
                IconButton(onClick = onFavoriteToggle) {
                    Icon(
                        imageVector = if (proxy.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Save Proxy",
                        tint = if (proxy.isFavorite) CyberPrimary else CyberTextSecondary
                    )
                }

                // Delete button
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Local",
                        tint = CyberTextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Latency diagnosis
                LatencyIndicator(ping = proxy.ping)

                Row {
                    OutlinedButton(
                        onClick = onCopy,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberPrimary),
                        border = BorderStroke(1.dp, CyberPrimary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("کپی لینک", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onConnect,
                        colors = ButtonDefaults.buttonColors(containerColor = CyberSecondary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Send, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "اتصال به تلگرام",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LatencyIndicator(ping: Int) {
    val (label, tint) = when {
        ping == -1 -> "محدود شده یا خاموش" to PingGray
        ping < 200 -> "$ping ms" to PingGreen
        ping < 500 -> "$ping ms" to PingYellow
        else -> "$ping ms" to PingRed
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(tint)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = tint,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AiAssistView(
    viewModel: ProxyViewModel,
    onCopyAll: (List<String>) -> Unit
) {
    val aiResultList by viewModel.aiResultList.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
    var prompt by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurface.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, CyberSurfaceGlass),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "🤖 هوش مصنوعی شکارچی سرور",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "با نوشتن نوع کشور یا سرعت در کادر زیر، هوش مصنوعی Gemini 3.5 Flash به صورت هوشمند چند ثانیه بعد سالم‌ترین سرورهای فیلترشکن را در لیست شما تولید و ذخیره می‌کند.",
                        fontSize = 11.sp,
                        color = CyberTextSecondary,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        placeholder = { Text("مثلاً: سرورهای پرسرعت آلمان و آمریکا vless bفرست", fontSize = 12.sp, color = CyberTextSecondary) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .testTag("ai_prompt_input"),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberPrimary,
                            unfocusedBorderColor = CyberSurfaceGlass,
                            focusedTextColor = CyberTextPrimary,
                            unfocusedTextColor = CyberTextPrimary,
                            focusedContainerColor = CyberBg.copy(alpha = 0.8f),
                            unfocusedContainerColor = CyberBg.copy(alpha = 0.6f)
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            if (prompt.isNotEmpty()) viewModel.searchWithGeminiAI(prompt)
                        })
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { viewModel.searchWithGeminiAI(prompt) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("ai_generate_button"),
                        enabled = prompt.isNotEmpty() && !isAiLoading,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isAiLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                        } else {
                            Icon(Icons.Default.AutoAwesome, null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("دریافت سرور هوشمند فیلترشکن", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        if (aiResultList.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "سرورهای استخراج شده توسط AI (${aiResultList.size})",
                        fontWeight = FontWeight.Bold,
                        color = CyberPrimary,
                        fontSize = 12.sp
                    )

                    TextButton(onClick = { onCopyAll(aiResultList) }) {
                        Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("کپی همه", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            items(aiResultList) { rawConfig ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, CyberSurfaceGlass)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isTg = rawConfig.startsWith("tg://") || rawConfig.contains("t.me")
                        Icon(
                            imageVector = if (isTg) Icons.Default.Send else Icons.Default.Bolt,
                            contentDescription = null,
                            tint = if (isTg) CyberSecondary else CyberPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = rawConfig,
                            color = CyberTextPrimary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        } else if (!isAiLoading) {
            item {
                EmptyListPlaceholder(
                    icon = Icons.Default.AutoAwesome,
                    title = "یافت نشد یا منتظر شروع جستجو",
                    desc = "شکارچی هوشمند با کمک هوش مصنوعی کدهای فیلترشکن را برای شما استخراج خواهد کرد."
                )
            }
        }
    }
}

@Composable
fun AddManualConfigView(
    onAdd: (String) -> Unit
) {
    var configText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CyberSurface.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, CyberSurfaceGlass),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "✍️ افزودن دستی سرور یا پروکسی تلگرام",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "کد کانفینگ مورد نظر خود را در باکس پایین کپی کنید (از فرمت‌های vless، vmess، sss یا پروکسی تلگرام tg:// پشتیبانی می‌شود)",
                    fontSize = 11.sp,
                    color = CyberTextSecondary,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = configText,
                    onValueChange = { configText = it },
                    placeholder = { Text("مثال: vless://7d48... یا tg://proxy?server=...", fontSize = 11.sp, color = CyberTextSecondary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .testTag("manual_config_input"),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberPrimary,
                        unfocusedBorderColor = CyberSurfaceGlass,
                        focusedTextColor = CyberTextPrimary,
                        unfocusedTextColor = CyberTextPrimary,
                        focusedContainerColor = CyberBg.copy(alpha = 0.8f),
                        unfocusedContainerColor = CyberBg.copy(alpha = 0.6f)
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        if (configText.isNotEmpty()) {
                            onAdd(configText)
                            configText = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("add_manual_save_button"),
                    enabled = configText.isNotEmpty(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Save, null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ذخیره و افزودن به لیست من", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun EmptyListPlaceholder(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CyberTextSecondary.copy(alpha = 0.4f),
            modifier = Modifier.size(54.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = title,
            color = CyberTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = desc,
            color = CyberTextSecondary,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )
    }
}

fun imageOfProxyType(type: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (type) {
        "VMESS" -> Icons.Default.VpnLock
        "VLESS" -> Icons.Default.Bolt
        "TROJAN" -> Icons.Default.Security
        "SS" -> Icons.Default.SendAndArchive
        else -> Icons.Default.CloudQueue
    }
}
