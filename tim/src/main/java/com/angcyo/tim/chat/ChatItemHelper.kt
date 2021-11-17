package com.angcyo.tim.chat

import androidx.fragment.app.Fragment
import com.angcyo.dsladapter.DslAdapter
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.find
import com.angcyo.tim.bean.MessageInfoBean
import com.angcyo.tim.dslitem.BaseChatMsgItem
import com.angcyo.tim.dslitem.MsgImageItem
import com.angcyo.tim.dslitem.MsgTextItem
import com.tencent.imsdk.v2.V2TIMMessage

/**
 * 将[MessageInfoBean]转换成对应的[DslAdapterItem]
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/15
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
object ChatItemHelper {
}

/**[V2TIMMessage]->[BaseChatMsgItem]
 * [reverse] 将消息进行反序*/
fun List<V2TIMMessage>.toDslAdapterItemList(fragment: Fragment?, reverse: Boolean = true): List<BaseChatMsgItem> {
    val result = mutableListOf<BaseChatMsgItem>()
    val list = mutableListOf<MessageInfoBean>()
    forEach {
        it.toMessageInfoBean()?.let { list.add(it) }
    }
    if (reverse) {
        list.reverse()
    }
    list.forEach {
        it.toDslAdapterItem(fragment)?.let { item ->
            result.add(item)
        }
    }
    return result
}

/**[MessageInfoBean]->[BaseChatMsgItem]*/
fun MessageInfoBean.toDslAdapterItem(fragment: Fragment?): BaseChatMsgItem? {
    val bean = this

    val result: BaseChatMsgItem? = when (message?.elemType) {
        V2TIMMessage.V2TIM_ELEM_TYPE_TEXT -> MsgTextItem()
        V2TIMMessage.V2TIM_ELEM_TYPE_IMAGE, V2TIMMessage.V2TIM_ELEM_TYPE_VIDEO -> MsgImageItem()
        else -> null
    }

    result?.apply {
        itemFragment = fragment
        messageInfoBean = bean
    }
    return result
}

/**通过[MessageInfoBean], 查找列表中的[DslAdapterItem]*/
fun MessageInfoBean.findDslAdapterItem(adapter: DslAdapter?): BaseChatMsgItem? {
    val result: BaseChatMsgItem? = adapter?.find {
        it is BaseChatMsgItem && it.messageInfoBean?.messageId == messageId
    }
    return result
}

/**获取第一个[MessageInfoBean]*/
fun DslAdapter.firstMessageInfoBean(): MessageInfoBean? {
    return find<BaseChatMsgItem> { it is BaseChatMsgItem }?.messageInfoBean
}