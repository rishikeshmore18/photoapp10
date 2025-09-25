package com.example.photoapp10.feature.backup.drive

import android.content.Context
import android.util.Log
import com.example.photoapp10.feature.auth.AuthManager
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File as DriveFile
import com.google.api.services.drive.model.FileList
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DriveAppData(val drive: Drive) {
    companion object { private const val BACKUP = "backup.json" }

    data class BackupFile(val id: String, val name: String, val modifiedTimeMillis: Long)

    suspend fun findLatestBackup(): BackupFile? = withContext(Dispatchers.IO) {
        try {
            Log.d("DriveAppData", "Searching for latest backup in appDataFolder via Drive client")

            val listRequest = drive.files().list()
                .setSpaces("appDataFolder")
                .setQ("name = '$BACKUP' and trashed = false")
                .setFields("files(id,name,modifiedTime)")
                .setOrderBy("modifiedTime desc")
                .setPageSize(1)

            val fileList: FileList = listRequest.execute()
            val files = fileList.files

            if (files.isNullOrEmpty()) {
                Log.d("DriveAppData", "No backup.json found in appDataFolder")
                return@withContext null
            }

            val file = files[0]
            val modifiedTime = file.modifiedTime?.value ?: 0L
            
            val backupFile = BackupFile(
                id = file.id,
                name = file.name,
                modifiedTimeMillis = modifiedTime
            )
            
            Log.d("DriveAppData", "Found backup: ${backupFile.name} (${backupFile.id}) modified at ${backupFile.modifiedTimeMillis}")
            return@withContext backupFile

        } catch (e: Exception) {
            Log.e("DriveAppData", "Failed to find latest backup", e)
            return@withContext null
        }
    }

    suspend fun download(fileId: String, dst: File): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d("DriveAppData", "Downloading file $fileId to ${dst.absolutePath} via Drive client")

            dst.parentFile?.mkdirs()
            
            val outputStream = FileOutputStream(dst)
            drive.files().get(fileId).executeMediaAndDownloadTo(outputStream)
            outputStream.close()

            Log.d("DriveAppData", "Successfully downloaded file: ${dst.length()} bytes")
            return@withContext true

        } catch (e: Exception) {
            Log.e("DriveAppData", "Failed to download file $fileId", e)
            return@withContext false
        }
    }
}

suspend fun driveAppDataOrNull(ctx: Context): DriveAppData? {
    return try {
        val drive = AuthManager.buildDriveService(ctx)
        if (drive == null) {
            Log.w("DriveAppData", "No Drive service available")
            return null
        }

        Log.d("DriveAppData", "Created DriveAppData with Drive client")
        DriveAppData(drive)
    } catch (e: Exception) {
        Log.e("DriveAppData", "Failed to create DriveAppData", e)
        null
    }
}