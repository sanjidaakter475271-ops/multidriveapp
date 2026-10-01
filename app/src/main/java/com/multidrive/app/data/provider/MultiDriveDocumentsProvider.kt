package com.multidrive.app.data.provider

import android.database.Cursor
import android.database.MatrixCursor
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract.Document
import android.provider.DocumentsContract.Root
import android.provider.DocumentsProvider
import com.multidrive.app.R
import com.multidrive.app.data.local.MultiDriveDatabase
import com.multidrive.app.data.local.entity.AccountEntity
import com.multidrive.app.data.local.entity.FileMetaEntity
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import java.io.FileNotFoundException

/**
 * DocumentsProvider implementation for MultiDrive.
 *
 * This makes MultiDrive appear as a storage provider in:
 * - Android file picker (ACTION_OPEN_DOCUMENT, ACTION_CREATE_DOCUMENT)
 * - Google Files app → Browse → MultiDrive
 * - Any SAF-compatible file manager
 *
 * Each connected Google account appears as a separate Root.
 * Files inside each account appear as Documents.
 */
class MultiDriveDocumentsProvider : DocumentsProvider() {

    companion object {
        const val AUTHORITY = "com.multidrive.app.documents"

        // Columns returned for roots
        private val DEFAULT_ROOT_PROJECTION = arrayOf(
            Root.COLUMN_ROOT_ID,
            Root.COLUMN_MIME_TYPES,
            Root.COLUMN_FLAGS,
            Root.COLUMN_ICON,
            Root.COLUMN_TITLE,
            Root.COLUMN_SUMMARY,
            Root.COLUMN_DOCUMENT_ID,
            Root.COLUMN_AVAILABLE_BYTES
        )

        // Columns returned for documents (files/folders)
        private val DEFAULT_DOCUMENT_PROJECTION = arrayOf(
            Document.COLUMN_DOCUMENT_ID,
            Document.COLUMN_MIME_TYPE,
            Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_LAST_MODIFIED,
            Document.COLUMN_FLAGS,
            Document.COLUMN_SIZE
        )

        // Document ID format:
        // Root (account) :  "root:<accountId>"
        // Folder         :  "<accountId>:<driveFileId>"
        // File           :  "<accountId>:<driveFileId>"
        private const val DOC_ID_ROOT_PREFIX = "root:"

        private fun rootDocId(accountId: Int) = "$DOC_ID_ROOT_PREFIX$accountId"
        private fun fileDocId(accountId: Int, driveFileId: String) = "$accountId:$driveFileId"
    }

    private lateinit var db: MultiDriveDatabase

    override fun onCreate(): Boolean {
        // Manually obtain the database without Hilt (provider starts before Application)
        db = MultiDriveDatabase.buildManually(requireContext())
        return true
    }

    override fun queryRoots(projection: Array<out String>?): Cursor {
        val cols = projection ?: DEFAULT_ROOT_PROJECTION
        val cursor = MatrixCursor(cols)

        val accounts = runBlocking {
            db.accountDao().getAllAccountsSuspend()
        }

        for (account in accounts) {
            val available = if (account.storageQuota > 0)
                account.storageQuota - account.storageUsed
            else 0L

            val flags = Root.FLAG_SUPPORTS_CREATE or
                    Root.FLAG_SUPPORTS_RECENTS or
                    Root.FLAG_SUPPORTS_SEARCH

            cursor.newRow().apply {
                add(Root.COLUMN_ROOT_ID, "root_${account.id}")
                add(Root.COLUMN_DOCUMENT_ID, rootDocId(account.id))
                add(Root.COLUMN_TITLE, "MultiDrive")
                add(Root.COLUMN_SUMMARY, account.email)
                add(Root.COLUMN_FLAGS, flags)
                add(Root.COLUMN_ICON, R.mipmap.ic_launcher)
                add(Root.COLUMN_MIME_TYPES, "*/*")
                add(Root.COLUMN_AVAILABLE_BYTES, available)
            }
        }

        return cursor
    }

