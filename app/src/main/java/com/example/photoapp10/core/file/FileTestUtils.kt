package com.example.photoapp10.core.file

import android.content.Context
import android.content.Intent
import timber.log.Timber

object FileTestUtils {

    fun testFileOperations(context: Context): Boolean {
        val appStorage = AppStorage(context)
        val testAlbumId = 999L
        val testFilename = "test_photo.jpg"

        return try {
            // Test 1: Create dummy file
            Timber.d("Testing file creation...")
            val dummyFile = appStorage.createDummyFile(testAlbumId, testFilename, "Test photo content")
            if (dummyFile == null || !dummyFile.exists()) {
                Timber.e("Failed to create dummy file")
                return false
            }
            Timber.d("✅ File created successfully: ${dummyFile.absolutePath}")

            // Test 2: Check file size
            val fileSize = appStorage.getFileSize(dummyFile)
            Timber.d("✅ File size: ${AppStorage.formatBytes(fileSize)}")

            // Test 3: List files
            val files = appStorage.listFiles(testAlbumId)
            Timber.d("✅ Files in album $testAlbumId: ${files.size}")

            // Test 4: Get storage info
            val storageInfo = appStorage.getStorageInfo()
            Timber.d("✅ Storage info: ${AppStorage.formatBytes(storageInfo.totalAppSize)} used, ${AppStorage.formatBytes(storageInfo.availableSpace)} available")

            // Test 5: Create URI for sharing
            val uri = appStorage.getUriForFile(dummyFile)
            if (uri == null) {
                Timber.e("Failed to create URI for file")
                return false
            }
            Timber.d("✅ URI created: $uri")

            // Test 6: Delete file
            val deleted = appStorage.deleteFile(dummyFile)
            if (!deleted) {
                Timber.e("Failed to delete dummy file")
                return false
            }
            Timber.d("✅ File deleted successfully")

            // Test 7: Clean up album directory
            appStorage.deleteAlbumFiles(testAlbumId)
            Timber.d("✅ Album files cleaned up")

            Timber.d("🎉 All file operations tests passed!")
            true

        } catch (e: Exception) {
            Timber.e(e, "File operations test failed")
            false
        }
    }

    fun testShareIntent(context: Context): Intent? {
        val appStorage = AppStorage(context)
        val testAlbumId = 998L
        val testFilename = "share_test.jpg"

        return try {
            // Create a dummy file to share
            val dummyFile = appStorage.createDummyFile(testAlbumId, testFilename, "Shareable content")
            if (dummyFile == null) {
                Timber.e("Failed to create file for sharing test")
                return null
            }

            // Get URI for the file
            val uri = appStorage.getUriForFile(dummyFile)
            if (uri == null) {
                Timber.e("Failed to create URI for sharing")
                appStorage.deleteFile(dummyFile)
                return null
            }

            // Create share intent
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "Shared from PhotoApp")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            Timber.d("✅ Share intent created successfully for URI: $uri")

            // Clean up test file
            appStorage.deleteFile(dummyFile)

            Intent.createChooser(shareIntent, "Share Photo")

        } catch (e: Exception) {
            Timber.e(e, "Share intent test failed")
            null
        }
    }

    fun runAllTests(context: Context): Boolean {
        Timber.d("🧪 Starting AppStorage tests...")
        
        val fileOpsResult = testFileOperations(context)
        val shareIntentResult = testShareIntent(context) != null
        
        val allPassed = fileOpsResult && shareIntentResult
        
        if (allPassed) {
            Timber.d("🎉 All AppStorage tests passed!")
        } else {
            Timber.e("❌ Some AppStorage tests failed")
        }
        
        return allPassed
    }
}













