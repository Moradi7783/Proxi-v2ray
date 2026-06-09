package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "v2ray_servers")
data class V2rayServerEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // VMESS, VLESS, TROJAN, SS
    val name: String,
    val config: String, // Full configuration string (vless://..., vmess://...)
    val address: String, // Parsed host/IP for pinging
    val port: Int, // Parsed port
    val ping: Int = -1, // Latency in ms, -1 if untested or unreachable
    val countryCode: String = "UN", // Two letter country code or "UN" for unknown
    val isFavorite: Boolean = false,
    val isUserAdded: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "telegram_proxies")
data class TelegramProxyEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val server: String,
    val port: Int,
    val secret: String,
    val ping: Int = -1, // Latency in ms, -1 if untested or unreachable
    val countryCode: String = "UN",
    val isFavorite: Boolean = false,
    val isUserAdded: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
