package com.multidrive.app.data.remote

sealed class DriveError(message: String) : Exception(message) {
    data object NetworkUnavailable : DriveError("Network unavailable.")
    data object Unauthorized : DriveError("Authentication required or token expired (401).")
    data object QuotaExceeded : DriveError("Google Drive storage quota exceeded (403).")
    data object PermissionDenied : DriveError("Permission denied (403).")
    data object FileNotFound : DriveError("File or folder not found (404).")
    data object RateLimited : DriveError("Rate limit exceeded (429). Please wait before retrying.")
    data object ServerUnavailable : DriveError("Google Drive server unavailable (5xx).")
    data class Unknown(val errMessage: String) : DriveError(errMessage)
}

object DriveErrorMapper {
    fun mapCode(code: Int, message: String? = null): DriveError {
        return when (code) {
            401 -> DriveError.Unauthorized
            403 -> if (message?.contains("quota", true) == true) DriveError.QuotaExceeded else DriveError.PermissionDenied
            404 -> DriveError.FileNotFound
            429 -> DriveError.RateLimited
            in 500..599 -> DriveError.ServerUnavailable
            else -> DriveError.Unknown(message ?: "HTTP Error $code")
        }
    }
}
