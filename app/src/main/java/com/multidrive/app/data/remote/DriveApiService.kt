package com.multidrive.app.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

data class DriveFileListResponse(
    val files: List<DriveFileDto> = emptyList(),
    val nextPageToken: String? = null
)

data class DriveFileDto(
    val id: String,
    val name: String,
    val mimeType: String,
    val size: Long? = null,
    val parents: List<String>? = null,
    val createdTime: String? = null,
    val modifiedTime: String? = null,
    val trashed: Boolean? = null,
    val webViewLink: String? = null,
    val webContentLink: String? = null
)

data class DriveAboutResponse(
    val storageQuota: DriveStorageQuota? = null,
    val user: DriveUserDto? = null
)

data class DriveStorageQuota(
    val limit: Long? = null,
    val usage: Long? = null,
    val usageInDrive: Long? = null,
    val usageInDriveTrash: Long? = null
)

data class DriveUserDto(
    val displayName: String? = null,
    val emailAddress: String? = null,
    val photoLink: String? = null,
    val permissionId: String? = null
)

interface DriveApiService {

    @GET("files")
    suspend fun listFiles(
        @Header("Authorization") authorization: String,
        @Query("q") query: String? = null,
        @Query("pageSize") pageSize: Int = 100,
        @Query("pageToken") pageToken: String? = null,
        @Query("fields") fields: String = "nextPageToken,files(id,name,mimeType,size,parents,createdTime,modifiedTime,trashed,webViewLink,webContentLink)"
    ): DriveFileListResponse

    @GET("about")
    suspend fun getAbout(
        @Header("Authorization") authorization: String,
        @Query("fields") fields: String = "storageQuota,user"
    ): DriveAboutResponse

    @GET("files/{fileId}")
    suspend fun getFileMetadata(
        @Header("Authorization") authorization: String,
        @Path("fileId") fileId: String,
        @Query("fields") fields: String = "id,name,mimeType,size,parents,createdTime,modifiedTime,trashed,webViewLink,webContentLink"
    ): DriveFileDto

    @DELETE("files/{fileId}")
    suspend fun deleteFile(
        @Header("Authorization") authorization: String,
        @Path("fileId") fileId: String
    ): Response<Void>

    @PATCH("files/{fileId}")
    suspend fun updateFile(
        @Header("Authorization") authorization: String,
        @Path("fileId") fileId: String,
        @Body body: RequestMap
    ): DriveFileDto

    @POST("files/{fileId}/permissions")
    suspend fun createPermission(
        @Header("Authorization") authorization: String,
        @Path("fileId") fileId: String,
        @Body body: RequestMap
    ): Response<ResponseBody>

    @POST("files")
    suspend fun createFile(
        @Header("Authorization") authorization: String,
        @Body body: RequestMap
    ): DriveFileDto

    @GET("files/{fileId}")
    @Streaming
    suspend fun downloadFile(
        @Header("Authorization") authorization: String,
        @Path("fileId") fileId: String,
        @Query("alt") alt: String = "media"
    ): ResponseBody

    @GET("changes/startPageToken")
    suspend fun getStartPageToken(
        @Header("Authorization") authorization: String
    ): ResponseBody

    @GET("changes")
    suspend fun getChanges(
        @Header("Authorization") authorization: String,
        @Query("pageToken") pageToken: String
    ): ResponseBody
}

typealias RequestMap = Map<String, Any>
