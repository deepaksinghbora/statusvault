package com.deepak.statusvault.data

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

class GalleryExporter(private val context: Context) {

    suspend fun saveToGallery(path: String, isVideo: Boolean): String = withContext(Dispatchers.IO) {
        val file = File(path)
        if (!file.exists()) throw IllegalStateException("File not found")

        val extension = file.extension.ifBlank { if (isVideo) "mp4" else "jpg" }
        val mime = MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(extension.lowercase())
            ?: if (isVideo) "video/mp4" else "image/jpeg"

        val collection = if (isVideo) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
        }

        val values = ContentValues().apply {
            put(
                MediaStore.MediaColumns.DISPLAY_NAME,
                "StatusVault_${System.currentTimeMillis()}.$extension",
            )
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val relative = if (isVideo) {
                    Environment.DIRECTORY_MOVIES + "/StatusVault"
                } else {
                    Environment.DIRECTORY_PICTURES + "/StatusVault"
                }
                put(MediaStore.MediaColumns.RELATIVE_PATH, relative)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val itemUri = resolver.insert(collection, values)
            ?: throw IllegalStateException("Unable to create gallery entry")

        resolver.openOutputStream(itemUri)?.use { output ->
            FileInputStream(file).use { input -> input.copyTo(output) }
        } ?: throw IllegalStateException("Unable to write gallery file")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(itemUri, values, null, null)
        }

        itemUri.toString()
    }
}
