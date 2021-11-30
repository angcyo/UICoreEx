package com.angcyo.tim.ui.view

import android.content.Context
import android.text.Editable
import android.text.Spannable
import android.text.TextUtils
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import com.angcyo.library.ex._color
import com.angcyo.tim.R
import com.angcyo.tim.util.handlerEmojiText
import com.angcyo.widget.base.restoreSelection
import com.angcyo.widget.edit.DslEditText
import java.util.*
import java.util.regex.Pattern

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/11
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
open class ChatEditText : DslEditText {

    companion object {
        const val TIM_MENTION_TAG = "@"
        const val TIM_MENTION_TAG_FULL = "＠"

        val TIM_MENTION_PATTERN = Pattern.compile("@[^\\s]+\\s")
        val TIM_MENTION_PATTERN_FULL = Pattern.compile("＠[^\\s]+\\s")
    }

    private var mentionTextColor = 0

    private val patternMap = hashMapOf<String, Pattern>()

    private var isRangeSelected = false

    private var lastSelectedRange: Range? = null

    private val rangeArrayList: MutableList<Range>? = mutableListOf<Range>()

    /**输入@监听*/
    var onMentionInputListener: OnMentionInputListener? = null

    constructor(context: Context) : super(context) {
        initAttribute(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initAttribute(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initAttribute(context, attrs)
    }

    private fun initAttribute(context: Context, attrs: AttributeSet?) {
        mentionTextColor = _color(R.color.colorAccent, context)
        patternMap.clear()
        patternMap[TIM_MENTION_TAG] = TIM_MENTION_PATTERN
        patternMap[TIM_MENTION_TAG_FULL] = TIM_MENTION_PATTERN_FULL
        //setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        addTextChangedListener(MentionTextWatcher())
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo?): InputConnection? {
        return HackInputConnection(super.onCreateInputConnection(outAttrs), true, this)
    }

    var _lastInputText: CharSequence? = null

    override fun onTextChanged(
        text: CharSequence?,
        start: Int,
        lengthBefore: Int,
        lengthAfter: Int
    ) {
        val textStr = text?.toString()
        val lastStr = _lastInputText?.toString()
        if (lastStr != textStr) {
            val sStart = selectionStart
            val sEnd = selectionEnd
            _lastInputText = textStr?.handlerEmojiText()
            setText(_lastInputText)
            restoreSelection(sStart, sEnd)
        } else {
            colorMentionString()
        }
    }

    /**获取@的人
     * [excludeMentionCharacter] 剔除@字符*/
    open fun getMentionList(excludeMentionCharacter: Boolean): List<String>? {
        val mentionList: MutableList<String> = ArrayList()
        if (TextUtils.isEmpty(text.toString())) {
            return mentionList
        }
        for ((_, value) in patternMap.entries) {
            val matcher = value.matcher(text.toString())
            while (matcher.find()) {
                var mentionText = matcher.group()
                if (excludeMentionCharacter) {
                    mentionText = mentionText.substring(1, mentionText.length - 1)
                }
                if (!mentionList.contains(mentionText)) {
                    mentionList.add(mentionText)
                }
            }
        }
        return mentionList
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        if (lastSelectedRange?.isEqual(selStart, selEnd) == true) {
            return
        }
        val closestRange: Range? = getRangeOfClosestMentionString(selStart, selEnd)
        if (closestRange != null && closestRange.to == selEnd) {
            isRangeSelected = false
        }
        val nearbyRange = getRangeOfNearbyMentionString(selStart, selEnd) ?: return

        //forbid cursor located in the mention string.
        if (selStart == selEnd) {
            setSelection(nearbyRange.getAnchorPosition(selStart))
        } else {
            if (selEnd < nearbyRange.to) {
                setSelection(selStart, nearbyRange.to)
            }
            if (selStart > nearbyRange.from) {
                setSelection(nearbyRange.from, selEnd)
            }
        }
    }

    /**给@xxx 添加颜色*/
    private fun colorMentionString() {
        isRangeSelected = false
        rangeArrayList?.clear()

        val spannableText = text
        if (spannableText == null || TextUtils.isEmpty(spannableText.toString())) {
            return
        }

        //clear
        val oldSpans = spannableText.getSpans(
            0,
            spannableText.length,
            ForegroundColorSpan::class.java
        )
        for (oldSpan in oldSpans) {
            spannableText.removeSpan(oldSpan)
        }
        val text = spannableText.toString()
        var lastMentionIndex = -1
        for ((_, value) in patternMap) {
            val matcher = value.matcher(text)
            while (matcher.find()) {
                val mentionText = matcher.group()
                val start: Int = if (lastMentionIndex != -1) {
                    text.indexOf(mentionText, lastMentionIndex)
                } else {
                    text.indexOf(mentionText)
                }
                val end = start + mentionText.length
                spannableText.setSpan(
                    ForegroundColorSpan(mentionTextColor),
                    start,
                    end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                lastMentionIndex = end
                rangeArrayList?.add(Range(start, end))
            }
        }
    }

    private fun getRangeOfClosestMentionString(selStart: Int, selEnd: Int): Range? {
        if (rangeArrayList != null) {
            for (range in rangeArrayList) {
                if (range.contains(selStart, selEnd)) {
                    return range
                }
            }
        }
        return null
    }

    private fun getRangeOfNearbyMentionString(selStart: Int, selEnd: Int): Range? {
        if (rangeArrayList != null) {
            for (range in rangeArrayList) {
                if (range.isWrappedBy(selStart, selEnd)) {
                    return range
                }
            }
        }
        return null
    }

    private inner class MentionTextWatcher : TextWatcher {

        override fun beforeTextChanged(
            sequence: CharSequence?,
            start: Int,
            count: Int,
            after: Int
        ) {
        }

        override fun onTextChanged(text: CharSequence, start: Int, lengthBefore: Int, count: Int) {
            if (count == 1 && !TextUtils.isEmpty(text) && hasFocus()) {
                val mentionChar = text.toString()[start]
                for ((key) in patternMap.entries) {
                    if (key == mentionChar.toString()) {
                        onMentionInputListener?.onMentionCharacterInput(key)
                    }
                }
            }
        }

        override fun afterTextChanged(editable: Editable) {}
    }

    private inner class HackInputConnection(
        target: InputConnection?,
        mutable: Boolean,
        val editText: ChatEditText
    ) : InputConnectionWrapper(target, mutable) {

        override fun sendKeyEvent(event: KeyEvent): Boolean {
            if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_DEL) {
                val selectionStart = editText.selectionStart
                val selectionEnd = editText.selectionEnd
                val closestRange: Range? =
                    getRangeOfClosestMentionString(selectionStart, selectionEnd)
                if (closestRange == null) {
                    isRangeSelected = false
                    return super.sendKeyEvent(event)
                }
                if (isRangeSelected || selectionStart == closestRange.from) {
                    isRangeSelected = false
                    return super.sendKeyEvent(event)
                } else {
                    isRangeSelected = true
                    lastSelectedRange = closestRange
                    setSelection(closestRange.to, closestRange.from)
                    sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                }
                return true
            }
            return super.sendKeyEvent(event)
        }

        override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
            return if (beforeLength == 1 && afterLength == 0) {
                (sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                        && sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL)))
            } else super.deleteSurroundingText(beforeLength, afterLength)
        }
    }

    private class Range(var from: Int, var to: Int) {

        fun isWrappedBy(start: Int, end: Int): Boolean {
            return start in (from + 1) until to || end in (from + 1) until to
        }

        fun contains(start: Int, end: Int): Boolean {
            return from <= start && to >= end
        }

        fun isEqual(start: Int, end: Int): Boolean {
            return from == start && to == end || from == end && to == start
        }

        fun getAnchorPosition(value: Int): Int {
            return if (value - from - (to - value) >= 0) {
                to
            } else {
                from
            }
        }
    }

    interface OnMentionInputListener {
        fun onMentionCharacterInput(tag: String)
    }

}