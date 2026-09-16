package com.example.cpen321application

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var highlight by remember { mutableStateOf<MatchHighlight?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Watch the pros", style = MaterialTheme.typography.titleMedium)
        Text("Finished your penalties? Discover a real match highlight.")
        highlight?.let { match ->
            Text(match.title, style = MaterialTheme.typography.titleSmall)
            Text(listOf(match.competition, match.date).filter { it.isNotBlank() }.joinToString(" • "))
            Button(onClick = {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(match.videoUrl)))
                } catch (_: ActivityNotFoundException) {
                    error = "No browser is available to open the video."
                }
            }) { Text("Watch highlight") }
            Text("Opens in your browser. Videos by ScoreBat; availability and ads may vary.", style = MaterialTheme.typography.bodySmall)
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(enabled = !loading, onClick = {
            scope.launch {
                loading = true
                error = null
                try {
                    highlight = fetchHighlight()
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    error = "Highlights are unavailable right now. You can still play again."
                } finally {
                    loading = false
                }
            }
        }) { Text(if (loading) "Finding highlights…" else if (highlight != null) "Find another highlight" else "Find a highlight") }
    }
}
