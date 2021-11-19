package com.angcyo.tim.chat

import android.widget.EditText
import com.angcyo.dsladapter.DslAdapter
import com.angcyo.tim.R
import com.angcyo.tim.bean.ChatInfoBean
import com.angcyo.tim.ui.chat.BaseChatFragment
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.layout.DslSoftInputLayout
import com.angcyo.widget.recycler.DslRecyclerView

/**
 * 聊天操作层
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/11
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
abstract class BaseChatControl {

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

    val chatBean: ChatInfoBean?
        get() = chatFragment?.chatInfoBean

    //<editor-fold desc="初始化">

    open fun initControl(fragment: BaseChatFragment) {
        chatFragment = fragment
    }

    //</editor-fold desc="初始化">
}