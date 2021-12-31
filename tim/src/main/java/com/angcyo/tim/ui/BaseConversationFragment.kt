package com.angcyo.tim.ui

import android.os.Bundle
import android.view.View
import com.angcyo.core.fragment.BaseDslFragment
import com.angcyo.core.vmApp
import com.angcyo.dialog.popup.actionPopupWindow
import com.angcyo.dsladapter.removeIt
import com.angcyo.dsladapter.toLoading
import com.angcyo.item.DslMessageListItem
import com.angcyo.item.style.itemLoadImageTextBgColor
import com.angcyo.library.ex.getSafe
import com.angcyo.library.ex.randomColorList
import com.angcyo.library.ex.toColor
import com.angcyo.tim.bean.*
import com.angcyo.tim.dslitem.ChatConnectTipItem
import com.angcyo.tim.dslitem.ConversationItem
import com.angcyo.tim.helper.ConversationHelper
import com.angcyo.tim.model.ChatModel
import com.angcyo.tim.model.ConversationModel
import com.angcyo.tim.model.MessageNotifyModel

/**
 * IM会话列表界面, IM中聊过天的都会在这里显示
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/11
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
open class BaseConversationFragment : BaseDslFragment() {

    val conversationModel = vmApp<ConversationModel>()

    /**SDK未连接时的提示*/
    val chatConnectTipItem = ChatConnectTipItem()

    init {
        fragmentTitle = "会话列表"
        enableRefresh = false
        enableAdapterRefresh = false
        page.singlePage()
    }

    override fun initBaseView(savedInstanceState: Bundle?) {
        super.initBaseView(savedInstanceState)

        onInitConversation()
    }

    override fun onFragmentShow(bundle: Bundle?) {
        super.onFragmentShow(bundle)
        vmApp<MessageNotifyModel>().closeMessageNotify = true
    }

    override fun onFragmentHide() {
        super.onFragmentHide()
        vmApp<MessageNotifyModel>().closeMessageNotify = false
    }

    open fun onInitConversation() {
        _adapter.toLoading()

        //监听连接状态
        vmApp<ChatModel>().sdkConnectData.observe { connect ->
            if (connect == false) {
                //未连接
                _adapter.changeHeaderItems {
                    if (!it.contains(chatConnectTipItem)) {
                        it.add(0, chatConnectTipItem)
                    }
                }
            } else {
                //网络链接
                _adapter.changeHeaderItems {
                    it.remove(chatConnectTipItem)
                }
            }
        }

        //监听会话列表变化
        conversationModel.conversationListData.observe {
            if (conversationModel.conversationListData.isSetValue) {

                val list = mutableListOf<ConversationInfoBean>()
                for (i in 0..30) {
                    list.addAll(it ?: emptyList())
                }

                loadDataEndIndex(ConversationItem::class.java, it) { bean, index ->
                    initConversationItem(bean)

                    itemLoadImageTextBgColor = randomColorList.getSafe(index)!!.toColor()

                    //click
                    itemClick = {
                        onClickConversationItem(this, it, bean)
                    }

                    //long press
                    itemLongClick = {
                        onLongClickConversationItem(this, it, bean)
                    }

                    //init
                    onInitConversationItem(this, bean)
                }
            }
        }
    }

    override fun onLoadData() {
        super.onLoadData()

        //主动获取会话列表
        conversationModel.fetchConversationList()
    }

    /**初始化[DslMessageListItem]*/
    open fun onInitConversationItem(item: DslMessageListItem, bean: ConversationInfoBean) {

    }

    /**点击[DslMessageListItem]*/
    open fun onClickConversationItem(
        item: DslMessageListItem,
        view: View,
        bean: ConversationInfoBean
    ) {
        ConversationHelper.conversationJump(this, bean.toChatInfoBean())
    }

    /**长按[DslMessageListItem]*/
    open fun onLongClickConversationItem(
        item: DslMessageListItem,
        view: View,
        bean: ConversationInfoBean
    ): Boolean {

        fContext().actionPopupWindow(view) {
            if (bean.isTop) {
                addAction("取消置顶") { _, _ ->
                    ConversationHelper.setConversationTop(bean.conversationId, false)
                }
            } else {
                addAction("置顶聊天") { _, _ ->
                    ConversationHelper.setConversationTop(bean.conversationId, true)
                }
            }

            addAction("删除聊天") { _, _ ->
                ConversationHelper.deleteConversation(bean.conversationId) { timSdkException ->
                    if (timSdkException == null) {
                        //删除成功
                        item.removeIt()
                    }
                }

            }
            addAction("清空消息") { _, _ ->
                ConversationHelper.clearHistoryMessage(bean.chatId, bean.isGroup)
            }
        }
        return true
    }
}