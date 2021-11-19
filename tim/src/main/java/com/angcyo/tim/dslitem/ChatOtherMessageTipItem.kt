package com.angcyo.tim.dslitem

import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.item.style.ITextItem
import com.angcyo.item.style.TextItemConfig
import com.angcyo.tim.R

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/19
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class ChatOtherMessageTipItem : DslAdapterItem(), ITextItem {

    override var textItemConfig: TextItemConfig = TextItemConfig()

    init {
        itemLayoutId = R.layout.chat_other_message_tip_item
    }
}