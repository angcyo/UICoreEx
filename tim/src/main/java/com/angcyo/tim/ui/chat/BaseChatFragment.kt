package com.angcyo.tim.ui.chat

import android.os.Bundle
import com.angcyo.base.removeThis
import com.angcyo.core.fragment.BaseDslFragment
import com.angcyo.getDataSerializable
import com.angcyo.library.toastQQ
import com.angcyo.tim.R
import com.angcyo.tim.bean.ChatInfoBean
import com.angcyo.tim.chat.BaseChatPresenter

/**
 * TIM 基础聊天界面
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/11
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
abstract class BaseChatFragment : BaseDslFragment() {

    init {
        fragmentTitle = ""
        enableRefresh = true
        enableAdapterRefresh = false
        enableContentBounds = false
        page.singlePage()

        enableSoftInput

        fragmentLayoutId = R.layout.lib_chat_fragment
    }

    /**数据结构*/
    var chatInfoBean: ChatInfoBean? = null

    /**界面操作*/
    var chatPresenter: BaseChatPresenter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chatInfoBean = getDataSerializable()
    }

    override fun initBaseView(savedInstanceState: Bundle?) {
        super.initBaseView(savedInstanceState)

        onInitChat()
    }

    override fun onFragmentShow(bundle: Bundle?) {
        super.onFragmentShow(bundle)
    }

    open fun onInitChat() {
        val chatInfo = chatInfoBean
        if (chatInfo == null) {
            toastQQ("数据异常")
            removeThis()
        } else {
            fragmentTitle = chatInfo.chatTitle

            chatPresenter?.initView(this)
            chatPresenter?.initMoreAction()

            chatPresenter?.loadMessage()
            chatPresenter?.listenerMessage()
        }
    }

    override fun onBackPressed(): Boolean {
        return super.onBackPressed()
    }
}