package com.angcyo.tim.chat

import android.view.View
import android.widget.EditText
import com.angcyo.dsladapter.DslAdapter
import com.angcyo.library.L
import com.angcyo.library.ex.str
import com.angcyo.library.ex.string
import com.angcyo.tim.R
import com.angcyo.tim.TimMessage
import com.angcyo.tim.bean.MessageInfoBean
import com.angcyo.tim.dslitem.ChatEmojiItem
import com.angcyo.tim.dslitem.ChatLoadingItem
import com.angcyo.tim.dslitem.ChatMoreActionItem
import com.angcyo.tim.dslitem.MsgTextItem
import com.angcyo.tim.ui.chat.BaseChatFragment
import com.angcyo.tim.util.FaceManager
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.base.*
import com.angcyo.widget.layout.DslSoftInputLayout
import com.angcyo.widget.recycler.DslRecyclerView
import com.angcyo.widget.recycler.initDslAdapter

/**
 * 聊天操作层
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/11
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
abstract class BaseChatPresenter {

    var chatFragment: BaseChatFragment? = null

    /**是否是语音输入*/
    var isVoiceInput: Boolean = false

    val _adapter: DslAdapter?
        get() = chatFragment?._adapter

    val _recycler: DslRecyclerView?
        get() = chatFragment?._recycler

    val _vh: DslViewHolder?
        get() = chatFragment?._vh

    val softInputLayout: DslSoftInputLayout?
        get() = _vh?.v<DslSoftInputLayout>(R.id.lib_soft_input_layout)

    val inputEditText: EditText?
        get() = _vh?.v<EditText>(R.id.chat_edit_text)

    //<editor-fold desc="初始化">

    /**初始化界面*/
    open fun initView(fragment: BaseChatFragment) {
        chatFragment = fragment

        //输入框
        inputEditText?.onTextChange {
            if (it.isEmpty()) {
                _vh?.gone(R.id.chat_send_button)
            } else {
                _vh?.visible(R.id.chat_send_button)
            }
        }

        //发送按钮
        _vh?.click(R.id.chat_send_button) {
            sendInputMessage(inputEditText?.string())
            inputEditText?.setInputText()
        }

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

    //</editor-fold desc="初始化">

    //<editor-fold desc="操作">

    /**加载消息*/
    open fun loadMessage() {
        val lastMessageInfo: MessageInfoBean? = chatFragment?.chatInfoBean?.lastMessageInfoBean

        _adapter?.changeDataItems {
            it.add(0, ChatLoadingItem())
        }

        chatFragment?.let {
            it.chatInfoBean?.let { chatBean ->
                TimMessage.getC2CHistoryMessageList(
                    chatBean.chatId,
                    lastMessageInfo?.message
                ) { list, timSdkException ->
                    L.i()
                }
            }
        }
    }

    /**发送输入框的消息*/
    open fun sendInputMessage(text: CharSequence?) {
        if (text.isNullOrEmpty()) {
            return
        }

        chatFragment?.chatInfoBean?.let { chatBean ->
            _adapter?.changeDataItems {
                it.add(MsgTextItem().apply {
                    messageInfoBean = TimMessage.textMessageBean(text.str())
                })
            }
        }
    }

    //</editor-fold desc="操作">

    //<editor-fold desc="内部操作">

    /**语音输入布局控制*/
    fun _switchVoiceInput(voiceInput: Boolean, showSoftInput: Boolean = true) {
        chatFragment?._vh?.apply {
            val editText = inputEditText

            if (voiceInput) {
                //切换到语音输入
                img(R.id.chat_voice_view)?.setImageResource(R.drawable.ic_chat_keyboard)
                visible(R.id.chat_voice_input)
                gone(R.id.chat_edit_text)
                gone(R.id.chat_send_button) //发送按钮

                editText?.hideSoftInput()
            } else {
                //切换到文本输入
                img(R.id.chat_voice_view)?.setImageResource(R.drawable.ic_chat_voice)
                gone(R.id.chat_voice_input)
                visible(R.id.chat_edit_text)
                visible(R.id.chat_send_button, inputEditText.string().isNotEmpty()) //发送按钮

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

    //</editor-fold desc="内部操作">
}