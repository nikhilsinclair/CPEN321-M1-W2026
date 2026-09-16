package com.example.cpen321application

import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URL
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Collections
import java.util.Locale
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

internal data class ServerInfo(val ip: String, val time: String, val name: String)

internal suspend fun fetchServerInfo(baseUrl: String): ServerInfo = withContext(Dispatchers.IO) {
    fun get(path: String): JSONObject {
        val url = URL("${baseUrl.trimEnd('/')}/$path")
        require(url.protocol == "https") { "The backend must use HTTPS." }
        val connection = (url.openConnection() as HttpsURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            instanceFollowRedirects = false
        }
        try {
            check(connection.responseCode == 200) { "Server returned HTTP ${connection.responseCode}." }
            return connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
        } finally {
            connection.disconnect()
        }
    }
    val ip = get("server-ip").getString("ip")
    val time = get("server-time").getString("time")
    val name = get("name")
    ServerInfo(ip, time, "${name.getString("firstName")} ${name.getString("lastName")}")
}

internal fun clientLocalTime(now: ZonedDateTime = ZonedDateTime.now()): String =
    now.format(DateTimeFormatter.ofPattern("HH:mm:ss 'GMT'xxx", Locale.US))

internal suspend fun clientIpAddress(): String = withContext(Dispatchers.IO) {
    val interfaces = NetworkInterface.getNetworkInterfaces() ?: return@withContext "Unavailable"
    val addresses = Collections.list(interfaces)
        .filter { it.isUp && !it.isLoopback }
        .flatMap { Collections.list(it.inetAddresses) }
        .filter { !it.isLoopbackAddress && !it.isLinkLocalAddress }
    (addresses.firstOrNull { it is Inet4Address } ?: addresses.firstOrNull())
        ?.hostAddress?.substringBefore('%') ?: "Unavailable"
}
