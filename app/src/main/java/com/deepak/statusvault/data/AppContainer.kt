package com.deepak.statusvault.data

import android.content.Context

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val folderStore = StatusFolderStore(appContext)
    val themeStore = ThemeStore(appContext)
    val thumbnailCache = ThumbnailCache(appContext)
    val whatsAppRepository = WhatsAppStatusRepository(appContext, folderStore, thumbnailCache)
    val savedRepository = SavedMediaRepository(appContext)
    val galleryExporter = GalleryExporter(appContext)
    val shareHelper = ShareHelper(appContext)
}
