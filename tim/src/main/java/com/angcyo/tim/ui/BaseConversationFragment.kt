package com.angcyo.tim.ui

import android.os.Bundle
import com.angcyo.core.fragment.BaseDslFragment
import com.angcyo.core.vmApp
import com.angcyo.dsladapter.toLoading
import com.angcyo.item.DslMessageListItem
import com.angcyo.item.style.*
import com.angcyo.library.ex.*
import com.angcyo.library.toast
import com.angcyo.tim.conversation.ConversationModel
import com.angcyo.tim.bean.faceUrl

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
                        itemDes = draftInfo.text
                        itemTime = draftInfo.time.shotTimeString()
                    } else {
                        itemDes = if (!bean.atInfoText.isNullOrEmpty()) {
                            //at信息
                            bean.atInfoText
                        } else {
                            bean.messageInfoBean?.content
                        }
                        itemTime = bean.lastMessageTime.shotTimeString()
                    }

                    itemBadgeText = if (bean.unReadCount > 0) {
                        ""
                    } else {
                        null
                    }

                    itemClick = {
                        toast(bean.toString())
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