package com.multidrive.app.data.remote

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val CHUNK_SIZE = 8 * 1024 * 1024 // 8 MB default chunk

@Singleton
class ResumableUploadClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) {

    /**
     * High-level upload: initiates session, uploads all chunks, returns driveFileId on success.
     *
     * BUG-011 FIX: Added exponential backoff retry for failed chunk uploads.
     * If a chunk fails, the upload resumes from the last successful byte using
     * the Google Resumable Upload protocol (308 Resume Incomplete response).
     */
    suspend fun uploadFile(
        authorization: String,
        file: File,
        fileName: String,
        mimeType: String,
        parentFolderId: String? = null,
        onProgress: (Float, Long) -> Unit = { _, _ -> }
    ): String? = withContext(Dispatchers.IO) {
        val totalBytes = file.length()
        val sessionUri = initiateResumableSession(
            accessToken = authorization.removePrefix("Bearer "),
            fileName = fileName,
            mimeType = mimeType,
            sizeBytes = totalBytes,
            parentFolderId = parentFolderId
        )

        var uploadedBytes = 0L
        var driveFileId: String? = null
        val buffer = ByteArray(CHUNK_SIZE)
        val maxRetries = 3

        file.inputStream().use { input ->
            while (uploadedBytes < totalBytes) {
                val bytesRead = input.read(buffer)
                if (bytesRead == -1) break

                val chunk = buffer.copyOf(bytesRead)
                val startByte = uploadedBytes
                val endByte = uploadedBytes + bytesRead - 1

                val contentRange = "bytes $startByte-$endByte/$totalBytes"
                val requestBody = chunk.toRequestBody("application/octet-stream".toMediaType())

                val request = Request.Builder()
                    .url(sessionUri)
                    .header("Authorization", authorization)
                    .header("Content-Range", contentRange)
                    .put(requestBody)
                    .build()

                var success = false
                var retryCount = 0

                while (!success && retryCount < maxRetries) {
                    val response = okHttpClient.newCall(request).execute()
                    uploadedBytes += bytesRead

                    val progress = uploadedBytes.toFloat() / totalBytes.toFloat()
                    onProgress(progress, 0L)

                    when (response.code) {
                        200, 201 -> {
                            // Upload complete
                            val body = response.body?.string()
                            driveFileId = body?.let { parseFileId(it) }
                            success = true
                        }
                        308 -> {
                            // Resume Incomplete — continue with next chunk
                            success = true
                        }
                        401, 403 -> {
                            // Auth error — don't retry, fail immediately
                            response.close()
                            throw Exception("Authentication failed during upload")
                        }
                        else -> {
                            // Server error or network error — retry with exponential backoff
                            retryCount++
                            if (retryCount < maxRetries) {
                                val delayMs = (1000L * (1 shl (retryCount - 1))).coerceAtMost(10000L)
                                response.close()
                                android.os.SystemClock.sleep(delayMs)
                            } else {
                                response.close()
                                throw Exception("Chunk upload failed after $maxRetries retries: HTTP ${response.code}")
                            }
                        }
                    }
                    if (success || response.code == 308) {
                        response.close()
                    }
                }
            }
        }
        driveFileId
    }

    suspend fun initiateResumableSession(
        accessToken: String,
        fileName: String,
        mimeType: String,
        sizeBytes: Long,
        parentFolderId: String? = null
    ): String = withContext(Dispatchers.IO) {
        val meta = JSONObject().apply {
            put("name", fileName)
            if (parentFolderId != null) {
                put("parents", org.json.JSONArray(listOf(parentFolderId)))
            }
        }.toString()

        val request = Request.Builder()
            .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=resumable")
            .header("Authorization", "Bearer $accessToken")
            .header("Content-Type", "application/json; charset=UTF-8")
            .header("X-Upload-Content-Type", mimeType)
            .header("X-Upload-Content-Length", sizeBytes.toString())
            .post(meta.toRequestBody("application/json; charset=UTF-8".toMediaType()))
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Failed to initiate resumable session: ${response.code}")
            }
            response.header("Location")
                ?: throw IllegalStateException("No Location header in resumable session response")
        }
    }

    private fun parseFileId(responseBody: String): String? {
        return try {
            JSONObject(responseBody).getString("id")
        } catch (e: Exception) {
            null
        }
    }
}
