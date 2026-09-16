package com.example.cpen321application

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import java.io.IOException
import javax.net.ssl.SSLException
import kotlinx.coroutines.CancellationException
import org.json.JSONException

@Composable
fun ServerInfoPanel() {
    var refresh by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var server by remember { mutableStateOf<ServerInfo?>(null) }
    var clientIp by remember { mutableStateOf("Unavailable") }
    var clientTime by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(refresh) {
        loading = true
        error = null
        server = null
        clientTime = clientLocalTime()
        clientIp = try { clientIpAddress() } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) { "Unavailable" }
        try {
            server = fetchServerInfo(BuildConfig.API_BASE_URL)
        } catch (e: CancellationException) {
            throw e
        } catch (_: SSLException) {
            error = "The secure connection could not be verified. Check the device clock and server certificate."
        } catch (_: IOException) {
            error = "Could not reach the server. Check your connection and try again."
        } catch (_: JSONException) {
            error = "The server returned information this app could not read."
        } catch (_: IllegalStateException) {
            error = "The server could not complete the request. Please try again."
        } catch (_: IllegalArgumentException) {
            error = "The backend address must use HTTPS."
        } finally {
            loading = false
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Server and device information", style = MaterialTheme.typography.titleMedium)
        Text("Client IP: $clientIp")
        Text("Client local time: $clientTime")
        if (loading) Text("Loading server information…")
        server?.let {
            Text("Server IP: ${it.ip}")
            Text("Server local time: ${it.time}")
            Text("Developer name: ${it.name}")
            Text("Times captured for this request. Tap Refresh to update.", style = MaterialTheme.typography.bodySmall)
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(enabled = !loading, onClick = { refresh++ }) {
            Text(if (error == null) "Refresh" else "Retry")
        }
    }
}
