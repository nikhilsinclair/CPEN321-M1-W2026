package com.example.cpen321application

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

internal data class PixelUpdate(val x: Int, val y: Int, val argb: Long)

internal fun parsePixel(text: String): PixelUpdate {
    val json = JSONObject(text)
    val x = json.get("x")
    val y = json.get("y")
    require(x is Int && x in 0..15 && y is Int && y in 0..15)
    val color = json.getString("color")
    require(Regex("#[0-9a-fA-F]{6}").matches(color))
    return PixelUpdate(x, y, 0xff000000L or color.substring(1).toLong(16))
}

internal object PixelStream {
    // Uses Android's existing network security configuration for the VM certificate.
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    fun updates(baseUrl: String) = callbackFlow {
        require(baseUrl.startsWith("https://"))
        val url = "wss://${baseUrl.removePrefix("https://").trimEnd('/')}/pixels"
        val socket = client.newWebSocket(Request.Builder().url(url).build(), object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val pixel = try {
                    parsePixel(text)
                } catch (_: Exception) {
                    close(IOException("Invalid pixel update received."))
                    webSocket.cancel()
                    return
                }
                if (trySend(pixel).isFailure) {
                    close(IOException("Pixel stream could not keep up. Reconnecting."))
                    webSocket.cancel()
                }
            }
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(code, reason)
                close(IOException("Pixel connection closed."))
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                close(t)
            }
        })
        awaitClose { socket.cancel() }
    }
}
