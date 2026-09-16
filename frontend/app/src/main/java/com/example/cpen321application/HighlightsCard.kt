package com.example.cpen321application

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import android.annotation.SuppressLint
import android.webkit.*
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

internal data class MatchHighlight(val title: String, val competition: String, val date: String, val videoUrl: String)

internal suspend fun fetchHighlight(): MatchHighlight = withContext(Dispatchers.IO) {
    val url = URL("${BuildConfig.API_BASE_URL.trimEnd('/')}/highlights")
    require(url.protocol == "https")
    val connection = (url.openConnection() as HttpsURLConnection).apply {
        connectTimeout = 15_000
        readTimeout = 15_000
        instanceFollowRedirects = false
    }
    try {
        check(connection.responseCode == 200)
        val data = connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
        val list = data.getJSONArray("highlights")
        check(list.length() > 0)
        val item = list.getJSONObject(kotlin.random.Random.nextInt(list.length()))
        val videoUrl = item.getString("videoUrl")
        val parsed = URL(videoUrl)
        require(parsed.protocol == "https" && parsed.host in listOf("www.scorebat.com", "scorebat.com") && parsed.path.startsWith("/embed/v/") && parsed.userInfo == null)
        MatchHighlight(item.getString("title"), item.optString("competition"), item.optString("date").take(10), videoUrl)
    } finally {
        connection.disconnect()
    }
}

@Composable
fun HighlightsCard() {
    var request by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var highlight by remember { mutableStateOf<MatchHighlight?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(request) {
        loading = true
        highlight = null
        error = null
        try {
            highlight = fetchHighlight()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            error = "Highlights are unavailable right now. Try again or play another shootout."
        } finally {
            loading = false
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Watch real highlights", style = MaterialTheme.typography.titleMedium)
        if (loading) Text("Loading a match highlight…")
        highlight?.let { match ->
            Text(match.title, style = MaterialTheme.typography.titleSmall)
            Text(listOf(match.competition, match.date).filter { it.isNotBlank() }.joinToString(" • "))
            key(match.videoUrl, request) { HighlightPlayer(match.videoUrl) }
            Text("Videos by ScoreBat. Free highlights may include ads and watermarks.", style = MaterialTheme.typography.bodySmall)
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(enabled = !loading, onClick = { request++ }) {
            Text(if (error != null) "Try again" else "Watch another highlight")
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun HighlightPlayer(url: String) {
    val owner = LocalLifecycleOwner.current
    var player by remember { mutableStateOf<WebView?>(null) }
    var failed by remember { mutableStateOf(false) }
    DisposableEffect(owner, player) {
        val view = player
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) view?.onResume()
            if (event == Lifecycle.Event.ON_PAUSE) view?.onPause()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    if (failed) Text("This video could not load. Try another highlight.", color = MaterialTheme.colorScheme.error)
    AndroidView(
        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        // Keep playback in the player and reject non-HTTPS navigation.
                        return request.url.scheme != "https"
                    }
                    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                        if (request.isForMainFrame) failed = true
                    }
                }
                player = this
                val source = android.text.TextUtils.htmlEncode(url)
                loadDataWithBaseURL(
                    "${BuildConfig.API_BASE_URL.trimEnd('/')}/",
                    """<!DOCTYPE html><html><head>
                        <meta name="viewport" content="width=device-width,initial-scale=1">
                        <style>html,body{margin:0;width:100%;height:100%;background:#000}
                        iframe{position:fixed;inset:0;width:100%;height:100%;border:0}</style>
                        </head><body><iframe src="$source" title="Football highlight"
                        allow="autoplay; fullscreen; encrypted-media" allowfullscreen
                        referrerpolicy="strict-origin-when-cross-origin"></iframe></body></html>""".trimIndent(),
                    "text/html", "UTF-8", null
                )
            }
        },
        onRelease = { view ->
            view.stopLoading()
            view.onPause()
            view.removeAllViews()
            view.destroy()
            player = null
        }
    )
}
