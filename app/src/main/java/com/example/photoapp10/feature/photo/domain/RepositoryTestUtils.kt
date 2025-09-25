package com.example.photoapp10.feature.photo.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

object RepositoryTestUtils {

    fun testRepositoryOperations(context: Context) {
        Timber.d("🧪 Testing repository operations...")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val albumRepo = com.example.photoapp10.core.di.Modules.provideAlbumRepository(context)
                val photoRepo = com.example.photoapp10.core.di.Modules.providePhotoRepository(context)
                
                // Test 1: Create album
                Timber.d("📁 Creating test album...")
                val albumId = albumRepo.createAlbum("Test Album")
                Timber.d("✅ Album created with ID: $albumId")
                
                // Test 2: Add photo to album
                Timber.d("📸 Adding test photo...")
                val testImagePath = createTestImageFile(context, albumId, 1L)
                val photoId = photoRepo.addPhotoFromPath(
                    albumId = albumId,
                    originalPath = testImagePath,
                    filename = "test_photo.jpg",
                    width = 1920,
                    height = 1080,
                    sizeBytes = 1024 * 1024 // 1MB
                )
                Timber.d("✅ Photo added with ID: $photoId")
                
                // Test 3: Toggle favorite
                Timber.d("⭐ Toggling favorite...")
                photoRepo.toggleFavorite(photoId)
                Timber.d("✅ Favorite toggled")
                
                // Test 4: Update caption
                Timber.d("📝 Updating caption...")
                photoRepo.updateCaption(photoId, "Test caption")
                Timber.d("✅ Caption updated")
                
                // Test 5: Update tags
                Timber.d("🏷️ Updating tags...")
                photoRepo.updateTags(photoId, listOf("test", "photo", "demo"))
                Timber.d("✅ Tags updated")
                
                // Test 6: Create second album and move photo
                Timber.d("🔄 Moving photo to new album...")
                val album2Id = albumRepo.createAlbum("Test Album 2")
                photoRepo.movePhoto(photoId, album2Id)
                Timber.d("✅ Photo moved to album $album2Id")
                
                // Test 7: Delete photo
                Timber.d("🗑️ Deleting photo...")
                photoRepo.deletePhoto(photoId)
                Timber.d("✅ Photo deleted")
                
                // Test 8: Delete albums
                Timber.d("🗑️ Cleaning up albums...")
                val albums = albumRepo.observeAlbums().first()
                albums.find { it.id == albumId }?.let { albumRepo.deleteAlbum(it) }
                albums.find { it.id == album2Id }?.let { albumRepo.deleteAlbum(it) }
                Timber.d("✅ Albums cleaned up")
                
                Timber.d("🎉 All repository tests completed successfully!")
                
            } catch (e: Exception) {
                Timber.e(e, "Repository test failed")
            }
        }
    }
    
    private fun createTestImageFile(context: Context, albumId: Long, photoId: Long): String {
        val storage = com.example.photoapp10.core.file.AppStorage(context)
        val testFile = storage.photoFile(albumId, photoId, "jpg")
        
        // Create a proper test image (512x512 with gradient)
        val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        for (x in 0 until 512) {
            for (y in 0 until 512) {
                val color = Color.rgb(
                    (x * 255 / 512),
                    (y * 255 / 512),
                    128
                )
                bitmap.setPixel(x, y, color)
            }
        }
        
        // Save as JPEG
        FileOutputStream(testFile).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
        }
        bitmap.recycle()
        
        Timber.d("✅ Created test image file: ${testFile.absolutePath}")
        return testFile.absolutePath
    }
}
