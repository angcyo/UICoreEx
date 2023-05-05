package com.angcyo.exoplayer

import android.content.Intent
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.Util
import com.google.common.base.Preconditions.checkArgument
import com.google.common.base.Preconditions.checkState
import com.google.common.collect.ImmutableList


/**
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2023/05/05
 */
object IntentUtil {

    // Actions.

    // Actions.
    val ACTION_VIEW = "androidx.media3.angcyo.action.VIEW"
    val ACTION_VIEW_LIST = "androidx.media3.angcyo.action.VIEW_LIST"

    // Activity extras.
    val PREFER_EXTENSION_DECODERS_EXTRA = "prefer_extension_decoders"

    // Media item configuration extras.

    // Media item configuration extras.
    val URI_EXTRA = "uri"
    val TITLE_EXTRA = "title"
    val MIME_TYPE_EXTRA = "mime_type"
    val CLIP_START_POSITION_MS_EXTRA = "clip_start_position_ms"
    val CLIP_END_POSITION_MS_EXTRA = "clip_end_position_ms"

    val AD_TAG_URI_EXTRA = "ad_tag_uri"

    val DRM_SCHEME_EXTRA = "drm_scheme"
    val DRM_LICENSE_URI_EXTRA = "drm_license_uri"
    val DRM_KEY_REQUEST_PROPERTIES_EXTRA = "drm_key_request_properties"
    val DRM_SESSION_FOR_CLEAR_CONTENT = "drm_session_for_clear_content"
    val DRM_MULTI_SESSION_EXTRA = "drm_multi_session"
    val DRM_FORCE_DEFAULT_LICENSE_URI_EXTRA = "drm_force_default_license_uri"

    val SUBTITLE_URI_EXTRA = "subtitle_uri"
    val SUBTITLE_MIME_TYPE_EXTRA = "subtitle_mime_type"
    val SUBTITLE_LANGUAGE_EXTRA = "subtitle_language"

    /** Creates a list of [media items][MediaItem] from an [Intent].  */
    fun createMediaItemsFromIntent(intent: Intent): List<MediaItem> {
        val mediaItems = mutableListOf<MediaItem>()
        if (ACTION_VIEW_LIST == intent.action) {
            var index = 0
            while (intent.hasExtra(URI_EXTRA + "_" + index)) {
                val uri = Uri.parse(intent.getStringExtra(URI_EXTRA + "_" + index))
                mediaItems.add(
                    createMediaItemFromIntent(
                        uri,
                        intent,  /* extrasKeySuffix= */
                        "_$index"
                    )
                )
                index++
            }
        } else {
            val uri = intent.data
            mediaItems.add(createMediaItemFromIntent(uri, intent,  /* extrasKeySuffix= */""))
        }
        return mediaItems
    }

    /** Populates the intent with the given list of [media items][MediaItem].  */
    fun addToIntent(mediaItems: List<MediaItem>, intent: Intent) {
        checkArgument(mediaItems.isNotEmpty())
        if (mediaItems.size == 1) {
            val mediaItem: MediaItem = mediaItems[0]
            val localConfiguration: MediaItem.LocalConfiguration =
                checkNotNull(mediaItem.localConfiguration)
            intent.setAction(ACTION_VIEW).data = mediaItem.localConfiguration?.uri
            if (mediaItem.mediaMetadata.title != null) {
                intent.putExtra(TITLE_EXTRA, mediaItem.mediaMetadata.title)
            }
            addPlaybackPropertiesToIntent(localConfiguration, intent,  /* extrasKeySuffix= */"")
            addClippingConfigurationToIntent(
                mediaItem.clippingConfiguration, intent,  /* extrasKeySuffix= */""
            )
        } else {
            intent.action = ACTION_VIEW_LIST
            for (i in mediaItems.indices) {
                val mediaItem: MediaItem = mediaItems[i]
                val localConfiguration: MediaItem.LocalConfiguration =
                    checkNotNull(mediaItem.localConfiguration)
                intent.putExtra(URI_EXTRA + "_$i", localConfiguration.uri.toString())
                addPlaybackPropertiesToIntent(
                    localConfiguration,
                    intent,  /* extrasKeySuffix= */
                    "_$i"
                )
                addClippingConfigurationToIntent(
                    mediaItem.clippingConfiguration, intent,  /* extrasKeySuffix= */"_$i"
                )
                if (mediaItem.mediaMetadata.title != null) {
                    intent.putExtra(TITLE_EXTRA + "_$i", mediaItem.mediaMetadata.title)
                }
            }
        }
    }

