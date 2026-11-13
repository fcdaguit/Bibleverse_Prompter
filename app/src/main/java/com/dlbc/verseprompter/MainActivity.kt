package com.dlbc.verseprompter

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dlbc.verseprompter.data.db.seed.SeedState

/**
 * Placeholder host activity for Steps 1-2.
 *
 * It exists so the project compiles and produces an installable APK right now:
 * launching it shows the Bible import progress, which is the quickest way to
 * confirm your asset JSON is wired up correctly on a real phone. Step 4 replaces
 * the body of this file with the themed prompter UI and navigation.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // The prompter must never sleep mid-sermon.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val app = application as VersePrompterApp

        setContent {
            MaterialTheme {
                val seedState by app.seedManager.state.collectAsStateWithLifecycle()
                SeedStatusScreen(seedState)
            }
        }
    }
}

@Composable
private fun SeedStatusScreen(state: SeedState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (state) {
            is SeedState.InProgress -> {
                CircularProgressIndicator(color = Color(0xFF00FF41))
                Text(
                    text = state.label,
                    color = Color(0xFFF5F5F5),
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 24.dp)
                )
                Text(
                    text = "${state.versesLoaded} verses loaded",
                    color = Color(0xFF8A8A8A),
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            is SeedState.Failed -> {
                Text(
                    text = "Could not load ${state.versionCode}",
                    color = Color(0xFFFF6B6B),
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = state.message,
                    color = Color(0xFF8A8A8A),
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            SeedState.Ready -> {
                Text(
                    text = "Offline Bible library ready",
                    color = Color(0xFF00FF41),
                    fontSize = 26.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Prompter UI arrives in Step 4.",
                    color = Color(0xFF8A8A8A),
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}
