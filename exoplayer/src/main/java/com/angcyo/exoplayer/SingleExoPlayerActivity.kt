package com.angcyo.exoplayer

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ErrorMessageProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.util.Util
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.drm.DefaultDrmSessionManagerProvider
import androidx.media3.exoplayer.drm.FrameworkMediaDrm
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.util.DebugTextViewHelper
import androidx.media3.exoplayer.util.EventLogger
import androidx.media3.ui.PlayerView
import com.angcyo.exoplayer.IntentUtil.createMediaItemsFromIntent
import com.angcyo.library.component.lastContext
import com.angcyo.library.ex.baseConfig
import com.angcyo.library.ex.toUri
import com.angcyo.library.toastQQ
import kotlin.math.max


/**
 * 简单的Exo播放器
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2023/05/05
 */
class SingleExoPlayerActivity : AppCompatActivity(), PlayerView.ControllerVisibilityListener {

    companion object {

        fun play(url: String, context: Context = lastContext) {
            play(url.toUri(), context)
        }

        /**播放单个视频*/
        fun play(uri: Uri?, context: Context = lastContext) {
            uri ?: return
            val intent = Intent(context, SingleExoPlayerActivity::class.java)
            intent.baseConfig(context)
            intent.action = IntentUtil.ACTION_VIEW
            intent.data = uri
            context.startActivity(intent)
        }

        /**播放多个视频*/
        fun playList(uriList: List<Uri>, context: Context = lastContext) {
            val intent = Intent(context, SingleExoPlayerActivity::class.java)
            intent.baseConfig(context)
            intent.action = IntentUtil.ACTION_VIEW_LIST
            for ((uri, index) in uriList.withIndex()) {
                val key = "${IntentUtil.URI_EXTRA}_$index"
                intent.putExtra(key, "$uri")
            }
            context.startActivity(intent)
        }
    }

    // Saved instance state keys.

    private val KEY_TRACK_SELECTION_PARAMETERS = "track_selection_parameters"
    private val KEY_ITEM_INDEX = "item_index"
    private val KEY_POSITION = "position"
    private val KEY_AUTO_PLAY = "auto_play"

    protected var playerView: PlayerView? = null
    protected var debugRootView: LinearLayout? = null
    protected var debugTextView: TextView? = null

    protected var player: ExoPlayer? = null

    private var selectTracksButton: Button? = null
    private var dataSourceFactory: DataSource.Factory? = null
    private var mediaItems: List<MediaItem>? = null
    private var trackSelectionParameters: TrackSelectionParameters? = null
    private var debugViewHelper: DebugTextViewHelper? = null
    private var lastSeenTracks: Tracks? = null
    private var startAutoPlay = false
    private var startItemIndex = 0
    private var startPosition: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dataSourceFactory = ExoUtil.getDataSourceFactory(/* context= */ this)

        setContentView(R.layout.activity_single_exo_player)

        debugRootView = findViewById(R.id.controls_root)
        debugTextView = findViewById(R.id.debug_text_view)
        selectTracksButton = findViewById(R.id.select_tracks_button)

        playerView = findViewById(R.id.player_view)
        playerView?.setControllerVisibilityListener(this)
        playerView?.setErrorMessageProvider(PlayerErrorMessageProvider())
        playerView?.requestFocus()

