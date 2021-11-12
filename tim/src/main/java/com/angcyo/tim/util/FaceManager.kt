package com.angcyo.tim.util

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.util.DisplayMetrics
import android.util.LruCache
import com.angcyo.library.app
import com.angcyo.library.ex._dimen
import com.angcyo.tim.R
import com.angcyo.tim.bean.Emoji
import java.io.IOException
import java.io.InputStream
import java.util.*

/**
 * Emoji 表情管理
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/11
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
object FaceManager {

    private var emojiFilters: Array<String> = emptyArray()
    private val drawableCache: LruCache<String, Bitmap> = LruCache<String, Bitmap>(1024)
    val emojiList = ArrayList<Emoji>()

    var defaultEmojiSize: Int = 32

    /**初始化*/
    fun init() {
        emojiFilters = app().resources.getStringArray(R.array.emoji_filter_key)
        defaultEmojiSize = _dimen(R.dimen.chat_emoji_icon_size)
        loadFaceFiles()
    }

    private fun loadAssetBitmap(filter: String, assetPath: String, isEmoji: Boolean): Emoji? {
        var `is`: InputStream? = null
        try {
            val emoji = Emoji()
            val resources: Resources = app().resources
            val options = BitmapFactory.Options()
            options.inDensity = DisplayMetrics.DENSITY_XXHIGH
            options.inScreenDensity = resources.displayMetrics.densityDpi
            options.inTargetDensity = resources.displayMetrics.densityDpi
            app().assets.list("")
            `is` = app().assets.open(assetPath)
            val bitmap = BitmapFactory.decodeStream(
                `is`,
                Rect(0, 0, defaultEmojiSize, defaultEmojiSize),
                options
            )
            if (bitmap != null) {
                drawableCache.put(filter, bitmap)
                emoji.icon = bitmap
                emoji.filter = filter
                emoji.width = defaultEmojiSize
                emoji.height = defaultEmojiSize
                if (isEmoji) {
                    emojiList.add(emoji)
                }
            }
            return emoji
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (`is` != null) {
                try {
                    `is`.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        return null
    }


    private fun loadFaceFiles() {
        for (i in emojiFilters.indices) {
            loadAssetBitmap(emojiFilters[i], "emoji/" + emojiFilters[i] + "@2x.png", true)
        }
    }

}