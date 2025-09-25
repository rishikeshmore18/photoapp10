package com.example.photoapp10.feature.auth.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.photoapp10.core.di.Modules
import com.example.photoapp10.feature.auth.AuthManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var rememberDevice by remember { mutableStateOf(true) }
    var snackbarHostState by remember { mutableStateOf(SnackbarHostState()) }
    
    val userPrefs = remember { Modules.provideUserPrefs(context) }
    
    // Google Sign-In launcher
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isLoading = false
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(ApiException::class.java)
            
            Timber.d("SignInScreen: Sign-in successful for ${account.email}")
            
            // Save remember device preference
            if (rememberDevice) {
                scope.launch {
                    userPrefs.setRememberDevice(context, true)
                }
            }
            
            // Navigate to restore gate
            navController.navigate("restore_gate") {
                popUpTo("signin") { inclusive = true }
            }
            
        } catch (e: ApiException) {
            Timber.e(e, "SignInScreen: Sign-in failed")
            val errorMessage = when (e.statusCode) {
                10 -> "Google Sign-In not configured. Please set up OAuth 2.0 in Google Cloud Console."
                12501 -> "Sign-in was cancelled"
                7 -> "Network error. Please check your connection."
                else -> "Sign-in failed (Error ${e.statusCode}). Please try again."
            }
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = errorMessage,
                    duration = SnackbarDuration.Long
                )
            }
        }
    }
    
    // Handle sign-in button click
    val handleSignIn: () -> Unit = {
        isLoading = true
        try {
            val signInIntent = AuthManager.getSignInIntent(context)
            signInLauncher.launch(signInIntent)
        } catch (e: Exception) {
            isLoading = false
            Timber.e(e, "SignInScreen: Failed to launch sign-in")
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "Failed to launch sign-in: ${e.message}",
                    duration = SnackbarDuration.Long
                )
            }
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App title
            Text(
                text = "Photo App",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 48.dp)
            )
            
            // Sign in button
            Button(
                onClick = handleSignIn,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = "Sign in with Google",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Development bypass button
            OutlinedButton(
                onClick = {
                    Timber.d("SignInScreen: Development bypass - skipping to restore gate")
                    navController.navigate("restore_gate") {
                        popUpTo("signin") { inclusive = true }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Skip Sign-In (Development)",
                    fontSize = 14.sp
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Remember device checkbox
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = rememberDevice,
                    onCheckedChange = { rememberDevice = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Remember this device",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
