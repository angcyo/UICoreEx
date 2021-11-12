package com.angcyo.tim.chat

import android.view.View
import android.widget.EditText
import com.angcyo.tim.R
import com.angcyo.tim.dslitem.ChatEmojiItem
import com.angcyo.tim.dslitem.ChatMoreActionItem
import com.angcyo.tim.ui.chat.BaseChatFragment
import com.angcyo.tim.util.FaceManager
import com.angcyo.widget.base.*
import com.angcyo.widget.layout.DslSoftInputLayout
import com.angcyo.widget.recycler.DslRecyclerView
import com.angcyo.widget.recycler.initDslAdapter

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/11
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
abstract class BaseChatPresenter {

    var chatFragment: BaseChatFragment? = null

    /**是否是语音输入*/
    var isVoiceInput: Boolean = false

    /**初始化界面*/
    open fun initView(fragment: BaseChatFragment) {
        chatFragment = fragment

        val softInputLayout = fragment._vh.v<DslSoftInputLayout>(R.id.lib_soft_input_layout)

        //语音
        fragment._vh.click(R.id.chat_voice_view) {
            softInputLayout?.hideEmojiLayout()
            _switchVoiceInput(!isVoiceInput)
        }
        //表情
        fragment._vh.click(R.id.chat_emoji_view) {
            softInputLayout?.showEmojiLayout()
            _showEmojiLayout()
            if (isVoiceInput) {
                _switchVoiceInput(false, false)
            }
        }
        //更多
        fragment._vh.click(R.id.chat_more_view) {
            softInputLayout?.showEmojiLayout()
            _showMoreLayout()
            if (isVoiceInput) {
                _switchVoiceInput(false, false)
            }
        }
    }

    val moreActionList: MutableList<MoreActionBean> = mutableListOf()

    /**更多菜单item*/
    open fun initMoreAction() {
        moreActionList.clear()
        moreActionList.add(MoreActionBean().apply {
            title = "相册"
            iconResId = R.drawable.ic_image_type_item
        })
        moreActionList.add(MoreActionBean().apply {
            title = "拍摄"
            iconResId = R.drawable.ic_camera_type_item
        })
        moreActionList.add(MoreActionBean().apply {
            title = "视频通话"
            iconResId = R.drawable.ic_video_type_item
        })
        moreActionList.add(MoreActionBean().apply {
            title = "位置"
            iconResId = R.drawable.ic_gps_type_item
        })
        moreActionList.add(MoreActionBean().apply {
            title = "文件"
            iconResId = R.drawable.ic_file_type_item
        })
    }

    /**语音输入布局控制*/
    fun _switchVoiceInput(voiceInput: Boolean, showSoftInput: Boolean = true) {
        chatFragment?._vh?.apply {
            val editText = v<EditText>(R.id.chat_edit_text)

            if (voiceInput) {
                //切换到语音输入
                img(R.id.chat_voice_view)?.setImageResource(R.drawable.ic_chat_keyboard)
                visible(R.id.chat_voice_input)
                gone(R.id.chat_edit_text)

                editText?.hideSoftInput()
            } else {
                //切换到文本输入
                img(R.id.chat_voice_view)?.setImageResource(R.drawable.ic_chat_voice)
                gone(R.id.chat_voice_input)
                visible(R.id.chat_edit_text)

                if (showSoftInput) {
                    editText?.setSelectionLast()
                    editText?.showSoftInput()
                }
            }
            isVoiceInput = voiceInput
        }
    }

    /**关闭表情布局*/
    fun onCloseEmojiLayout() {

    }

    var emojiLayout: View? = null

    /**显示emoji布局*/
    fun _showEmojiLayout() {
        if (emojiLayout?.parent != null) {
            return
        }
        chatFragment?._vh?.group(R.id.lib_emoji_layout)?.apply {
            removeView(moreLayout)
            if (emojiLayout == null) {
                replace(R.layout.lib_chat_face_layout).apply {
                    emojiLayout = this
                    moreLayout = null

                    find<DslRecyclerView>(R.id.lib_recycler_view)?.initDslAdapter {
                        FaceManager.emojiList.forEach {
                            ChatEmojiItem()() {
                                emoji = it
                            }
                        }
                    }
                }
            } else {
                addView(emojiLayout)
            }
        }
    }

    var moreLayout: View? = null

    /**显示更多布局*/
    fun _showMoreLayout() {
        if (moreLayout?.parent != null) {
            return
        }
        chatFragment?._vh?.group(R.id.lib_emoji_layout)?.apply {
            removeView(emojiLayout)
            if (moreLayout == null) {
                replace(R.layout.lib_chat_more_layout).apply {
                    moreLayout = this
                    emojiLayout = null

                    find<DslRecyclerView>(R.id.lib_recycler_view)?.initDslAdapter {
                        moreActionList.forEach {
                            ChatMoreActionItem()() {
                                moreActionBean = it
                            }
                        }
                    }
                }
            } else {
                addView(moreLayout)
            }
        }
    }
}