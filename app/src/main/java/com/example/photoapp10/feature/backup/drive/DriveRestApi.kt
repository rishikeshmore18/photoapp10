package com.example.photoapp10.feature.backup.drive

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

data class DriveFileList(
    val files: List<DriveFile>?
)

data class DriveFile(
    val id: String,
    val name: String,
    val modifiedTime: String?
)

data class DriveFileMetadata(
    val name: String,
    val parents: List<String>,
    val mimeType: String? = null
)

interface DriveRestApi {
    @GET("drive/v3/files")
    suspend fun listFiles(
        @Header("Authorization") auth: String,
        @Query("spaces") spaces: String = "appDataFolder",
        @Query("q") query: String? = null,
        @Query("fields") fields: String = "files(id,name,modifiedTime)",
        @Query("orderBy") orderBy: String? = null,
        @Query("pageSize") pageSize: Int = 10
    ): Response<DriveFileList>

    @GET("drive/v3/files/{fileId}")
    suspend fun downloadFile(
        @Header("Authorization") auth: String,
        @Path("fileId") fileId: String,
        @Query("alt") alt: String = "media"
    ): Response<ResponseBody>

    @Multipart
    @POST("upload/drive/v3/files")
    suspend fun uploadFile(
        @Header("Authorization") auth: String,
        @Query("uploadType") uploadType: String = "multipart",
        @Part metadata: MultipartBody.Part,
        @Part file: MultipartBody.Part
    ): Response<DriveFile>

    @Multipart
    @PUT("upload/drive/v3/files/{fileId}")
    suspend fun updateFile(
        @Header("Authorization") auth: String,
        @Path("fileId") fileId: String,
        @Query("uploadType") uploadType: String = "media",
        @Part file: MultipartBody.Part
    ): Response<DriveFile>
}

