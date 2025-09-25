package com.example.photoapp10.feature.backup.ui

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.photoapp10.feature.backup.domain.SimpleBackupManager
import com.example.photoapp10.core.di.Modules
import com.example.photoapp10.feature.settings.data.UserPrefs
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SimpleBackupScreen() {
    Timber.i("SimpleBackupScreen: Starting")
    val app = LocalContext.current.applicationContext as Application
    val vm: SimpleBackupViewModel = viewModel(factory = SimpleBackupViewModel.factory(app))

    val backupInfo by vm.backupInfo.collectAsState()
    val isProcessing by vm.isProcessing.collectAsState()
    val result by vm.result.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Local Backup", 
            style = MaterialTheme.typography.titleLarge
        )

        // Backup folder info
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Backup Location", 
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    vm.getBackupFolderPath(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "This folder is accessible via the Files app",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Backup info (if exists)
        backupInfo?.let { info ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Existing Backup", 
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    
                    val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
                    Text("Created: ${dateFormat.format(Date(info.createdAt))}")
                    Text("Albums: ${info.albumsCount}")
                    Text("Photos: ${info.photosCount}")
                    Text("Size: ${formatBytes(info.totalSizeBytes)}")
                }
            }
        }

        // Action buttons
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Actions", 
                    style = MaterialTheme.typography.titleMedium
                )

                // Backup button
                Button(
                    onClick = { vm.createBackup() },
                    enabled = !isProcessing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Create Backup")
                }

                // Restore button
                Button(
                    onClick = { vm.restoreBackup() },
                    enabled = !isProcessing && backupInfo != null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Restore Backup")
                }

                if (backupInfo == null) {
                    Text(
                        "No backup found. Create a backup first.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Processing indicator
        if (isProcessing) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(8.dp))
                    Text("Processing...")
                }
            }
        }

        // Result display
        result?.let { resultText ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Result", 
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        resultText,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }

    // Load backup info on startup
    LaunchedEffect(Unit) {
        vm.loadBackupInfo()
    }
}

class SimpleBackupViewModel(app: Application) : AndroidViewModel(app) {
    private val backupManager = SimpleBackupManager(
        context = app,
        db = Modules.provideDb(app),
        storage = Modules.provideStorage(app),
        prefs = UserPrefs(app)
    )

    private val _backupInfo = MutableStateFlow<SimpleBackupManager.BackupInfo?>(null)
    val backupInfo: StateFlow<SimpleBackupManager.BackupInfo?> = _backupInfo.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _result = MutableStateFlow<String?>(null)
    val result: StateFlow<String?> = _result.asStateFlow()

    fun getBackupFolderPath(): String {
        return backupManager.getDefaultBackupFolderPath()
    }

    fun loadBackupInfo() {
        viewModelScope.launch {
            try {
                val info = backupManager.getBackupInfo()
                _backupInfo.value = info
                Timber.d("SimpleBackupViewModel: Loaded backup info: $info")
            } catch (e: Exception) {
                Timber.e(e, "SimpleBackupViewModel: Failed to load backup info")
            }
        }
    }

    fun createBackup() {
        viewModelScope.launch {
            try {
                _isProcessing.value = true
                _result.value = null
                
                val result = backupManager.createBackup()
                
                if (result.success) {
                    _result.value = buildString {
                        appendLine("✅ Backup Created Successfully")
                        appendLine()
                        appendLine("Albums: ${result.albumsCount}")
                        appendLine("Photos: ${result.photosCount}")
                        appendLine("Photos copied: ${result.photosCopied}")
                        if (result.photosMissing > 0) {
                            appendLine("Photos missing: ${result.photosMissing}")
                        }
                        appendLine("Thumbnails copied: ${result.thumbsCopied}")
                        if (result.thumbsMissing > 0) {
                            appendLine("Thumbnails missing: ${result.thumbsMissing}")
                        }
                        appendLine()
                        appendLine("Backup saved to:")
                        appendLine(result.backupPath)
                    }
                } else {
                    _result.value = "❌ ${result.message}"
                }
                
                // Reload backup info
                loadBackupInfo()
                
            } catch (e: Exception) {
                Timber.e(e, "SimpleBackupViewModel: Create backup failed")
                _result.value = "❌ Backup failed: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun restoreBackup() {
        viewModelScope.launch {
            try {
                _isProcessing.value = true
                _result.value = null
                
                val result = backupManager.restoreBackup()
                
                if (result.success) {
                    _result.value = buildString {
                        appendLine("✅ Restore Completed Successfully")
                        appendLine()
                        appendLine("Albums:")
                        appendLine("  • Inserted: ${result.albumsInserted}")
                        appendLine("  • Updated: ${result.albumsUpdated}")
                        appendLine()
                        appendLine("Photos:")
                        appendLine("  • Inserted: ${result.photosInserted}")
                        appendLine("  • Updated: ${result.photosUpdated}")
                        if (result.photosMissing > 0) {
                            appendLine("  • Missing files: ${result.photosMissing}")
                        }
                        appendLine()
                        appendLine("Used merge mode (kept existing data, updated with newer items)")
                    }
                } else {
                    _result.value = "❌ ${result.message}"
                }
                
            } catch (e: Exception) {
                Timber.e(e, "SimpleBackupViewModel: Restore backup failed")
                _result.value = "❌ Restore failed: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    companion object {
        fun factory(app: Application) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return SimpleBackupViewModel(app) as T
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    
    return when {
        gb >= 1.0 -> "%.1f GB".format(gb)
        mb >= 1.0 -> "%.1f MB".format(mb)
        kb >= 1.0 -> "%.1f KB".format(kb)
        else -> "$bytes bytes"
    }
}
