package com.angcyo.tim.dslitem

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import com.angcyo.core.coreApp
import com.angcyo.item.DslMessageListItem
import com.angcyo.item.style.*
import com.angcyo.library.ex.orString
import com.angcyo.library.ex.shotTimeString
import com.angcyo.library.ex.toColor
import com.angcyo.tim.bean.ConversationInfoBean
import com.angcyo.tim.bean.faceUrl
import com.angcyo.tim.helper.convert.UnknownConvert
import com.angcyo.tim.util.handlerEmojiText
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.base.setRBgDrawable
import com.tencent.imsdk.v2.V2TIMMessage

/**
 * 会话列表item
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/29
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class ConversationItem : DslMessageListItem() {

    /**会话信息*/
    var conversationInfo: ConversationInfoBean? = null

    /**置顶时的背景提示*/
    var topBackgroundDrawable: Drawable? = ColorDrawable("#F5F6FA".toColor())

    override fun _initItemBackground(itemHolder: DslViewHolder) {
        super._initItemBackground(itemHolder)

        itemHolder.itemView.setRBgDrawable(
            if (conversationInfo?.isTop == true) {
                topBackgroundDrawable
            } else {
                null
            }
        )
    }

    /**初始化item*/
    fun initConversationItem(bean: ConversationInfoBean) {
        conversationInfo = bean

        itemText = bean.title
        itemLoadImage = coreApp().toUrl(bean.messageInfoBean?.faceUrl)
        itemLoadImageText = itemText.orString()
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
            } else if (bean.messageInfoBean == null) {
                UnknownConvert.UNKNOWN_TYPE
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
    }

}