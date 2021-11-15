package com.angcyo.tim.model

import com.angcyo.core.lifecycle.LifecycleViewModel
import com.angcyo.library.L
import com.angcyo.tim.Tim
import com.angcyo.tim.bean.ConversationInfoBean
import com.angcyo.tim.bean.conversationId
import com.angcyo.tim.bean.toConversationInfoBean
import com.angcyo.viewmodel.vmData
import com.angcyo.viewmodel.vmDataNull
import com.tencent.imsdk.v2.V2TIMConversation
import com.tencent.imsdk.v2.V2TIMConversationListener
import com.tencent.imsdk.v2.V2TIMManager
import com.tencent.imsdk.v2.V2TIMValueCallback

/**
 * Tim 会话数据提供者
 *
 * 会话指南
 * https://cloud.tencent.com/document/product/269/44492
 *
 * 会话api
 * https://im.sdk.qcloud.com/doc/zh-cn/classcom_1_1tencent_1_1imsdk_1_1v2_1_1V2TIMConversationManager.html
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/10
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class ConversationModel : LifecycleViewModel() {

    //<editor-fold desc="成员">

    /**未读数数据*/
    val conversationCountData = vmData<Long>(0)

    /**会话列表, 排好序了*/
    val conversationListData = vmDataNull<List<ConversationInfoBean>>()

    //</editor-fold desc="成员">

    /**会话管理*/
    val conversationManager = V2TIMManager.getConversationManager()

    //<editor-fold desc="操作">

    /**拉取所有会话列表
     * [nextSeq] 分页拉取的游标，第一次默认取传 0，后续分页拉传上一次分页拉取成功回调里的 nextSeq
     * [count] 分页拉取的个数，一次分页拉取不宜太多，会影响拉取的速度，建议每次拉取 100 个会话
     * */
    fun fetchConversationList(nextSeq: Long = 0, count: Int = 100) {
        Tim.getConversationList(nextSeq, count) { v2TIMConversationResult, timSdkException ->
            conversationListData.lastError = timSdkException
            v2TIMConversationResult?.apply {
                notifyConversationList(conversationList, isFinished)
                if (!isFinished) {
                    //未完成继续拉取
                    fetchConversationList(this.nextSeq, count)
                }
            }
        }
    }

    /**通知会话列表数据改变
     * [sort] 是否排序*/
    fun notifyConversationList(conversationList: List<V2TIMConversation>?, sort: Boolean = true) {
        if (conversationList == null) {
            return
        }

        //新的列表
        val list = mutableListOf<ConversationInfoBean>()
        conversationList.forEach {
            it.toConversationInfoBean()?.let { bean ->
                list.add(bean)
            }
        }

        //旧的列表
        val oldList = conversationListData.value ?: emptyList()

        //返回的列表
        val result = mutableListOf<ConversationInfoBean>()

        result.addAll(list)

        //追加旧的会话
        oldList.forEach { bean ->
            val find = list.find { it.conversationId == bean.conversationId }
            if (find == null) {
                result.add(bean)
            }
        }

        if (sort) {
            result.sort()
        }

        conversationListData.value = result
    }

    //</editor-fold desc="操作">

    //<editor-fold desc="监听">

    val _conversationCountListener = object : V2TIMConversationListener() {
        override fun onTotalUnreadMessageCountChanged(totalUnreadCount: Long) {
            conversationCountData.value = totalUnreadCount
        }
    }

    /**监听未读数量变化*/
    fun listenerUnreadCount() {
        conversationManager.getTotalUnreadMessageCount(object :
            V2TIMValueCallback<Long> {
            override fun onSuccess(count: Long) {
                conversationCountData.value = count
            }

            override fun onError(code: Int, desc: String?) {
                L.w("code:$code desc:$desc")
            }
        })
        conversationManager.addConversationListener(_conversationCountListener)
    }

    fun removeListenerUnreadCount() {
        conversationManager.removeConversationListener(_conversationCountListener)
    }

    val _conversationListener = object : V2TIMConversationListener() {

        override fun onNewConversation(conversationList: MutableList<V2TIMConversation>?) {
            notifyConversationList(conversationList)
        }

        override fun onConversationChanged(conversationList: MutableList<V2TIMConversation>?) {
            notifyConversationList(conversationList)
        }
    }

    /**监听会话改变*/
    fun listenerConversation() {
        conversationManager.addConversationListener(_conversationListener)
    }

    fun removeListenerConversation() {
        conversationManager.removeConversationListener(_conversationListener)
    }

    //</editor-fold desc="监听">

}