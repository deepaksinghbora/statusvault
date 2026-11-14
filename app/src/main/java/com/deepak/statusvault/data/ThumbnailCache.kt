package com.deepak.statusvault.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class ThumbResult(
    val imagePath: String,
    val durationMs: Long? = null,
)

class ThumbnailCache(private val context: Context) {

    suspend fun getOrCreate(
        uriString: String,
        name: String,
        isVideo: Boolean,
    ): ThumbResult? = withContext(Dispatchers.IO) {
        val safeName = name.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val outFile = File(context.cacheDir, "thumb_$safeName.jpg")
        val durationFile = File(context.cacheDir, "thumb_$safeName.duration")
        if (outFile.exists() && outFile.length() > 0) {
            val durationMs = if (isVideo) storedOrReadDuration(durationFile, uriString) else null
            return@withContext ThumbResult(outFile.absolutePath, durationMs)
        }

        val localCopy = File(context.cacheDir, "thumb_src_$safeName")
        try {
            context.contentResolver.openInputStream(Uri.parse(uriString))?.use { input ->
                FileOutputStream(localCopy).use { output -> input.copyTo(output) }
            } ?: return@withContext null

            val durationMs = if (isVideo) durationOf(localCopy.absolutePath) else null
            val bitmap: Bitmap? = if (isVideo) {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(localCopy.absolutePath)
                    scaleBitmap(retriever.frameAtTime, 256)
                } finally {
                    retriever.release()
                }
            } else {
                decodeSampledBitmapFromFile(localCopy.absolutePath, 256, 256)
            }

            if (bitmap == null) return@withContext null

            FileOutputStream(outFile).use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, output)
            }
            bitmap.recycle()
            writeDuration(durationFile, durationMs)
            ThumbResult(outFile.absolutePath, durationMs)
        } catch (_: Exception) {
            null
        } finally {
            if (localCopy.exists()) localCopy.delete()
        }
    }

    suspend fun getOrCreateFromFile(
        path: String,
        isVideo: Boolean,
    ): ThumbResult? = withContext(Dispatchers.IO) {
        val source = File(path)
        if (!source.exists()) return@withContext null
        if (!isVideo) return@withContext ThumbResult(source.absolutePath)

        val safeName = source.name.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val outFile = File(context.cacheDir, "saved_thumb_$safeName.jpg")
        val durationMs = durationOf(source.absolutePath)
        if (outFile.exists() && outFile.length() > 0L && outFile.lastModified() >= source.lastModified()) {
            return@withContext ThumbResult(outFile.absolutePath, durationMs)
        }

        try {
            val frame = videoFrame(source) ?: return@withContext null
            val bitmap = scaleBitmap(frame, 512) ?: return@withContext null
            FileOutputStream(outFile).use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)
            }
            bitmap.recycle()
            ThumbResult(outFile.absolutePath, durationMs)
        } catch (_: Exception) {
            null
        }
    }

    private fun durationOf(path: String): Long? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(path)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.takeIf { it > 0L }
        } catch (_: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    private fun storedOrReadDuration(file: File, uriString: String): Long? {
        cachedDuration(file)?.let { return it }
        val durationMs = durationOfUri(uriString) ?: return null
        writeDuration(file, durationMs)
        return durationMs
    }

    private fun durationOfUri(uriString: String): Long? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, Uri.parse(uriString))
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.takeIf { it > 0L }
        } catch (_: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    private fun cachedDuration(file: File): Long? =
        if (file.exists()) file.readText().toLongOrNull()?.takeIf { it > 0L } else null

    private fun writeDuration(file: File, durationMs: Long?) {
        if (durationMs != null) file.writeText(durationMs.toString())
    }

    private fun videoFrame(file: File): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                ThumbnailUtils.createVideoThumbnail(file, Size(512, 512), null)?.let { return it }
            } catch (_: Exception) {
                // Fall through to MediaMetadataRetriever.
            }
        }
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST)
        } catch (_: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    private fun decodeSampledBitmapFromFile(
        path: String,
        reqWidth: Int,
        reqHeight: Int,
    ): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            val full = BitmapFactory.decodeFile(path) ?: return null
            return scaleBitmap(full, reqWidth) ?: full
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds, reqWidth, reqHeight)
            inJustDecodeBounds = false
        }
        return BitmapFactory.decodeFile(path, options)
    }

    private fun scaleBitmap(source: Bitmap?, maxSize: Int): Bitmap? {
        if (source == null) return null
        val largest = maxOf(source.width, source.height)
        if (largest <= maxSize) return source
        val scale = maxSize.toFloat() / largest.toFloat()
        val width = (source.width * scale).toInt().coerceAtLeast(1)
        val height = (source.height * scale).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(source, width, height, true)
        if (scaled != source) source.recycle()
        return scaled
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int,
    ): Int {
        val (height, width) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight &&
                halfWidth / inSampleSize >= reqWidth
            ) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
