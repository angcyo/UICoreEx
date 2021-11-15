package com.angcyo.tim.dslitem

import com.angcyo.core.vmApp
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.isUpdateMedia
import com.angcyo.glide.giv
import com.angcyo.glide.loadAvatar
import com.angcyo.library.ex.chatTimeString
import com.angcyo.library.ex.randomColorList
import com.angcyo.library.ex.toColor
import com.angcyo.library.ex.undefined_res
import com.angcyo.tim.R
import com.angcyo.tim.bean.*
import com.angcyo.tim.model.ChatModel
import com.angcyo.tim.util.TimConfig.SHOW_MESSAGE_TIME_INTERVAL
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.base.replace

/**
 * 基础聊天界面的item,
 * 包含消息的时间, 消息的左右2个头像, 和消息体以及消息状态
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/12
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
open class BaseChatMsgItem : DslAdapterItem() {

    /**真正的消息体布局*/
    var msgContentLayoutId: Int = undefined_res

    /**消息结构体*/
    var messageInfoBean: MessageInfoBean? = null

    init {
        itemLayoutId = R.layout.chat_msg_content_item
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        if (!itemHolder.isBindView) {
            //填充真正的消息内容
            itemHolder.group(R.id.msg_content_layout)?.apply {
                if (msgContentLayoutId != undefined_res) {
                    replace(msgContentLayoutId)
                } else {
                    removeAllViews()
                }
            }
        }

        bindMsgUser(itemHolder, itemPosition, payloads)
        bindMsgTime(itemHolder, itemPosition, payloads)
        bindMsgStyle(itemHolder, itemPosition, payloads)
        bindMsgContent(itemHolder, itemPosition, payloads)
    }

    /**绑定用户相关的数据*/
    open fun bindMsgUser(itemHolder: DslViewHolder, itemPosition: Int, payloads: List<Any>) {
        val mediaUpdate = payloads.isUpdateMedia()
        messageInfoBean?.apply {

            //用户名
            if (isGroup) {
                itemHolder.visible(R.id.msg_user_name_view)
                itemHolder.tv(R.id.msg_user_name_view)?.text = showUserName
            } else {
                itemHolder.gone(R.id.msg_user_name_view)
            }

            //自己发送的消息
            if (isSelf) {
                itemHolder.gone(R.id.msg_left_avatar_view)
                itemHolder.visible(R.id.msg_right_avatar_view)
            } else {
                //对方发送的消息
                itemHolder.visible(R.id.msg_left_avatar_view)
                itemHolder.gone(R.id.msg_right_avatar_view)
            }

            //头像
            if (mediaUpdate) {
                if (isSelf) {
                    //自己
                    itemHolder.giv(R.id.msg_right_avatar_view)
                        ?.loadAvatar(
                            vmApp<ChatModel>().selfFaceUrlData.value,
                            vmApp<ChatModel>().selfShowNameData.value ?: "?",
                            solidColor = randomColorList[1].toColor()
                        )
                } else {
                    //对方
                    itemHolder.giv(R.id.msg_left_avatar_view)
                        ?.loadAvatar(
                            message?.faceUrl,
                            showUserName ?: "?",
                            solidColor = randomColorList.first().toColor()
                        )
                }
            }
        }
    }

    /**消息的时间*/
    open fun bindMsgTime(itemHolder: DslViewHolder, itemPosition: Int, payloads: List<Any>) {
        val timestamp = messageInfoBean?.timestamp ?: 0
        var showTime = false
        if (itemPosition > 1) {
            //距离上一条消息, 大于5分钟时, 才显示时间
            itemDslAdapter?.getValidFilterDataList()?.getOrNull(itemPosition - 1)?.let { prevItem ->
                if (prevItem is BaseChatMsgItem) {
                    prevItem.messageInfoBean?.timestamp?.let { prevTimestamp ->
                        if (timestamp - prevTimestamp >= SHOW_MESSAGE_TIME_INTERVAL) {
                            showTime = true
                        }
                    }
                }
            }
        } else {
            showTime = true
        }

        if (showTime) {
            itemHolder.visible(R.id.msg_time_view)
            itemHolder.tv(R.id.msg_time_view)?.text = timestamp.chatTimeString()
        } else {
            itemHolder.gone(R.id.msg_time_view)
        }
    }

    /**绑定消息的状态*/
    open fun bindMsgStyle(itemHolder: DslViewHolder, itemPosition: Int, payloads: List<Any>) {
        messageInfoBean?.apply {
            _changeMsgContent(itemHolder, isSelf)

            //消息气泡背景
            if (isSelf) {
                itemHolder.view(R.id.msg_content_layout)
                    ?.setBackgroundResource(R.drawable.chat_bubble_right)
            } else {
                itemHolder.view(R.id.msg_content_layout)
                    ?.setBackgroundResource(R.drawable.chat_bubble_left)
            }

            //消息状态
            itemHolder.visible(R.id.msg_sending_view, status == MessageInfoBean.MSG_STATUS_SENDING)
            if (status == MessageInfoBean.MSG_STATUS_SEND_FAIL) {
                //消息发送失败
                itemHolder.visible(R.id.msg_status_view)
            } else {
                itemHolder.gone(R.id.msg_status_view)
            }

            //已读/未读
            if (isGroup || status != MessageInfoBean.MSG_STATUS_SEND_SUCCESS) {
                itemHolder.gone(R.id.msg_read_tip_view)
            } else {
                itemHolder.visible(R.id.msg_read_tip_view)
                itemHolder.tv(R.id.msg_read_tip_view)?.text = if (isPeerRead) {
                    "已读"
                } else {
                    "未读"
                }
            }

        }
    }

    /**将消息内容的样式改变成自己发送的*/
    fun _changeMsgContent(itemHolder: DslViewHolder, toSelf: Boolean) {
        //消息内容的容器
        val msgContentContainerLayout =
            itemHolder.group(R.id.msg_content_container_layout) ?: return
        //消息内容包裹的layout
        val msgContentLayout = itemHolder.group(R.id.msg_content_layout) ?: return
        if (toSelf) {
            //自己发送的消息, 内容布局放在后面
            msgContentContainerLayout.removeView(msgContentLayout)
            msgContentContainerLayout.addView(msgContentLayout)
        } else {
            //否则放在第一个
            msgContentContainerLayout.removeView(msgContentLayout)
            msgContentContainerLayout.addView(msgContentLayout, 0)
        }
    }

    /**绑定消息的内容*/
    open fun bindMsgContent(itemHolder: DslViewHolder, itemPosition: Int, payloads: List<Any>) {

    }
}