package com.example.cpen321application

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cpen321application.ui.theme.CPEN321ApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CPEN321ApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    M1App(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun M1App(modifier: Modifier = Modifier) {
    var selectedFeature by rememberSaveable { mutableStateOf<String?>(null) }
    BackHandler(enabled = selectedFeature != null) {
        selectedFeature = null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (selectedFeature == null) {
            Text("CPEN 321 M1", style = MaterialTheme.typography.headlineMedium)
            Text("Choose a feature to get started.")
            Button(
                onClick = { selectedFeature = "Login + Server Info" },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Login + Server Info")
            }
            Button(
                onClick = { selectedFeature = "Live Pixel Art" },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Live Pixel Art")
            }
            Button(
                onClick = { selectedFeature = "Timer + Surprise" },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Timer + Surprise")
            }
        } else {
            TextButton(onClick = { selectedFeature = null }) {
                Text("Back to home")
            }
            Text(selectedFeature.orEmpty(), style = MaterialTheme.typography.headlineMedium)
            when (selectedFeature) {
                "Login + Server Info" -> GoogleSignInScreen()
                "Live Pixel Art" -> PixelArtScreen()
                else -> Text("Coming next: set a timer and discover a surprise when it finishes.")
            }
        }
    }
}
