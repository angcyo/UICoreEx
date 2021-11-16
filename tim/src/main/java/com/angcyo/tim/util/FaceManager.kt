package com.angcyo.tim.util

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.util.DisplayMetrics
import android.util.LruCache
import android.widget.EditText
import android.widget.TextView
import com.angcyo.library.app
import com.angcyo.library.ex._dimen
import com.angcyo.tim.R
import com.angcyo.tim.bean.Emoji
import java.io.IOException
import java.io.InputStream
import java.util.*
import java.util.regex.Pattern

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

    /**处理Emoji表情
     * [typing] 是否是正在编辑*/
    fun handlerEmojiText(
        view: TextView?,
        content: String,
        typing: Boolean
    ): SpannableStringBuilder {
        val sb = SpannableStringBuilder(content)
        val regex = "\\[(\\S+?)\\]"
        val p = Pattern.compile(regex)
        val m = p.matcher(content)
        var imageFound = false
        while (m.find()) {
            val emojiName = m.group()
            val bitmap = drawableCache[emojiName]
            if (bitmap != null) {
                imageFound = true
                sb.setSpan(
                    ImageSpan(view?.context ?: app(), bitmap),
                    m.start(),
                    m.end(),
                    Spannable.SPAN_INCLUSIVE_EXCLUSIVE
                )
            }
        }
        if (view == null) {
            return sb
        }
        // 如果没有发现表情图片，并且当前是输入状态，不再重设输入框
        if (!imageFound && typing) {
            return sb
        }
        val selection = view.selectionStart
        view.text = sb
        if (view is EditText) {
            view.setSelection(selection)
        }
        return sb
    }

    fun isFaceChar(faceChar: String): Boolean {
        return drawableCache[faceChar] != null
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

/**处理emoji*/
fun String.handlerEmojiText() = FaceManager.handlerEmojiText(null, this, false)