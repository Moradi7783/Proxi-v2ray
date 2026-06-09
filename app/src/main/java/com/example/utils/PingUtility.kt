package com.example.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

object PingUtility {

    /**
     * Measures latency (RTT) to a remote host and port via TCP handshake.
     * Returns the latency in milliseconds, or -1 if the connection failed/timed out.
     */
    suspend fun pingAddress(address: String, port: Int, timeoutMs: Int = 1800): Int = withContext(Dispatchers.IO) {
        if (address.isEmpty() || port <= 0 || port > 65535) return@withContext -1
        
        val startTime = System.currentTimeMillis()
        try {
            val socket = Socket()
            // Initiate a connection to test raw port reachability
            socket.connect(InetSocketAddress(address, port), timeoutMs)
            socket.close()
            val latency = (System.currentTimeMillis() - startTime).toInt()
            if (latency >= 0) latency else 0
        } catch (e: Exception) {
            -1 // Server is offline, blocked, or port is closed
        }
    }
}
