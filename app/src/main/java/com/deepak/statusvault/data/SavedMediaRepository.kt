package com.deepak.statusvault.data

import android.content.Context
import com.deepak.statusvault.domain.SavedItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class SavedMediaRepository(private val context: Context) {

    private fun savedDir(): File {
        val dir = File(context.filesDir, "saved")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun isVideoPath(path: String): Boolean =
        Regex("\\.(mp4|mov|m4v|webm)$", RegexOption.IGNORE_CASE).containsMatchIn(path)

    suspend fun listSaved(): List<SavedItem> = withContext(Dispatchers.IO) {
        val files = savedDir().listFiles()?.filter { it.isFile } ?: emptyList()
        files
            .sortedByDescending { it.name }
            .map { file ->
                SavedItem(
                    path = file.absolutePath,
                    name = file.name,
                    isVideo = isVideoPath(file.absolutePath),
                )
            }
    }

    suspend fun saveFromFile(
        sourcePath: String,
        isVideo: Boolean,
        preferredName: String? = null,
    ): SavedItem = withContext(Dispatchers.IO) {
        val source = File(sourcePath)
        require(source.exists()) { "Source file not found" }

        val ext = when {
            isVideo -> "mp4"
            else -> {
                val fromName = source.extension.lowercase().ifBlank { "jpg" }
                if (fromName == "jpeg") "jpg" else fromName
            }
        }
        val name = "${System.currentTimeMillis()}.$ext"
        val dest = File(savedDir(), name)
        source.copyTo(dest, overwrite = true)
        SavedItem(
            path = dest.absolutePath,
            name = preferredName ?: name,
            isVideo = isVideo,
        )
    }

    suspend fun delete(item: SavedItem): Boolean = withContext(Dispatchers.IO) {
        val file = File(item.path)
        if (file.exists()) file.delete() else false
    }
}
