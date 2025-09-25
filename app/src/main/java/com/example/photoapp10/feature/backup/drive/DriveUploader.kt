package com.example.photoapp10.feature.backup.drive

import android.util.Log
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File as DriveFile
import com.google.api.services.drive.model.FileList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class DriveUploader(private val drive: Drive) {

    suspend fun putBackupJson(json: ByteArray, fileName: String = "backup.json") = withContext(Dispatchers.IO) {
        try {
            Log.d("DriveUploader", "Uploading $fileName (${json.size} bytes) via Drive client")
            
            // Check if file already exists
            val existing = findExistingFile(fileName)
            
            val fileMetadata = DriveFile().apply {
                name = fileName
                parents = listOf("appDataFolder")
            }
            
            val mediaContent = com.google.api.client.http.FileContent(
                "application/json",
                createTempFile(json)
            )
            
            val result = if (existing == null) {
                // Create new file
                drive.files().create(fileMetadata, mediaContent)
                    .setFields("id,name,modifiedTime")
                    .execute()
            } else {
                // Update existing file - don't set parents field for updates
                val updateMetadata = DriveFile().apply {
                    name = fileName
                    // Don't set parents for updates - file stays in same location
                }
                drive.files().update(existing.id, updateMetadata, mediaContent)
                    .setFields("id,name,modifiedTime")
                    .execute()
            }
            
            Log.d("DriveUploader", "Successfully uploaded $fileName with ID: ${result.id}")
            
        } catch (e: Exception) {
            Log.e("DriveUploader", "Failed to upload $fileName", e)
            throw e
        }
    }

    suspend fun putPhoto(file: File, albumId: Long, photoId: Long) = withContext(Dispatchers.IO) {
        try {
            val remotePath = "photos/$albumId/$photoId.jpg"
            Log.d("DriveUploader", "Uploading photo $remotePath (${file.length()} bytes) via Drive client")
            
            // Check if photo already exists
            val existing = findExistingFile(remotePath)
            
            val fileMetadata = DriveFile().apply {
                name = remotePath
                parents = listOf("appDataFolder")
            }
            
            val mediaContent = com.google.api.client.http.FileContent(
                "image/jpeg",
                file
            )
            
            val result = if (existing == null) {
                // Create new photo
                drive.files().create(fileMetadata, mediaContent)
                    .setFields("id,name,modifiedTime")
                    .execute()
            } else {
                // Update existing photo - don't set parents field for updates
                val updateMetadata = DriveFile().apply {
                    name = remotePath
                    // Don't set parents for updates - file stays in same location
                }
                drive.files().update(existing.id, updateMetadata, mediaContent)
                    .setFields("id,name,modifiedTime")
                    .execute()
            }
            
            Log.d("DriveUploader", "Successfully uploaded photo $remotePath with ID: ${result.id}")
            
        } catch (e: Exception) {
            Log.e("DriveUploader", "Failed to upload photo $albumId/$photoId", e)
            throw e
        }
    }
    
    private suspend fun findExistingFile(fileName: String): DriveFile? = withContext(Dispatchers.IO) {
        try {
            val listRequest = drive.files().list()
                .setSpaces("appDataFolder")
                .setQ("name = '$fileName' and trashed = false")
                .setFields("files(id,name)")
                .setPageSize(1)
            
            val fileList: FileList = listRequest.execute()
            return@withContext fileList.files?.firstOrNull()
        } catch (e: Exception) {
            Log.w("DriveUploader", "Failed to check for existing file: $fileName", e)
            return@withContext null
        }
    }
    
    private fun createTempFile(data: ByteArray): java.io.File {
        val tempFile = java.io.File.createTempFile("backup", ".json")
        tempFile.writeBytes(data)
        return tempFile
    }
}