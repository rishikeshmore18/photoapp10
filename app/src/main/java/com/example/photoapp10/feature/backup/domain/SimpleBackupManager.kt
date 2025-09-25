package com.example.photoapp10.feature.backup.domain

import android.content.Context
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import com.example.photoapp10.core.db.AppDb
import com.example.photoapp10.core.file.AppStorage
import com.example.photoapp10.feature.album.data.AlbumEntity
import com.example.photoapp10.feature.photo.data.PhotoEntity
import com.example.photoapp10.feature.backup.domain.BackupRoot
import com.example.photoapp10.feature.backup.domain.BackupAlbum
import com.example.photoapp10.feature.backup.domain.BackupPhoto
import com.example.photoapp10.feature.backup.domain.BackupSettings
import com.example.photoapp10.feature.backup.domain.BACKUP_SCHEMA_VERSION
import com.example.photoapp10.feature.settings.data.UserPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File

/**
 * Simplified backup manager that creates a default backup folder
 * and provides simple backup/restore functionality
 */
class SimpleBackupManager(
    private val context: Context,
    private val db: AppDb,
    private val storage: AppStorage,
    private val prefs: UserPrefs
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }
    
    companion object {
        private const val BACKUP_FOLDER_NAME = "PhotoApp10_Backups"
        private const val BACKUP_JSON_FILE = "backup.json"
        private const val MEDIA_FOLDER = "media"
        private const val PHOTOS_FOLDER = "photos"
        private const val THUMBS_FOLDER = "thumbs"
    }

    /**
     * Get the default backup folder path that users can access via Files app
     */
    fun getDefaultBackupFolderPath(): String {
        val externalDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val backupDir = File(externalDir, BACKUP_FOLDER_NAME)
        return backupDir.absolutePath
    }

    /**
     * Get the default backup folder as a File object
     */
    private fun getDefaultBackupFolder(): File {
        val externalDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val backupDir = File(externalDir, BACKUP_FOLDER_NAME)
        backupDir.mkdirs() // Ensure it exists
        return backupDir
    }

    /**
     * Check if a backup exists in the default folder
     */
    suspend fun hasBackup(): Boolean = withContext(Dispatchers.IO) {
        val backupDir = getDefaultBackupFolder()
        val backupFile = File(backupDir, BACKUP_JSON_FILE)
        backupFile.exists()
    }

    /**
     * Get backup info (creation date, size, etc.)
     */
    suspend fun getBackupInfo(): BackupInfo? = withContext(Dispatchers.IO) {
        val backupDir = getDefaultBackupFolder()
        val backupFile = File(backupDir, BACKUP_JSON_FILE)
        
        if (!backupFile.exists()) {
            return@withContext null
        }

        try {
            val root = json.decodeFromString<BackupRoot>(backupFile.readText())
            val mediaDir = File(backupDir, MEDIA_FOLDER)
            val totalSize = if (mediaDir.exists()) {
                mediaDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
            } else {
                0L
            }

            BackupInfo(
                createdAt = root.createdAt,
                albumsCount = root.albums.size,
                photosCount = root.photos.size,
                totalSizeBytes = totalSize + backupFile.length(),
                backupPath = backupDir.absolutePath
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to read backup info")
            null
        }
    }

    /**
     * Create a backup of all albums and photos to the default folder
     */
    suspend fun createBackup(): BackupResult = withContext(Dispatchers.IO) {
        try {
            Timber.i("SimpleBackupManager: Starting backup to default folder")
            
            val backupDir = getDefaultBackupFolder()
            
            // Remove existing backup.json if present
            val backupFile = File(backupDir, BACKUP_JSON_FILE)
            if (backupFile.exists()) {
                backupFile.delete()
            }

            // Collect all albums and photos
            val albums = db.albumDao().getAllAlbums().map { album: AlbumEntity -> album.toBackupAlbum() }
            val photos = db.photoDao().getAllPhotos().map { photo: PhotoEntity ->
                BackupPhoto(
                    id = photo.id,
                    albumId = photo.albumId,
                    filename = photo.filename,
                    width = photo.width,
                    height = photo.height,
                    sizeBytes = photo.sizeBytes,
                    caption = photo.caption,
                    tags = photo.tags,
                    favorite = photo.favorite,
                    takenAt = photo.takenAt,
                    createdAt = photo.createdAt,
                    updatedAt = photo.updatedAt,
                    path = photo.path,
                    thumbPath = photo.thumbPath,
                    relativePath = "photos/${photo.albumId}/${photo.id}.jpg"
                )
            }

            val settings = BackupSettings(
                themeMode = prefs.themeFlow.firstOrNull()?.name?.lowercase() ?: "system",
                defaultSort = prefs.sortFlow.firstOrNull()?.name?.lowercase() ?: "date_new",
                lastSearch = prefs.lastSearchFlow.firstOrNull() ?: ""
            )

            val root = BackupRoot(
                createdAt = System.currentTimeMillis(),
                settings = settings,
                albums = albums,
                photos = photos
            )

            // Write backup.json
            backupFile.writeText(json.encodeToString(root))

            // Create media directories
            val mediaDir = File(backupDir, MEDIA_FOLDER)
            val photosDir = File(mediaDir, PHOTOS_FOLDER)
            val thumbsDir = File(mediaDir, THUMBS_FOLDER)
            
            photosDir.mkdirs()
            thumbsDir.mkdirs()

            // Copy photo files
            var copiedPhotos = 0
            var missingPhotos = 0
            var copiedThumbs = 0
            var missingThumbs = 0

            photos.forEach { photo: BackupPhoto ->
                // Copy original photo
                val srcPhoto = File(photo.path)
                val dstPhotoDir = File(photosDir, photo.albumId.toString())
                dstPhotoDir.mkdirs()
                val dstPhoto = File(dstPhotoDir, "${photo.id}.jpg")
                
                if (srcPhoto.exists()) {
                    srcPhoto.copyTo(dstPhoto, overwrite = true)
                    copiedPhotos++
                } else {
                    Timber.w("Photo file missing: ${srcPhoto.absolutePath}")
                    missingPhotos++
                }

                // Copy thumbnail if it exists
                val srcThumb = File(photo.thumbPath)
                if (srcThumb.exists()) {
                    val dstThumbDir = File(thumbsDir, photo.albumId.toString())
                    dstThumbDir.mkdirs()
                    val dstThumb = File(dstThumbDir, "${photo.id}.jpg")
                    srcThumb.copyTo(dstThumb, overwrite = true)
                    copiedThumbs++
                } else {
                    missingThumbs++
                }
            }

            val result = BackupResult(
                success = true,
                albumsCount = albums.size,
                photosCount = photos.size,
                photosCopied = copiedPhotos,
                photosMissing = missingPhotos,
                thumbsCopied = copiedThumbs,
                thumbsMissing = missingThumbs,
                backupPath = backupDir.absolutePath,
                message = "Backup completed successfully"
            )

            Timber.i("SimpleBackupManager: Backup completed - ${result.albumsCount} albums, ${result.photosCount} photos")
            result

        } catch (e: Exception) {
            Timber.e(e, "SimpleBackupManager: Backup failed")
            BackupResult(
                success = false,
                albumsCount = 0,
                photosCount = 0,
                photosCopied = 0,
                photosMissing = 0,
                thumbsCopied = 0,
                thumbsMissing = 0,
                backupPath = "",
                message = "Backup failed: ${e.message}"
            )
        }
    }

    /**
     * Restore from the default backup folder using merge mode
     */
    suspend fun restoreBackup(): RestoreResult = withContext(Dispatchers.IO) {
        try {
            Timber.i("SimpleBackupManager: Starting restore from default folder")
            
            val backupDir = getDefaultBackupFolder()
            val backupFile = File(backupDir, BACKUP_JSON_FILE)
            
            if (!backupFile.exists()) {
                return@withContext RestoreResult(
                    success = false,
                    message = "No backup found in default folder"
                )
            }

            val root = json.decodeFromString<BackupRoot>(backupFile.readText())
            require(root.schemaVersion == BACKUP_SCHEMA_VERSION) { "Unsupported backup schema" }

            var albumsInserted = 0
            var albumsUpdated = 0
            var photosInserted = 0
            var photosUpdated = 0
            var photosMissing = 0

            // Upsert albums (merge mode - keep existing, update with newer)
            val albumDao = db.albumDao()
            root.albums.forEach { album: BackupAlbum ->
                val existing = albumDao.getById(album.id)
                if (existing == null) {
                    albumDao.upsert(
                        AlbumEntity(
                            id = album.id,
                            name = album.name,
                            coverPhotoId = album.coverPhotoId,
                            photoCount = album.photoCount,
                            favorite = album.favorite,
                            emoji = album.emoji,
                            updatedAt = album.updatedAt
                        )
                    )
                    albumsInserted++
                } else if (album.updatedAt > existing.updatedAt) {
                    albumDao.update(existing.copy(
                        name = album.name,
                        coverPhotoId = album.coverPhotoId,
                        photoCount = album.photoCount,
                        favorite = album.favorite,
                        emoji = album.emoji,
                        updatedAt = album.updatedAt
                    ))
                    albumsUpdated++
                }
            }

            // Upsert photos and restore files
            val photoDao = db.photoDao()
            val mediaDir = File(backupDir, MEDIA_FOLDER)
            val photosDir = File(mediaDir, PHOTOS_FOLDER)
            val thumbsDir = File(mediaDir, THUMBS_FOLDER)

            root.photos.forEach { photo: BackupPhoto ->
                // Restore photo file
                val srcPhotoDir = File(photosDir, photo.albumId.toString())
                val srcPhoto = File(srcPhotoDir, "${photo.id}.jpg")
                val dstPhoto = storage.photoFile(photo.albumId, photo.id)
                var photoRestored = false

                if (srcPhoto.exists()) {
                    try {
                        dstPhoto.parentFile?.mkdirs()
                        srcPhoto.copyTo(dstPhoto, overwrite = true)
                        photoRestored = true
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to restore photo: ${photo.id}")
                        photosMissing++
                    }
                } else {
                    Timber.w("Source photo not found: ${srcPhoto.absolutePath}")
                    photosMissing++
                }

                // Restore thumbnail
                val srcThumbDir = File(thumbsDir, photo.albumId.toString())
                val srcThumb = File(srcThumbDir, "${photo.id}.jpg")
                val dstThumb = storage.thumbFile(photo.albumId, photo.id)
                var thumbRestored = false

                if (srcThumb.exists()) {
                    try {
                        dstThumb.parentFile?.mkdirs()
                        srcThumb.copyTo(dstThumb, overwrite = true)
                        thumbRestored = true
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to restore thumbnail: ${photo.id}")
                    }
                }

                // Upsert photo record
                val existing = photoDao.getById(photo.id)
                if (existing == null) {
                    photoDao.upsert(
                        PhotoEntity(
                            id = photo.id,
                            albumId = photo.albumId,
                            filename = photo.filename,
                            path = dstPhoto.absolutePath,
                            thumbPath = if (thumbRestored) dstThumb.absolutePath else "",
                            width = photo.width,
                            height = photo.height,
                            sizeBytes = if (photoRestored) dstPhoto.length() else photo.sizeBytes,
                            caption = photo.caption,
                            tags = photo.tags,
                            favorite = photo.favorite,
                            takenAt = photo.takenAt,
                            createdAt = photo.createdAt,
                            updatedAt = photo.updatedAt
                        )
                    )
                    photosInserted++
                } else if (photo.updatedAt > existing.updatedAt) {
                    photoDao.update(existing.copy(
                        albumId = photo.albumId,
                        filename = photo.filename,
                        path = dstPhoto.absolutePath,
                        thumbPath = if (thumbRestored) dstThumb.absolutePath else existing.thumbPath,
                        width = photo.width,
                        height = photo.height,
                        sizeBytes = if (photoRestored) dstPhoto.length() else existing.sizeBytes,
                        caption = photo.caption,
                        tags = photo.tags,
                        favorite = photo.favorite,
                        takenAt = photo.takenAt,
                        createdAt = photo.createdAt,
                        updatedAt = photo.updatedAt
                    ))
                    photosUpdated++
                }
            }

            val result = RestoreResult(
                success = true,
                albumsInserted = albumsInserted,
                albumsUpdated = albumsUpdated,
                photosInserted = photosInserted,
                photosUpdated = photosUpdated,
                photosMissing = photosMissing,
                message = "Restore completed successfully"
            )

            Timber.i("SimpleBackupManager: Restore completed - ${result.albumsInserted}+${result.albumsUpdated} albums, ${result.photosInserted}+${result.photosUpdated} photos")
            result

        } catch (e: Exception) {
            Timber.e(e, "SimpleBackupManager: Restore failed")
            RestoreResult(
                success = false,
                message = "Restore failed: ${e.message}"
            )
        }
    }

    private fun AlbumEntity.toBackupAlbum() = BackupAlbum(
        id = id,
        name = name,
        coverPhotoId = coverPhotoId,
        photoCount = photoCount,
        favorite = favorite,
        emoji = emoji,
        updatedAt = updatedAt
    )

    data class BackupInfo(
        val createdAt: Long,
        val albumsCount: Int,
        val photosCount: Int,
        val totalSizeBytes: Long,
        val backupPath: String
    )

    data class BackupResult(
        val success: Boolean,
        val albumsCount: Int,
        val photosCount: Int,
        val photosCopied: Int,
        val photosMissing: Int,
        val thumbsCopied: Int,
        val thumbsMissing: Int,
        val backupPath: String,
        val message: String
    )

    data class RestoreResult(
        val success: Boolean,
        val albumsInserted: Int = 0,
        val albumsUpdated: Int = 0,
        val photosInserted: Int = 0,
        val photosUpdated: Int = 0,
        val photosMissing: Int = 0,
        val message: String
    )
}