    private fun createMediaItemFromIntent(
        uri: Uri?,
        intent: Intent,
        extrasKeySuffix: String
    ): MediaItem {
        val mimeType = intent.getStringExtra(MIME_TYPE_EXTRA + extrasKeySuffix)
        val title = intent.getStringExtra(TITLE_EXTRA + extrasKeySuffix)
        val adTagUri = intent.getStringExtra(AD_TAG_URI_EXTRA + extrasKeySuffix)
        val subtitleConfiguration: MediaItem.SubtitleConfiguration? =
            createSubtitleConfiguration(intent, extrasKeySuffix)
        val builder = MediaItem.Builder()
            .setUri(uri)
            .setMimeType(mimeType)
            .setMediaMetadata(MediaMetadata.Builder().setTitle(title).build())
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(
                        intent.getLongExtra(
                            CLIP_START_POSITION_MS_EXTRA + extrasKeySuffix,
                            0
                        )
                    )
                    .setEndPositionMs(
                        intent.getLongExtra(
                            CLIP_END_POSITION_MS_EXTRA + extrasKeySuffix,
                            C.TIME_END_OF_SOURCE
                        )
                    )
                    .build()
            )
        if (adTagUri != null) {
            builder.setAdsConfiguration(
                MediaItem.AdsConfiguration.Builder(Uri.parse(adTagUri)).build()
            )
        }
        if (subtitleConfiguration != null) {
            builder.setSubtitleConfigurations(ImmutableList.of(subtitleConfiguration))
        }
        return populateDrmPropertiesFromIntent(builder, intent, extrasKeySuffix).build()
    }


    private fun createSubtitleConfiguration(
        intent: Intent, extrasKeySuffix: String
    ): MediaItem.SubtitleConfiguration? {
        return if (!intent.hasExtra(SUBTITLE_URI_EXTRA + extrasKeySuffix)) {
            null
        } else MediaItem.SubtitleConfiguration.Builder(
            Uri.parse(intent.getStringExtra(SUBTITLE_URI_EXTRA + extrasKeySuffix))
        ).setMimeType(
            checkNotNull(intent.getStringExtra(SUBTITLE_MIME_TYPE_EXTRA + extrasKeySuffix))
        ).setLanguage(intent.getStringExtra(SUBTITLE_LANGUAGE_EXTRA + extrasKeySuffix))
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .build()
    }

    private fun populateDrmPropertiesFromIntent(
        builder: MediaItem.Builder, intent: Intent, extrasKeySuffix: String
    ): MediaItem.Builder {
        val schemeKey = DRM_SCHEME_EXTRA + extrasKeySuffix
        val drmSchemeExtra = intent.getStringExtra(schemeKey) ?: return builder
        val headers: MutableMap<String, String> = HashMap()
        val keyRequestPropertiesArray =
            intent.getStringArrayExtra(DRM_KEY_REQUEST_PROPERTIES_EXTRA + extrasKeySuffix)
        if (keyRequestPropertiesArray != null) {
            var i = 0
            while (i < keyRequestPropertiesArray.size) {
                headers[keyRequestPropertiesArray[i]] = keyRequestPropertiesArray[i + 1]
                i += 2
            }
        }
        val drmUuid = Util.getDrmUuid(drmSchemeExtra)
        if (drmUuid != null) {
            builder.setDrmConfiguration(
                MediaItem.DrmConfiguration.Builder(drmUuid)
                    .setLicenseUri(intent.getStringExtra(DRM_LICENSE_URI_EXTRA + extrasKeySuffix))
                    .setMultiSession(
                        intent.getBooleanExtra(
                            DRM_MULTI_SESSION_EXTRA + extrasKeySuffix,
                            false
                        )
                    )
                    .setForceDefaultLicenseUri(
                        intent.getBooleanExtra(
                            DRM_FORCE_DEFAULT_LICENSE_URI_EXTRA + extrasKeySuffix,
                            false
                        )
                    )
                    .setLicenseRequestHeaders(headers)
                    .setForceSessionsForAudioAndVideoTracks(
                        intent.getBooleanExtra(
                            DRM_SESSION_FOR_CLEAR_CONTENT + extrasKeySuffix,
                            false
                        )
                    )
                    .build()
            )
        }
        return builder
    }

    private fun addPlaybackPropertiesToIntent(
        localConfiguration: MediaItem.LocalConfiguration,
        intent: Intent,
        extrasKeySuffix: String
    ) {
        intent
            .putExtra(MIME_TYPE_EXTRA + extrasKeySuffix, localConfiguration.mimeType)
            .putExtra(
                AD_TAG_URI_EXTRA + extrasKeySuffix,
                if (localConfiguration.adsConfiguration != null) localConfiguration.adsConfiguration!!.adTagUri.toString() else null
            )
        if (localConfiguration.drmConfiguration != null) {
            addDrmConfigurationToIntent(
                localConfiguration.drmConfiguration!!,
                intent,
                extrasKeySuffix
            )
        }
        if (!localConfiguration.subtitleConfigurations.isEmpty()) {
            checkState(localConfiguration.subtitleConfigurations.size == 1)
            val subtitleConfiguration: MediaItem.SubtitleConfiguration =
                localConfiguration.subtitleConfigurations.get(0)
            intent.putExtra(
                SUBTITLE_URI_EXTRA + extrasKeySuffix,
                subtitleConfiguration.uri.toString()
            )
            intent.putExtra(
                SUBTITLE_MIME_TYPE_EXTRA + extrasKeySuffix,
                subtitleConfiguration.mimeType
            )
            intent.putExtra(
                SUBTITLE_LANGUAGE_EXTRA + extrasKeySuffix,
                subtitleConfiguration.language
            )
        }
    }

    private fun addDrmConfigurationToIntent(
        drmConfiguration: MediaItem.DrmConfiguration, intent: Intent, extrasKeySuffix: String
    ) {
        intent.putExtra(DRM_SCHEME_EXTRA + extrasKeySuffix, drmConfiguration.scheme.toString())
        intent.putExtra(
            DRM_LICENSE_URI_EXTRA + extrasKeySuffix,
            if (drmConfiguration.licenseUri != null) drmConfiguration.licenseUri.toString() else null
        )
        intent.putExtra(DRM_MULTI_SESSION_EXTRA + extrasKeySuffix, drmConfiguration.multiSession)
        intent.putExtra(
            DRM_FORCE_DEFAULT_LICENSE_URI_EXTRA + extrasKeySuffix,
            drmConfiguration.forceDefaultLicenseUri
        )
        val drmKeyRequestProperties =
            arrayOfNulls<String>(drmConfiguration.licenseRequestHeaders.size * 2)
        var index = 0
        for ((key, value) in drmConfiguration.licenseRequestHeaders.entries) {
            drmKeyRequestProperties[index++] = key
            drmKeyRequestProperties[index++] = value
        }
        intent.putExtra(DRM_KEY_REQUEST_PROPERTIES_EXTRA + extrasKeySuffix, drmKeyRequestProperties)
        val forcedDrmSessionTrackTypes: List<Int> = drmConfiguration.forcedSessionTrackTypes
        if (forcedDrmSessionTrackTypes.isNotEmpty()) {
            // Only video and audio together are supported.
            checkState(
                forcedDrmSessionTrackTypes.size == 2 && forcedDrmSessionTrackTypes.contains(C.TRACK_TYPE_VIDEO)
                        && forcedDrmSessionTrackTypes.contains(C.TRACK_TYPE_AUDIO)
            )
            intent.putExtra(DRM_SESSION_FOR_CLEAR_CONTENT + extrasKeySuffix, true)
        }
    }

    private fun addClippingConfigurationToIntent(
        clippingConfiguration: MediaItem.ClippingConfiguration,
        intent: Intent,
        extrasKeySuffix: String
    ) {
        if (clippingConfiguration.startPositionMs != 0L) {
            intent.putExtra(
                CLIP_START_POSITION_MS_EXTRA + extrasKeySuffix,
                clippingConfiguration.startPositionMs
            )
        }
        if (clippingConfiguration.endPositionMs != C.TIME_END_OF_SOURCE) {
            intent.putExtra(
                CLIP_END_POSITION_MS_EXTRA + extrasKeySuffix, clippingConfiguration.endPositionMs
            )
        }
    }
}