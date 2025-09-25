package com.example.photoapp10.feature.album.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "albums",
    indices = [
        Index(value = ["name"], unique = false),
        Index(value = ["updatedAt"], unique = false)
    ]
)
data class AlbumEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val coverPhotoId: Long? = null,
    val photoCount: Int = 0,
    val favorite: Boolean = false,
    val emoji: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

