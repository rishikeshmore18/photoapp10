package com.example.photoapp10.feature.backup.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.photoapp10.core.di.Modules
import com.example.photoapp10.feature.backup.drive.DriveUploader
import com.example.photoapp10.feature.backup.domain.BackupBuilders.backupRootFromDb
import com.example.photoapp10.feature.backup.drive.driveAppDataOrNull
import com.example.photoapp10.feature.settings.data.UserPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

class DriveSyncWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Timber.d("DriveSyncWorker: Starting sync work")
            
            // Check if cancelled early
            ensureActive()
            
            val prefs = UserPrefs(context)
            val db = Modules.provideDb(context)
            val storage = Modules.provideStorage(context)

            // Must be signed in
            val driveAppData = driveAppDataOrNull(context)
            if (driveAppData == null) {
                Timber.w("DriveSyncWorker: Drive service not available, retrying later")
                return@withContext Result.retry()
            }

            // Check if cancelled before major operations
            ensureActive()

            // 1) Build backup.json (always upload)
            Timber.d("DriveSyncWorker: Building backup.json from database")
            val root = backupRootFromDb(db)
            val json = Json { prettyPrint = true; encodeDefaults = true }
            val jsonBytes = json.encodeToString(root).toByteArray()

            // Check if cancelled
            ensureActive()

            val uploader = DriveUploader(driveAppData.drive)
            Timber.d("DriveSyncWorker: Uploading backup.json (${jsonBytes.size} bytes)")
            uploader.putBackupJson(jsonBytes)

            // Check if cancelled
            ensureActive()

            // 2) Upload changed/new media since lastSyncedAt
            val lastTs = prefs.lastSyncedAtFlow.first()
            Timber.d("DriveSyncWorker: Checking for photos changed since $lastTs")
            
            val changed = db.photoDao().getChangedSince(lastTs)
            Timber.d("DriveSyncWorker: Found ${changed.size} changed photos to upload")

            // Limit concurrent uploads to prevent memory pressure
            val maxConcurrentUploads = minOf(changed.size, 3) // Max 3 concurrent uploads
            val chunkedPhotos = changed.chunked(maxConcurrentUploads)

            for (chunk in chunkedPhotos) {
                // Check if cancelled before each chunk
                ensureActive()
                
                chunk.forEachIndexed { index, p ->
                    try {
                        // Check if cancelled before each upload
                        ensureActive()
                        
                        val file = storage.photoFile(p.albumId, p.id)
                        if (file.exists()) {
                            val globalIndex = changed.indexOf(p) + 1
                            Timber.d("DriveSyncWorker: Uploading photo ${p.filename} ($globalIndex/${changed.size})")
                            uploader.putPhoto(file, p.albumId, p.id)
                        } else {
                            Timber.w("DriveSyncWorker: Photo file not found: ${file.absolutePath}")
                        }
                    } catch (e: CancellationException) {
                        Timber.d("DriveSyncWorker: Upload cancelled for photo ${p.id}")
                        throw e // Re-throw to cancel the entire job
                    } catch (e: Exception) {
                        Timber.e(e, "DriveSyncWorker: Failed to upload photo ${p.id}")
                        // Continue with other photos for non-cancellation errors
                    }
                }
            }

            // Check if cancelled before final operations
            ensureActive()

            // 3) Mark synced
            val now = System.currentTimeMillis()
            prefs.setLastSyncedAt(now)
            Timber.d("DriveSyncWorker: Sync completed, updated lastSyncedAt to $now")

            // 4) Notify manager (best-effort; if not injected, swallow)
            runCatching {
                Modules.provideDriveSyncManager(context).onWorkerFinished(true)
            }.onFailure { e ->
                Timber.w(e, "DriveSyncWorker: Failed to notify sync manager")
            }

            Timber.i("DriveSyncWorker: Sync work completed successfully")
            Result.success()
            
        } catch (e: CancellationException) {
            Timber.d("DriveSyncWorker: Sync work cancelled")
            
            // Notify manager of cancellation (treat as failure)
            runCatching {
                Modules.provideDriveSyncManager(context).onWorkerFinished(false)
            }
            
            // Return failure for cancellation to prevent automatic retry
            Result.failure()
            
        } catch (e: Exception) {
            Timber.e(e, "DriveSyncWorker: Sync work failed")
            
            // Notify manager of failure
            runCatching {
                Modules.provideDriveSyncManager(context).onWorkerFinished(false)
            }
            
            Result.retry()
        }
    }
}
