package com.angcyo.tim.ui.chat

import android.os.Bundle
import android.view.View
import com.angcyo.base.removeThis
import com.angcyo.core.dslitem.IFragmentItem
import com.angcyo.core.fragment.BaseDslFragment
import com.angcyo.dialog.popup.PopupAction
import com.angcyo.dialog.popup.actionPopupWindow
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.removeIt
import com.angcyo.dsladapter.replaceIt
import com.angcyo.getDataSerializable
import com.angcyo.library.ex.copy
import com.angcyo.library.toastQQ
import com.angcyo.tim.R
import com.angcyo.tim.TimMessage
import com.angcyo.tim.bean.ChatInfoBean
import com.angcyo.tim.bean.MessageInfoBean
import com.angcyo.tim.bean.MessageInfoBean.Companion.MSG_STATUS_REVOKE
import com.angcyo.tim.bean.MessageInfoBean.Companion.MSG_STATUS_SEND_FAIL
import com.angcyo.tim.bean.isSelf
import com.angcyo.tim.chat.BaseChatPresenter
import com.angcyo.tim.dslitem.BaseChatMsgItem
import com.angcyo.tim.helper.ChatConvertHelper
import com.angcyo.tim.helper.convert.BaseConvert
import com.tencent.imsdk.BaseConstants.ERR_REVOKE_TIME_LIMIT_EXCEED
import com.tencent.imsdk.BaseConstants.ERR_SVR_MSG_REVOKE_TIME_LIMIT
import com.tencent.imsdk.v2.V2TIMMessage

/**
 * TIM 基础聊天界面
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/11
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
abstract class BaseChatFragment : BaseDslFragment() {

    init {
        fragmentTitle = ""
        enableRefresh = true
        enableAdapterRefresh = false
        enableContentBounds = false
        page.singlePage()

        enableSoftInput

        fragmentLayoutId = R.layout.lib_chat_fragment
    }

    /**数据结构*/
    var chatInfoBean: ChatInfoBean? = null

    /**界面操作*/
    var chatPresenter: BaseChatPresenter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chatInfoBean = getDataSerializable()
    }

    override fun initBaseView(savedInstanceState: Bundle?) {
        super.initBaseView(savedInstanceState)

        onInitChat()
    }

    override fun onFragmentShow(bundle: Bundle?) {
        super.onFragmentShow(bundle)
    }

    open fun onInitChat() {
        val chatInfo = chatInfoBean
        if (chatInfo == null) {
            toastQQ("数据异常")
            removeThis()
        } else {
            fragmentTitle = chatInfo.chatTitle

            chatPresenter?.initControl(this)

            chatPresenter?.loadMessage()
            chatPresenter?.listenerMessage()
        }
    }

    override fun onBackPressed(): Boolean {
        return super.onBackPressed()
    }

    /**初始化[BaseChatMsgItem]*/
    open fun onInitChatAdapterItem(item: DslAdapterItem) {
        if (item is IFragmentItem) {
            item.itemFragment = this
        }
        /*item.itemLongClick = {
            onLongClickMessageItem(item, it)
        }*/
        val old = item.itemBindOverride
        item.itemBindOverride = { itemHolder, itemPosition, adapterItem, payloads ->
            old.invoke(itemHolder, itemPosition, adapterItem, payloads)

            //在内容上长按时, 才响应菜单
            itemHolder.longClick(R.id.msg_content_layout) {
                onLongClickMessageItem(item, it)
            }
        }
    }

    /**长按[BaseChatMsgItem]*/
    open fun onLongClickMessageItem(item: DslAdapterItem, view: View): Boolean {
        if (item is BaseChatMsgItem) {
            item.messageInfoBean?.let { bean ->
                val actions = initMessageItemPopupActions(item, bean)
                if (actions.isNotEmpty()) {
                    fContext().actionPopupWindow(view) {
                        actionList.addAll(actions)
                    }
                    return true
                }
            }
        }
        return false
    }

    /**初始化对应的菜单选项*/
    open fun initMessageItemPopupActions(
        item: DslAdapterItem,
        bean: MessageInfoBean
    ): List<PopupAction> {
        val result = mutableListOf<PopupAction>()

        if (bean.msgType == V2TIMMessage.V2TIM_ELEM_TYPE_TEXT) {
            result.add(PopupAction("复制") { any, view ->
                bean.content?.copy()
            })
        }

        result.add(PopupAction("删除") { any, view ->
            bean.message?.let { timMessage ->
                TimMessage.deleteMessages(listOf(timMessage)) {
                    if (it == null) {
                        item.removeIt()
                    }
                }
            }
        })

        //自己的消息
        if (bean.isSelf) {
            if (bean.status == MSG_STATUS_SEND_FAIL) {
                result.add(PopupAction("重发") { any, view ->
                    chatPresenter?.sendMessage(bean, true)
                })
            } else {
                result.add(PopupAction("撤回") { any, view ->
                    bean.message?.let { timMessage ->
                        TimMessage.revokeMessage(timMessage) {
                            if (it == null) {
                                BaseConvert.baseMessageInfoBean(timMessage).apply {
                                    //撤回的消息
                                    msgType = MSG_STATUS_REVOKE
                                    status = MSG_STATUS_REVOKE
                                    content = "您撤回了一条消息"
                                    ChatConvertHelper.convertToItem(this)?.let { newItem ->
                                        item.replaceIt(newItem)
                                    }
                                }
                            } else {
                                if (it.code == ERR_REVOKE_TIME_LIMIT_EXCEED ||
                                    it.code == ERR_SVR_MSG_REVOKE_TIME_LIMIT
                                ) {
                                    toastQQ("消息发送已超过2分钟")
                                } else {
                                    toastQQ("撤回失败${it.code}=${it.desc}")
                                }
                            }
                        }
                    }
                })
            }
        }

        return result
    }
}