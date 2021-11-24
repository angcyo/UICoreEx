package com.angcyo.tim.ui

import android.os.Bundle
import com.angcyo.core.fragment.BaseDslFragment
import com.angcyo.core.vmApp
import com.angcyo.dsladapter.toLoading
import com.angcyo.item.DslMessageListItem
import com.angcyo.item.style.*
import com.angcyo.library.ex.*
import com.angcyo.tim.bean.ConversationInfoBean
import com.angcyo.tim.bean.faceUrl
import com.angcyo.tim.bean.msgType
import com.angcyo.tim.dslitem.ChatConnectTipItem
import com.angcyo.tim.helper.ConversationHelper
import com.angcyo.tim.model.ChatModel
import com.angcyo.tim.model.ConversationModel
import com.angcyo.tim.util.handlerEmojiText
import com.tencent.imsdk.v2.V2TIMMessage

/**
 * 会话列表界面
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

    open fun onInitConversation() {
        _adapter.toLoading()

        vmApp<ChatModel>().sdkConnectData.observe {
            if (it == false) {
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

        conversationModel.conversationListData.observe {
            if (conversationModel.conversationListData.isSetValue) {
                loadDataEndIndex(DslMessageListItem::class.java, it) { bean, index ->
                    itemText = bean.title
                    itemLoadImage = bean.messageInfoBean?.faceUrl
                    itemLoadImageText = itemText.orString()
                    itemLoadImageTextBgColor = randomColorList.getSafe(index)!!.toColor()
                    itemShowLastLineView = true

                    val draftInfo = bean.draftInfo
                    if (draftInfo != null) {
                        //草稿内容
                        itemDes = draftInfo.text.handlerEmojiText()
                        itemTime = draftInfo.time.shotTimeString()
                    } else {
                        val content = bean.messageInfoBean?.content
                        itemDes = if (!bean.atInfoText.isNullOrEmpty()) {
                            //at信息
                            bean.atInfoText
                        } else {
                            when (bean.messageInfoBean?.msgType) {
                                V2TIMMessage.V2TIM_ELEM_TYPE_TEXT -> content?.handlerEmojiText()
                                else -> content
                            }
                        }
                        itemTime = bean.lastMessageTime.shotTimeString()
                    }

                    itemBadgeText = if (bean.unReadCount > 0) {
                        ""
                    } else {
                        null
                    }

                    //click
                    itemClick = {
                        onClickConversationItem(bean)
                    }

                    //init
                    onInitConversationItem(this, bean)
                }
            }
        }
    }

    override fun onLoadData() {
        super.onLoadData()
        conversationModel.fetchConversationList()
    }

    open fun onInitConversationItem(item: DslMessageListItem, bean: ConversationInfoBean) {

    }

    open fun onClickConversationItem(bean: ConversationInfoBean) {
        ConversationHelper.conversationJump(this, bean)
    }
}