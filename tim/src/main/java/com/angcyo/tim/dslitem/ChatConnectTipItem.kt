package com.angcyo.tim.dslitem

import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.library.component.DslIntent.Companion.openWirelessIntent
import com.angcyo.tim.R

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/19
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class ChatConnectTipItem : DslAdapterItem() {

    init {
        itemLayoutId = R.layout.chat_connect_tip_item
        itemClick = {
            openWirelessIntent(it.context)
        }
    }
}