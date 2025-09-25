package com.example.photoapp10

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.photoapp10.ui.components.AppScaffold
import com.example.photoapp10.ui.components.TopBar
import com.example.photoapp10.ui.theme.PhotoAppTheme
import com.example.photoapp10.ui.theme.ThemeMode
import com.example.photoapp10.ui.theme.ThemePrefs
import kotlinx.coroutines.launch

@Composable
fun AppRoot(app: Application) {
    val prefs = remember { ThemePrefs(app) }
    val mode by prefs.themeModeFlow.collectAsState(initial = ThemeMode.SYSTEM)
    val scope = rememberCoroutineScope()

    PhotoAppTheme(mode = mode) {
        AppScaffold(
            topBar = { 
                TopBar(
                    title = "PhotoApp",
                    actions = {
                        // Theme toggle button
                        IconButton(
                            onClick = {
                                scope.launch {
                                    val newMode = when (mode) {
                                        ThemeMode.SYSTEM -> ThemeMode.LIGHT
                                        ThemeMode.LIGHT -> ThemeMode.DARK
                                        ThemeMode.DARK -> ThemeMode.SYSTEM
                                    }
                                    prefs.setThemeMode(newMode)
                                }
                            }
                        ) {
                            Text(
                                text = when (mode) {
                                    ThemeMode.SYSTEM -> "S"
                                    ThemeMode.LIGHT -> "L"
                                    ThemeMode.DARK -> "D"
                                }
                            )
                        }
                    }
                ) 
            }
        ) { inner ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Hello Themed App",
                    style = MaterialTheme.typography.headlineMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Current theme: ${mode.name}",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = {
                        scope.launch {
                            val newMode = when (mode) {
                                ThemeMode.SYSTEM -> ThemeMode.LIGHT
                                ThemeMode.LIGHT -> ThemeMode.DARK
                                ThemeMode.DARK -> ThemeMode.SYSTEM
                            }
                            prefs.setThemeMode(newMode)
                        }
                    }
                ) {
                    Text("Toggle Theme")
                }
            }
        }
    }
}
