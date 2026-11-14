package com.deepak.statusvault.domain

data class StatusItem(
    val uri: String,
    val name: String,
    val mimeType: String,
    val lastModifiedMs: Long,
) {
    val isVideo: Boolean get() = mimeType.startsWith("video/")
}

data class SavedItem(
    val path: String,
    val name: String,
    val isVideo: Boolean,
)

enum class MediaFilter {
    ALL,
    IMAGES,
    VIDEOS,
}

enum class DateSort {
    NEWEST,
    OLDEST,
}

data class PreviewItem(
    val path: String? = null,
    val uri: String? = null,
    val name: String,
    val isVideo: Boolean,
    val title: String = name,
)
