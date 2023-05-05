package com.angcyo.exoplayer

import android.content.Context
import android.net.Uri
import androidx.fragment.app.FragmentManager
import androidx.media3.common.MediaItem
import androidx.media3.common.util.Log
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadIndex
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadRequest
import java.io.IOException
import java.util.concurrent.CopyOnWriteArraySet


/**
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2023/05/05
 */

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class DownloadTracker(
    context: Context,
    dataSourceFactory: DataSource.Factory?,
    downloadManager: DownloadManager
) {

    /** Listens for changes in the tracked downloads.  */
    interface Listener {
        /** Called when the tracked downloads changed.  */
        fun onDownloadsChanged()
    }

    private val TAG = "DownloadTracker"

    private var context = context.applicationContext
    private var dataSourceFactory: DataSource.Factory? = null
    private val listeners: CopyOnWriteArraySet<Listener> = CopyOnWriteArraySet()
    private val downloads: HashMap<Uri, Download> = hashMapOf()
    private var downloadIndex: DownloadIndex? = null

    init {
        this.dataSourceFactory = dataSourceFactory
        downloadIndex = downloadManager.downloadIndex
        downloadManager.addListener(DownloadManagerListener())
        loadDownloads()
    }

    fun addListener(listener: Listener) {
        listeners.add(listener)
    }

    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    fun isDownloaded(mediaItem: MediaItem): Boolean {
        val download = downloads[checkNotNull(mediaItem.localConfiguration).uri]
        return download != null && download.state != Download.STATE_FAILED
    }

    fun getDownloadRequest(uri: Uri): DownloadRequest? {
        val download = downloads[uri]
        return if (download != null && download.state != Download.STATE_FAILED) download.request else null
    }

    fun toggleDownload(
        fragmentManager: FragmentManager?,
        mediaItem: MediaItem,
        renderersFactory: RenderersFactory?
    ) {
        val download = downloads[checkNotNull(mediaItem.localConfiguration).uri]
        if (download != null && download.state != Download.STATE_FAILED) {
            /*DownloadService.sendRemoveDownload(
                context,
                DemoDownloadService::class.java,
                download.request.id,  *//* foreground= *//*
                false
            )*/
        } else {
            /*if (startDownloadDialogHelper != null) {
                startDownloadDialogHelper!!.release()
            }
            startDownloadDialogHelper = StartDownloadDialogHelper(
                fragmentManager,
                DownloadHelper.forMediaItem(
                    context,
                    mediaItem,
                    renderersFactory,
                    dataSourceFactory
                ),
                mediaItem
            )*/
        }
    }

    private fun loadDownloads() {
        try {
            downloadIndex?.getDownloads()?.use { loadedDownloads ->
                while (loadedDownloads.moveToNext()) {
                    val download = loadedDownloads.download
                    downloads[download.request.uri] = download
                }
            }
        } catch (e: IOException) {
            Log.w(TAG, "Failed to query downloads", e)
        }
    }

    private inner class DownloadManagerListener : DownloadManager.Listener {

        override fun onDownloadChanged(
            downloadManager: DownloadManager,
            download: Download,
            finalException: java.lang.Exception?
        ) {
            downloads[download.request.uri] = download
            for (listener in listeners) {
                listener.onDownloadsChanged()
            }
        }

        override fun onDownloadRemoved(downloadManager: DownloadManager, download: Download) {
            downloads.remove(download.request.uri)
            for (listener in listeners) {
                listener.onDownloadsChanged()
            }
        }
    }
}