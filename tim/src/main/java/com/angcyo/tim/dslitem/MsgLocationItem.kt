package com.angcyo.tim.dslitem

import com.angcyo.amap3d.fragment.aMapDetail
import com.angcyo.library.ex._drawable
import com.angcyo.tim.R
import com.angcyo.tim.bean.isSelf
import com.angcyo.tim.bean.locationElem
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.layout.RFrameLayout

/**
 * 位置消息
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/17
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class MsgLocationItem : BaseChatMsgItem() {

    init {
        msgContentLayoutId = R.layout.msg_location_layout
    }

    override fun bindMsgStyle(itemHolder: DslViewHolder, itemPosition: Int, payloads: List<Any>) {
        super.bindMsgStyle(itemHolder, itemPosition, payloads)
        //取消消息的背景
        itemHolder.view(R.id.msg_content_layout)?.background = null
    }

    override fun bindMsgContent(itemHolder: DslViewHolder, itemPosition: Int, payloads: List<Any>) {
        super.bindMsgContent(itemHolder, itemPosition, payloads)

        itemHolder.tv(R.id.msg_location_desc_view)?.text = messageInfoBean?.locationElem?.desc

        itemHolder.click(R.id.msg_content_layout) {
            //消息内容点击
            messageInfoBean?.locationElem?.let {
                itemFragment?.aMapDetail(it.latitude, it.longitude, it.desc)
            }
        }
    }
}