    override fun queryDocument(documentId: String, projection: Array<out String>?): Cursor {
        val cols = projection ?: DEFAULT_DOCUMENT_PROJECTION
        val cursor = MatrixCursor(cols)

        if (documentId.startsWith(DOC_ID_ROOT_PREFIX)) {
            // This is the root "folder" for an account
            val accountId = documentId.removePrefix(DOC_ID_ROOT_PREFIX).toIntOrNull() ?: return cursor
            val account = runBlocking { db.accountDao().getAccountById(accountId) } ?: return cursor

            cursor.newRow().apply {
                add(Document.COLUMN_DOCUMENT_ID, documentId)
                add(Document.COLUMN_DISPLAY_NAME, account.email)
                add(Document.COLUMN_MIME_TYPE, Document.MIME_TYPE_DIR)
                add(Document.COLUMN_FLAGS, Document.FLAG_DIR_SUPPORTS_CREATE)
                add(Document.COLUMN_LAST_MODIFIED, account.lastSynced)
                add(Document.COLUMN_SIZE, null)
            }
        } else {
            // Regular file or folder
            val parts = documentId.split(":", limit = 2)
            if (parts.size != 2) return cursor
            val accountId = parts[0].toIntOrNull() ?: return cursor
            val driveFileId = parts[1]

            val file = runBlocking { db.fileMetaDao().getFileByDriveId(driveFileId, accountId) }
                ?: return cursor

            addFileRow(cursor, accountId, file)
        }

        return cursor
    }

    override fun queryChildDocuments(
        parentDocumentId: String,
        projection: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        val cols = projection ?: DEFAULT_DOCUMENT_PROJECTION
        val cursor = MatrixCursor(cols)

        val (accountId, parentDriveId) = parseDocumentId(parentDocumentId)

        val files = runBlocking {
            if (parentDriveId == null) {
                // Root of account — show root-level files
                db.fileMetaDao().getRootFilesSuspend(accountId)
            } else {
                // Child files of a folder
                db.fileMetaDao().getFilesByParentSuspend(parentDriveId, accountId)
            }
        }

        for (file in files) {
            addFileRow(cursor, accountId, file)
        }

        return cursor
    }

    override fun openDocument(
        documentId: String,
        mode: String,
        signal: CancellationSignal?
    ): ParcelFileDescriptor {
        // For now, throw if we can't serve the file directly.
        // In production, download to cache and serve via pipe.
        throw FileNotFoundException("Direct file serving not implemented. Use download instead.")
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun parseDocumentId(documentId: String): Pair<Int, String?> {
        return if (documentId.startsWith(DOC_ID_ROOT_PREFIX)) {
            val accountId = documentId.removePrefix(DOC_ID_ROOT_PREFIX).toIntOrNull() ?: 0
            Pair(accountId, null)
        } else {
            val parts = documentId.split(":", limit = 2)
            val accountId = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val driveFileId = parts.getOrNull(1)
            Pair(accountId, driveFileId)
        }
    }

    private fun addFileRow(cursor: MatrixCursor, accountId: Int, file: FileMetaEntity) {
        val mimeType = if (file.isFolder) Document.MIME_TYPE_DIR else file.mimeType
        val flags = buildInt {
            if (file.isFolder) add(Document.FLAG_DIR_SUPPORTS_CREATE)
            if (!file.isFolder) add(Document.FLAG_SUPPORTS_WRITE)
            add(Document.FLAG_SUPPORTS_DELETE)
            add(Document.FLAG_SUPPORTS_RENAME)
        }

        cursor.newRow().apply {
            add(Document.COLUMN_DOCUMENT_ID, fileDocId(accountId, file.driveFileId))
            add(Document.COLUMN_DISPLAY_NAME, file.name)
            add(Document.COLUMN_MIME_TYPE, mimeType)
            add(Document.COLUMN_FLAGS, flags)
            add(Document.COLUMN_LAST_MODIFIED, file.modifiedTime)
            add(Document.COLUMN_SIZE, if (file.isFolder) null else file.size)
        }
    }
}

/** Helper to build a flag Int from multiple flag additions. */
private fun buildInt(block: MutableList<Int>.() -> Unit): Int {
    val list = mutableListOf<Int>()
    list.block()
    return list.fold(0) { acc, flag -> acc or flag }
}
