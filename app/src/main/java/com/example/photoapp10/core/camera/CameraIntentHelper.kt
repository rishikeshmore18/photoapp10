package com.example.photoapp10.core.camera

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.photoapp10.core.file.AppStorage
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Helper class for launching native camera app via intents
 * Replaces CameraX implementation with system camera integration
 */
class CameraIntentHelper(private val context: Context) {
    
    private val storage = AppStorage(context)
    
    /**
     * Creates a camera intent for the specified album
     * @param albumId The album to save photos to
     * @return CameraIntentData containing intent, file, and URI
     */
    fun createCameraIntent(albumId: Long): CameraIntentData {
        try {
            // Create photo file in internal storage
            val photoFile = createPhotoFile(albumId)
            
            // Create URI using FileProvider for secure sharing
            val photoUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            
            // Create camera intent
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            
            Timber.d("CameraIntentHelper: Created camera intent for album $albumId")
            Timber.d("CameraIntentHelper: Photo file: ${photoFile.absolutePath}")
            Timber.d("CameraIntentHelper: Photo URI: $photoUri")
            
            return CameraIntentData(intent, photoFile, photoUri)
            
        } catch (e: Exception) {
            Timber.e(e, "CameraIntentHelper: Failed to create camera intent")
            throw e
        }
    }
    
    /**
     * Creates a camera chooser intent allowing user to select camera app
     * @param albumId The album to save photos to
     * @return CameraIntentData containing chooser intent, file, and URI
     */
    fun createCameraChooserIntent(albumId: Long): CameraIntentData {
        val cameraData = createCameraIntent(albumId)
        val chooser = Intent.createChooser(cameraData.intent, "Choose Camera App")
        return cameraData.copy(intent = chooser)
    }
    
    /**
     * Creates a photo file with timestamp-based naming
     * @param albumId The album directory
     * @return File object for the photo
     */
    private fun createPhotoFile(albumId: Long): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val filename = "IMG_${timestamp}.jpg"
        
        // Ensure album directory exists
        val albumDir = storage.getPhotosDir(albumId)
        if (!albumDir.exists()) {
            albumDir.mkdirs()
            Timber.d("CameraIntentHelper: Created album directory: ${albumDir.absolutePath}")
        }
        
        return File(albumDir, filename)
    }
    
    /**
     * Validates that a photo file was successfully created by the camera app
     * @param photoFile The file to validate
     * @return true if file exists and has content
     */
    fun validatePhotoFile(photoFile: File): Boolean {
        return try {
            val exists = photoFile.exists()
            val hasContent = photoFile.length() > 0
            val isValid = exists && hasContent
            
            Timber.d("CameraIntentHelper: Photo validation - exists: $exists, hasContent: $hasContent, valid: $isValid")
            
            if (!isValid) {
                Timber.w("CameraIntentHelper: Invalid photo file: ${photoFile.absolutePath}")
            }
            
            isValid
        } catch (e: Exception) {
            Timber.e(e, "CameraIntentHelper: Error validating photo file")
            false
        }
    }
    
    /**
     * Cleans up temporary photo files if capture was cancelled
     * @param photoFile The file to clean up
     */
    fun cleanupPhotoFile(photoFile: File) {
        try {
            if (photoFile.exists()) {
                val deleted = photoFile.delete()
                Timber.d("CameraIntentHelper: Cleanup result - deleted: $deleted")
            }
        } catch (e: Exception) {
            Timber.e(e, "CameraIntentHelper: Error cleaning up photo file")
        }
    }
}

/**
 * Data class containing camera intent information
 */
data class CameraIntentData(
    val intent: Intent,
    val photoFile: File,
    val photoUri: Uri
)