        if (savedInstanceState != null) {
            trackSelectionParameters = TrackSelectionParameters.fromBundle(
                savedInstanceState.getBundle(KEY_TRACK_SELECTION_PARAMETERS)!!
            )
            startAutoPlay = savedInstanceState.getBoolean(KEY_AUTO_PLAY)
            startItemIndex = savedInstanceState.getInt(KEY_ITEM_INDEX)
            startPosition = savedInstanceState.getLong(KEY_POSITION)
        } else {
            trackSelectionParameters = TrackSelectionParameters.Builder( /* context= */this).build()
            clearStartPosition()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        releasePlayer()
        clearStartPosition()
        setIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        if (Build.VERSION.SDK_INT > 23) {
            initializePlayer()
            playerView?.onResume()
        }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT <= 23 || player == null) {
            initializePlayer()
            playerView?.onResume()
        }
    }

    override fun onPause() {
        super.onPause()
        if (Build.VERSION.SDK_INT <= 23) {
            playerView?.onPause()
            releasePlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        if (Build.VERSION.SDK_INT > 23) {
            playerView?.onPause()
            releasePlayer()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isEmpty()) {
            // Empty results are triggered if a permission is requested while another request was already
            // pending and can be safely ignored in this case.
            return
        }
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initializePlayer()
        } else {
            toastQQ("Permission to access storage was denied")
            finish()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        updateTrackSelectorParameters()
        updateStartPosition()
        outState.putBundle(KEY_TRACK_SELECTION_PARAMETERS, trackSelectionParameters!!.toBundle())
        outState.putBoolean(KEY_AUTO_PLAY, startAutoPlay)
        outState.putInt(KEY_ITEM_INDEX, startItemIndex)
        outState.putLong(KEY_POSITION, startPosition)
    }

    //---

    protected fun clearStartPosition() {
        startAutoPlay = true
        startItemIndex = C.INDEX_UNSET
        startPosition = C.TIME_UNSET
    }

    protected fun releasePlayer() {
        if (player != null) {
            updateTrackSelectorParameters()
            updateStartPosition()
            debugViewHelper!!.stop()
            debugViewHelper = null
            player?.release()
            player = null
            playerView?.player = null
            mediaItems = emptyList()
        }
    }

    private fun updateTrackSelectorParameters() {
        if (player != null) {
            trackSelectionParameters = player?.trackSelectionParameters
        }
    }

    private fun updateStartPosition() {
        player?.let { player ->
            startAutoPlay = player.playWhenReady
            startItemIndex = player.currentMediaItemIndex
            startPosition = max(0, player.contentPosition)
        }
    }

    /**
     * @return Whether initialization was successful.
     */
    protected fun initializePlayer(): Boolean {
        if (player == null) {
            val intent = intent
            mediaItems = createMediaItems(intent)
            if (mediaItems.isNullOrEmpty()) {
                return false
            }
            lastSeenTracks = Tracks.EMPTY
            val playerBuilder = ExoPlayer.Builder( /* context= */this)
                .setMediaSourceFactory(createMediaSourceFactory())
            setRenderersFactory(
                playerBuilder,
                intent.getBooleanExtra(IntentUtil.PREFER_EXTENSION_DECODERS_EXTRA, false)
            )
            player = playerBuilder.build()
            player?.trackSelectionParameters = trackSelectionParameters!!
            player?.addListener(PlayerEventListener())
            player?.addAnalyticsListener(EventLogger())
            player?.setAudioAttributes(AudioAttributes.DEFAULT,  /* handleAudioFocus= */true)
            player?.playWhenReady = startAutoPlay
            playerView?.player = player
            debugViewHelper = DebugTextViewHelper(player!!, debugTextView!!)
            debugViewHelper?.start()
        }
        val haveStartPosition = startItemIndex != C.INDEX_UNSET
        if (haveStartPosition) {
            player?.seekTo(startItemIndex, startPosition)
        }
        player?.setMediaItems(mediaItems!!,  /* resetPosition= */!haveStartPosition)
        player?.prepare()
        updateButtonVisibility()
        return true
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun setRenderersFactory(
        playerBuilder: ExoPlayer.Builder,
        preferExtensionDecoders: Boolean
    ) {
        val renderersFactory: RenderersFactory =
            ExoUtil.buildRenderersFactory( /* context= */this, preferExtensionDecoders)
        playerBuilder.setRenderersFactory(renderersFactory)
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class) // SSAI configuration
    private fun createMediaSourceFactory(): MediaSource.Factory {
        val drmSessionManagerProvider = DefaultDrmSessionManagerProvider()
        drmSessionManagerProvider.setDrmHttpDataSourceFactory(
            ExoUtil.getHttpDataSourceFactory( /* context= */this)
        )
        return DefaultMediaSourceFactory( /* context= */this)
            .setDataSourceFactory(dataSourceFactory!!)
            .setDrmSessionManagerProvider(drmSessionManagerProvider)
    }

    private fun createMediaItems(intent: Intent): List<MediaItem> {
        val action = intent.action
        val actionIsListView: Boolean = IntentUtil.ACTION_VIEW_LIST == action
        if (!actionIsListView && IntentUtil.ACTION_VIEW != action) {
            toastQQ("Unexpected intent action:${action}")
            finish()
            return emptyList()
        }
        val mediaItems = createMediaItems(intent, ExoUtil.getDownloadTracker( /* context= */this))
        for (i in mediaItems.indices) {
            val mediaItem = mediaItems[i]
            if (!Util.checkCleartextTrafficPermitted(mediaItem)) {
                toastQQ("Cleartext HTTP traffic not permitted. See https://developer.android.com/guide/topics/media/issues/cleartext-not-permitted")
                finish()
                return emptyList()
            }
            if (Util.maybeRequestReadExternalStoragePermission( /* activity= */this, mediaItem)) {
                // The player will be reinitialized if the permission is granted.
                return emptyList()
            }
            val drmConfiguration = mediaItem.localConfiguration!!.drmConfiguration
            if (drmConfiguration != null) {
                if (Build.VERSION.SDK_INT < 18) {
                    toastQQ("DRM content not supported on API levels below 18")
                    finish()
                    return emptyList()
                } else if (!FrameworkMediaDrm.isCryptoSchemeSupported(drmConfiguration.scheme)) {
                    toastQQ("This device does not support the required DRM scheme")
                    finish()
                    return emptyList()
                }
            }
        }
        return mediaItems
    }

    private fun createMediaItems(
        intent: Intent,
        downloadTracker: DownloadTracker
    ): List<MediaItem> {
        val mediaItems = mutableListOf<MediaItem>()
        for (item in createMediaItemsFromIntent(intent)) {
            mediaItems.add(
                maybeSetDownloadProperties(
                    item,
                    downloadTracker.getDownloadRequest(item.localConfiguration!!.uri)
                )
            )
        }
        return mediaItems
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun maybeSetDownloadProperties(
        item: MediaItem,
        downloadRequest: DownloadRequest?
    ): MediaItem {
        if (downloadRequest == null) {
            return item
        }
        val builder = item.buildUpon()
        builder.setMediaId(downloadRequest.id)
            .setUri(downloadRequest.uri)
            .setCustomCacheKey(downloadRequest.customCacheKey)
            .setMimeType(downloadRequest.mimeType)
            .setStreamKeys(downloadRequest.streamKeys)
        val drmConfiguration = item.localConfiguration!!.drmConfiguration
        if (drmConfiguration != null) {
            builder.setDrmConfiguration(
                drmConfiguration.buildUpon().setKeySetId(downloadRequest.keySetId).build()
            )
        }
        return builder.build()
    }

    //---

    private fun updateButtonVisibility() {
        selectTracksButton?.isEnabled = false
    }

    private fun showControls() {
        debugRootView?.visibility = View.VISIBLE
    }

    //---

    private inner class PlayerEventListener : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: @Player.State Int) {
            if (playbackState == Player.STATE_ENDED) {
                showControls()
            }
            updateButtonVisibility()
        }

        override fun onPlayerError(error: PlaybackException) {
            if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                player?.seekToDefaultPosition()
                player?.prepare()
            } else {
                updateButtonVisibility()
                showControls()
            }
        }

        override fun onTracksChanged(tracks: Tracks) {
            updateButtonVisibility()
            if (tracks === lastSeenTracks) {
                return
            }
            if (tracks.containsType(C.TRACK_TYPE_VIDEO)
                && !tracks.isTypeSupported(C.TRACK_TYPE_VIDEO,  /* allowExceedsCapabilities= */true)
            ) {
                toastQQ("Media includes video tracks, but none are playable by this device")
            }
            if (tracks.containsType(C.TRACK_TYPE_AUDIO)
                && !tracks.isTypeSupported(C.TRACK_TYPE_AUDIO,  /* allowExceedsCapabilities= */true)
            ) {
                toastQQ("Media includes audio tracks, but none are playable by this device")
            }
            lastSeenTracks = tracks
        }
    }

    private class PlayerErrorMessageProvider : ErrorMessageProvider<PlaybackException> {
        override fun getErrorMessage(e: PlaybackException): android.util.Pair<Int, String> {
            val errorString = "Playback failed"
            return android.util.Pair.create(0, errorString)
        }
    }

    //region ---callback---

    override fun onVisibilityChanged(visibility: Int) {
        debugRootView?.visibility = visibility;
    }

    //endregion ---callback---

}