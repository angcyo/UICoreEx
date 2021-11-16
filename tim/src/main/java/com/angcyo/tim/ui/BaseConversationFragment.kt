package com.angcyo.tim.ui

import android.os.Bundle
import com.angcyo.base.dslFHelper
import com.angcyo.core.fragment.BaseDslFragment
import com.angcyo.core.vmApp
import com.angcyo.dsladapter.toLoading
import com.angcyo.item.DslMessageListItem
import com.angcyo.item.style.*
import com.angcyo.library.ex.*
import com.angcyo.putData
import com.angcyo.tim.bean.faceUrl
import com.angcyo.tim.bean.isGroup
import com.angcyo.tim.bean.msgType
import com.angcyo.tim.bean.toChatInfoBean
import com.angcyo.tim.model.ConversationModel
import com.angcyo.tim.ui.chat.GroupChatFragment
import com.angcyo.tim.ui.chat.SingleChatFragment
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
        conversationModel.conversationListData.observe {
            if (conversationModel.conversationListData.isSetValue) {
                loadDataEndIndex(DslMessageListItem::class.java, it) { bean, index ->
                    itemText = bean.title
                    itemLoadImage = bean.messageInfoBean?.faceUrl
                    itemLoadImageText = itemText.orString()
                    itemLoadImageTextBgColor = randomColorList.getSafe(index)!!.toColor()

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

                    itemClick = {
                        dslFHelper {
                            show(if (bean.isGroup) GroupChatFragment::class.java else SingleChatFragment::class.java) {
                                putData(bean.toChatInfoBean())
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onLoadData() {
        super.onLoadData()
        conversationModel.fetchConversationList()
    }
}