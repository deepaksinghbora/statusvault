package com.deepak.statusvault.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

class ShareHelper(private val context: Context) {

    fun shareFile(path: String, isVideo: Boolean) {
        val file = File(path)
        require(file.exists()) { "File not found" }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val mime = if (isVideo) "video/*" else "image/*"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, context.getString(com.deepak.statusvault.R.string.share_via))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
