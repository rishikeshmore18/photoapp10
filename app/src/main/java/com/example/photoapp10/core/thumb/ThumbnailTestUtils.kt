package com.example.photoapp10.core.thumb

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

object ThumbnailTestUtils {

    fun createTestImage(context: Context, albumId: Long, photoId: Long): File? {
        return try {
            val storage = com.example.photoapp10.core.file.AppStorage(context)
            val testFile = storage.photoFile(albumId, photoId, "jpg")
            
            // Create a simple test bitmap (512x512 with gradient)
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
            
            Timber.d("✅ Created test image: ${testFile.absolutePath}")
            testFile
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to create test image")
            null
        }
    }

    suspend fun testThumbnailGeneration(context: Context): Boolean {
        val testAlbumId = 1000L
        val testPhotoId = 1L
        
        return try {
            // 1. Create test image
            val testImage = createTestImage(context, testAlbumId, testPhotoId)
            if (testImage == null || !testImage.exists()) {
                Timber.e("Failed to create test image")
                return false
            }
            
            // 2. Generate thumbnail
            val storage = com.example.photoapp10.core.file.AppStorage(context)
            val thumbnailer = Thumbnailer()
            val thumbFile = storage.thumbFile(testAlbumId, testPhotoId)
            
            Timber.d("Generating thumbnail...")
            val result = thumbnailer.generate(testImage, thumbFile, maxDim = 256, jpegQuality = 85)
            
            // 3. Verify thumbnail was created
            if (!thumbFile.exists()) {
                Timber.e("Thumbnail file was not created")
                return false
            }
            
            // 4. Verify thumbnail dimensions
            val thumbBitmap = BitmapFactory.decodeFile(thumbFile.absolutePath)
            if (thumbBitmap == null) {
                Timber.e("Failed to decode thumbnail")
                return false
            }
            
            val maxDim = maxOf(thumbBitmap.width, thumbBitmap.height)
            val isCorrectSize = maxDim <= 256
            
            Timber.d("✅ Thumbnail generated: ${thumbBitmap.width}x${thumbBitmap.height} (max: $maxDim)")
            Timber.d("✅ Thumbnail path: ${result.path}")
            Timber.d("✅ Size check: $isCorrectSize")
            
            // Cleanup
            thumbBitmap.recycle()
            testImage.delete()
            thumbFile.delete()
            
            isCorrectSize
            
        } catch (e: Exception) {
            Timber.e(e, "Thumbnail generation test failed")
            false
        }
    }

    suspend fun testMultipleThumbnails(context: Context): Boolean {
        val testAlbumId = 1001L
        val storage = com.example.photoapp10.core.file.AppStorage(context)
        val thumbnailer = Thumbnailer()
        
        return try {
            var successCount = 0
            val totalTests = 10
            
            for (i in 1..totalTests) {
                val testImage = createTestImage(context, testAlbumId, i.toLong())
                if (testImage != null && testImage.exists()) {
                    val thumbFile = storage.thumbFile(testAlbumId, i.toLong())
                    val result = thumbnailer.generate(testImage, thumbFile, maxDim = 256)
                    
                    if (thumbFile.exists()) {
                        successCount++
                        Timber.d("✅ Thumbnail $i generated successfully")
                    }
                    
                    // Cleanup
                    testImage.delete()
                    thumbFile.delete()
                }
            }
            
            val success = successCount == totalTests
            Timber.d("🎯 Multiple thumbnails test: $successCount/$totalTests successful")
            
            success
            
        } catch (e: Exception) {
            Timber.e(e, "Multiple thumbnails test failed")
            false
        }
    }

    fun runAllThumbnailTests(context: Context) {
        Timber.d("🧪 Starting thumbnail tests...")
        
        // Run the tests in a coroutine scope
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val singleTestResult = testThumbnailGeneration(context)
                val multipleTestResult = testMultipleThumbnails(context)
                
                Timber.d("🎯 Single thumbnail test: ${if (singleTestResult) "✅ PASSED" else "❌ FAILED"}")
                Timber.d("🎯 Multiple thumbnails test: ${if (multipleTestResult) "✅ PASSED" else "❌ FAILED"}")
                
                if (singleTestResult && multipleTestResult) {
                    Timber.d("🎉 ALL THUMBNAIL TESTS PASSED!")
                } else {
                    Timber.e("❌ Some thumbnail tests failed")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error running thumbnail tests")
            }
        }
    }
}
