package com.example.cpen321application

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.net.ssl.HttpsURLConnection

internal data class MatchScore(val home: String, val away: String, val homeGoals: Int, val awayGoals: Int, val competition: String, val date: String)

private suspend fun fetchScores(): List<MatchScore> = withContext(Dispatchers.IO) {
    val connection = (URL("${BuildConfig.API_BASE_URL.trimEnd('/')}/scores").openConnection() as HttpsURLConnection).apply {
        connectTimeout = 20_000
        readTimeout = 20_000
        instanceFollowRedirects = false
    }
    try {
        check(connection.responseCode == 200)
        val matches = connection.inputStream.bufferedReader().use { JSONObject(it.readText()).getJSONArray("matches") }
        List(matches.length()) { index ->
            val item = matches.getJSONObject(index)
            MatchScore(item.getString("home"), item.getString("away"), item.getInt("homeGoals"), item.getInt("awayGoals"), item.optString("competition"), item.getString("date"))
        }
    } finally { connection.disconnect() }
}

@Composable
fun WeeklyScoresPanel() {
    var attempt by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var matches by remember { mutableStateOf(emptyList<MatchScore>()) }
    var shown by remember { mutableStateOf(10) }
    LaunchedEffect(attempt) {
        loading = true
        failed = false
        try { matches = fetchScores() }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { failed = true }
        finally { loading = false }
    }
    val format = remember { DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()).withZone(ZoneId.systemDefault()) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("This week’s real soccer scores", style = MaterialTheme.typography.titleLarge)
        Text("Finished matches from the last 7 days across supported competitions. Results may be delayed.", style = MaterialTheme.typography.bodySmall)
        when {
            loading -> Text("Loading recent scores…")
            failed -> {
                Text("Scores aren’t available right now. You can still play another shootout.")
                Button(onClick = { attempt++ }) { Text("Retry scores") }
            }
            matches.isEmpty() -> Text("No completed matches were found for the last 7 days.")
            else -> {
                matches.take(shown).forEach { match ->
                    HorizontalDivider()
                    Text("${match.home}  ${match.homeGoals} – ${match.awayGoals}  ${match.away}", style = MaterialTheme.typography.titleSmall)
                    Text("${match.competition} • ${runCatching { format.format(Instant.parse(match.date)) }.getOrDefault(match.date.take(10))}", style = MaterialTheme.typography.bodySmall)
                }
                if (shown < matches.size) Button(onClick = { shown += 10 }) { Text("Show more scores") }
            }
        }
        Text("Data provided by football-data.org", style = MaterialTheme.typography.bodySmall)
    }
}
