package com.example.utils

import android.util.Base64
import com.example.data.TelegramProxyEntity
import com.example.data.V2rayServerEntity
import java.net.URLDecoder

object V2rayParser {

    /**
     * Parses any line string into a V2rayServerEntity if it's a valid config link.
     */
    fun parseV2rayLink(rawLink: String): V2rayServerEntity? {
        val link = rawLink.trim()
        try {
            return when {
                link.startsWith("vmess://") -> parseVmess(link)
                link.startsWith("vless://") -> parseGenericUri(link, "VLESS")
                link.startsWith("trojan://") -> parseGenericUri(link, "TROJAN")
                link.startsWith("ss://") -> parseShadowsocks(link)
                else -> null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Parses Telegram proxy link formats:
     * - tg://proxy?server=[server]&port=[port]&secret=[secret]
     * - https://t.me/proxy?server=[server]&port=[port]&secret=[secret]
     */
    fun parseTelegramProxy(rawLink: String): TelegramProxyEntity? {
        val link = rawLink.trim()
        if (!link.startsWith("tg://proxy") && !link.contains("t.me/proxy")) return null

        try {
            val querySection = if (link.contains("?")) {
                link.substringAfter("?")
            } else {
                return null
            }

            val params = querySection.split("&").associate {
                val parts = it.split("=")
                val key = parts.getOrNull(0) ?: ""
                val value = if (parts.size > 1) parts[1] else ""
                key to URLDecoder.decode(value, "UTF-8")
            }

            val server = params["server"] ?: return null
            val portStr = params["port"] ?: return null
            val port = portStr.toIntOrNull() ?: return null
            val secret = params["secret"] ?: return null
            
            // Generate a smart name
            val name = params["name"] ?: "MTProto Proxy (${server.take(12)})"
            val countryCode = detectCountry(name)

            return TelegramProxyEntity(
                name = name,
                server = server,
                port = port,
                secret = secret,
                countryCode = countryCode
            )
        } catch (e: Exception) {
            return null
        }
    }

    private fun parseVmess(link: String): V2rayServerEntity? {
        val base64Data = link.substringAfter("vmess://").trim()
        val decoded = try {
            val cleanBase64 = base64Data.filter { !it.isWhitespace() }
            String(Base64.decode(cleanBase64, Base64.DEFAULT))
        } catch (e: Exception) {
            return null
        }

        // Extremely safe JSON crawler to avoid libraries mismatch
        val add = extractJsonValue(decoded, "add") ?: "" // IP/Host
        val portStr = extractJsonValue(decoded, "port") ?: "0"
        val port = portStr.toIntOrNull() ?: 0
        var ps = extractJsonValue(decoded, "ps") ?: "VMess Config" // Alias Name
        ps = try { URLDecoder.decode(ps, "UTF-8") } catch (e: Exception) { ps }

        if (add.isEmpty() || port == 0) return null

        val countryCode = detectCountry(ps)

        return V2rayServerEntity(
            type = "VMESS",
            name = ps.ifEmpty { "VMess Hub" },
            config = link,
            address = add,
            port = port,
            countryCode = countryCode
        )
    }

    private fun parseGenericUri(link: String, type: String): V2rayServerEntity? {
        // vless://[uuid]@[ip]:[port]?[query]#[alias]
        val parts = link.split("#")
        val mainPart = parts[0]
        var name = if (parts.size > 1) {
            try { URLDecoder.decode(parts[1], "UTF-8") } catch (e: Exception) { parts[1] }
        } else {
            "$type Server"
        }

        val withoutScheme = mainPart.substringAfter("://")
        if (!withoutScheme.contains("@")) return null

        val connectionInfo = withoutScheme.substringAfter("@")
        val hostAndPortSection = connectionInfo.substringBefore("/")
            .substringBefore("?")

        val hostAndPort = hostAndPortSection.split(":")
        if (hostAndPort.isEmpty()) return null

        val address = hostAndPort[0]
        val portStr = if (hostAndPort.size > 1) hostAndPort[1] else "443"
        val port = portStr.toIntOrNull() ?: 443

        val countryCode = detectCountry(name)

        return V2rayServerEntity(
            type = type,
            name = name,
            config = link,
            address = address,
            port = port,
            countryCode = countryCode
        )
    }

    private fun parseShadowsocks(link: String): V2rayServerEntity? {
        // ss://[base64]#[name]
        val parts = link.split("#")
        val mainPart = parts[0]
        var name = if (parts.size > 1) {
            try { URLDecoder.decode(parts[1], "UTF-8") } catch (e: Exception) { parts[1] }
        } else {
            "Shadowsocks"
        }

        val rawSs = mainPart.substringAfter("ss://")
        var address = "0.0.0.0"
        var port = 8388

        try {
            if (rawSs.contains("@")) {
                // ss://base64@address:port
                val credentialsPart = rawSs.substringBefore("@")
                val addressPart = rawSs.substringAfter("@")
                val hostPort = addressPart.split(":")
                address = hostPort[0]
                port = hostPort.getOrNull(1)?.substringBefore("/")?.toIntOrNull() ?: 8388
            } else {
                // ss://base64 (which decodes to cipher:password@address:port)
                val cleanBase64 = rawSs.filter { !it.isWhitespace() }
                // Base64 padding adjust
                val paddedBase64 = cleanBase64 + "=".repeat((4 - cleanBase64.length % 4) % 4)
                val decoded = String(Base64.decode(paddedBase64, Base64.DEFAULT))
                if (decoded.contains("@")) {
                    val addressPart = decoded.substringAfter("@")
                    val hostPort = addressPart.split(":")
                    address = hostPort[0]
                    port = hostPort.getOrNull(1)?.substringBefore("/")?.toIntOrNull() ?: 8388
                }
            }
        } catch (e: Exception) {
            // Un-parsable host, keep defaults but still register config for direct copying
        }

        val countryCode = detectCountry(name)

        return V2rayServerEntity(
            type = "SS",
            name = name,
            config = link,
            address = address,
            port = port,
            countryCode = countryCode
        )
    }

    /**
     * Extract key string value from simple custom flat JSON
     */
    private fun extractJsonValue(json: String, key: String): String? {
        val pattern = "\"$key\"\\s*:\\s*\"([^\"]*)\"".toRegex()
        val matchResult = pattern.find(json)
        if (matchResult != null) {
            return matchResult.groupValues[1]
        }
        
        // Try fallback for unquoted values (like numeric ports)
        val numPattern = "\"$key\"\\s*:\\s*([0-9]+)".toRegex()
        val numMatch = numPattern.find(json)
        if (numMatch != null) {
            return numMatch.groupValues[1]
        }
        return null
    }

    /**
     * Determines Country code from alias name.
     */
    fun detectCountry(nameName: String): String {
        val name = nameName.lowercase()
        return when {
            // Flags checking
            name.contains("🇺🇸") -> "US"
            name.contains("🇩🇪") -> "DE"
            name.contains("🇬🇧") -> "GB"
            name.contains("🇨🇦") -> "CA"
            name.contains("🇫🇷") -> "FR"
            name.contains("🇳🇱") -> "NL"
            name.contains("🇸🇬") -> "SG"
            name.contains("🇯🇵") -> "JP"
            name.contains("🇹🇷") -> "TR"
            name.contains("🇫🇮") -> "FI"
            name.contains("🇦🇪") -> "AE"
            name.contains("🇮🇷") -> "IR"
            name.contains("🇷🇺") -> "RU"
            name.contains("🇮🇳") -> "IN"

            // Text checking
            name.contains("germany") || name.contains("german") || name.contains(" de ") -> "DE"
            name.contains("usa") || name.contains("united states") || name.contains("us-") || name.contains(" us ") -> "US"
            name.contains("netherlands") || name.contains("nl-") || name.contains("nl ") -> "NL"
            name.contains("singapore") || name.contains("sg-") -> "SG"
            name.contains("france") || name.contains("fr-") -> "FR"
            name.contains("canada") || name.contains("ca-") -> "CA"
            name.contains("turkey") || name.contains("tr-") || name.contains("turk") -> "TR"
            name.contains("finland") || name.contains("fi-") -> "FI"
            name.contains("england") || name.contains("uk-") || name.contains("great britain") -> "GB"
            name.contains("japan") || name.contains("jp-") -> "JP"
            name.contains("russia") || name.contains("ru-") -> "RU"
            name.contains("india") || name.contains("in-") -> "IN"
            name.contains("iran") || name.contains("ir-") || name.contains("mci") || name.contains("irancell") || name.contains("hamrah") || name.contains("rightel") -> "IR"
            
            else -> "UN"
        }
    }

    /**
     * Converts a Country Code to its corresponding Emoji Flag
     */
    fun getFlagEmoji(countryCode: String): String {
        if (countryCode == "UN" || countryCode.length != 2) return "🌐"
        val firstChar = Character.codePointAt(countryCode, 0) - 0x41 + 0x1F1E6
        val secondChar = Character.codePointAt(countryCode, 1) - 0x41 + 0x1F1E6
        return String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
    }
}
