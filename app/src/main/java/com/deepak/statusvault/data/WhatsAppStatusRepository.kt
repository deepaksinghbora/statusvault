package com.deepak.statusvault.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import com.deepak.statusvault.domain.StatusItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class WhatsAppStatusRepository(
    private val context: Context,
    private val folderStore: StatusFolderStore,
    private val thumbnailCache: ThumbnailCache,
) {

    suspend fun hasFolder(): Boolean = !folderStore.getTreeUri().isNullOrBlank()

    suspend fun persistTreeUri(uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        } catch (_: SecurityException) {
            // Some providers may not support persistable grants.
        }
        folderStore.setTreeUri(uri.toString())
    }

    suspend fun listStatuses(): List<StatusItem> = withContext(Dispatchers.IO) {
        val stored = folderStore.getTreeUri()
            ?: throw IllegalStateException("Select a readable WhatsApp statuses folder.")
        val treeUri = Uri.parse(stored)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri),
        )

        val items = mutableListOf<StatusItem>()
        context.contentResolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            ),
            null,
            null,
            null,
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIdx =
                cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeIdx =
                cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val modifiedIdx =
                cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
            while (cursor.moveToNext()) {
                val mime = cursor.getString(mimeIdx) ?: continue
                if (!mime.startsWith("image/") && !mime.startsWith("video/")) continue
                val docId = cursor.getString(idIdx)
                val name = cursor.getString(nameIdx) ?: "status"
                val modified = if (modifiedIdx >= 0) cursor.getLong(modifiedIdx) else 0L
                val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                items.add(
                    StatusItem(
                        uri = docUri.toString(),
                        name = name,
                        mimeType = mime,
                        lastModifiedMs = modified,
                    ),
                )
            }
        }
        items.sortedByDescending { it.lastModifiedMs }
    }

    suspend fun copyStatusToCache(item: StatusItem): String = withContext(Dispatchers.IO) {
        val safeName = item.name.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val outFile = File(context.cacheDir, "status_$safeName")
        context.contentResolver.openInputStream(Uri.parse(item.uri))?.use { input ->
            FileOutputStream(outFile).use { output -> input.copyTo(output) }
        } ?: throw IllegalStateException("Unable to read status file.")
        outFile.absolutePath
    }

    suspend fun thumbnailFor(item: StatusItem): ThumbResult? =
        thumbnailCache.getOrCreate(item.uri, item.name, item.isVideo)
}
