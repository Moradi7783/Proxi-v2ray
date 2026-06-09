package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProxyDao {
    // --- V2ray Servers DAO ---
    @Query("SELECT * FROM v2ray_servers ORDER BY isFavorite DESC, ping ASC, createdAt DESC")
    fun getAllV2rayServers(): Flow<List<V2rayServerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertV2rayServer(server: V2rayServerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertV2rayServers(servers: List<V2rayServerEntity>)

    @Update
    suspend fun updateV2rayServer(server: V2rayServerEntity)

    @Delete
    suspend fun deleteV2rayServer(server: V2rayServerEntity)

    @Query("DELETE FROM v2ray_servers WHERE isUserAdded = 0")
    suspend fun clearFetchedV2rayServers()

    @Query("DELETE FROM v2ray_servers")
    suspend fun clearAllV2rayServers()

    @Query("SELECT * FROM v2ray_servers WHERE config = :configLimit LIMIT 1")
    suspend fun getV2rayByConfig(configLimit: String): V2rayServerEntity?


    // --- Telegram Proxies DAO ---
    @Query("SELECT * FROM telegram_proxies ORDER BY isFavorite DESC, ping ASC, createdAt DESC")
    fun getAllTelegramProxies(): Flow<List<TelegramProxyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTelegramProxy(proxy: TelegramProxyEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTelegramProxies(proxies: List<TelegramProxyEntity>)

    @Update
    suspend fun updateTelegramProxy(proxy: TelegramProxyEntity)

    @Delete
    suspend fun deleteTelegramProxy(proxy: TelegramProxyEntity)

    @Query("DELETE FROM telegram_proxies WHERE isUserAdded = 0")
    suspend fun clearFetchedTelegramProxies()

    @Query("DELETE FROM telegram_proxies")
    suspend fun clearAllTelegramProxies()

    @Query("SELECT * FROM telegram_proxies WHERE server = :server AND port = :port AND secret = :secret LIMIT 1")
    suspend fun getTelegramProxyByUnique(server: String, port: Int, secret: String): TelegramProxyEntity?
}
