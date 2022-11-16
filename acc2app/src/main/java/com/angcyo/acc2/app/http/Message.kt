package com.angcyo.acc2.app.http

import com.angcyo.acc2.app.app
import com.angcyo.acc2.app.http.bean.MessageBean
import com.angcyo.acc2.app.model.GiteeModel
import com.angcyo.core.vmApp
import com.angcyo.http.base.fromJson
import com.angcyo.http.base.listType
import com.angcyo.http.base.toJson
import com.angcyo.http.toBean
import com.angcyo.library.ex.*
import com.angcyo.library.getAppVersionCode
import com.angcyo.library.toastQQ

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/02/03
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
object Message {

    const val MESSAGE_KEY = "message"
    var saveMessageBean = MessageBean()

    fun init() {
        MESSAGE_KEY.hawkGet()?.fromJson(MessageBean::class.java)?.let {
            saveMessageBean = it
        }
    }

    /**获取消息*/
    fun fetchMessage(online: Boolean = !isDebugType()) {
        val json = app().memoryConfigBean.file?.message ?: "message"

        if (online) {
            Gitee.get(json) { data, error ->
                data?.toBean<List<MessageBean>>(listType(MessageBean::class.java))?.let {
                    for (bean in it) {
                        if (parseMessage(bean)) {
                            break
                        }
                    }
                }
            }
        } else {
            Gitee.assets<List<MessageBean>>(json, listType(MessageBean::class.java)) {
                for (bean in it) {
                    if (parseMessage(bean)) {
                        break
                    }
                }
            }
        }
    }

    /**返回, 是否要中断后续消息处理*/
    fun parseMessage(bean: MessageBean): Boolean {
        if (checkIgnoreMessage(bean)) {
            return false
        }

        vmApp<GiteeModel>().messageData.postValue(bean)

        when (bean.type) {
            MessageBean.TYPE_NOTIFY -> {
                saveReadMessage(bean)
                toastQQ(bean.message)
            }
            MessageBean.TYPE_KILL_APP -> {
                saveReadMessage(bean)
                UserHelper.exit()
            }
            MessageBean.TYPE_UPDATE_CHECK -> {
                Gitee.fetchAllCheck(true)
                saveReadMessage(bean)
            }
            MessageBean.TYPE_UPDATE_ACTION -> {
                Gitee.fetchAllAction(true)
                Gitee.fetchAllBackAction(true)
                saveReadMessage(bean)
            }
            MessageBean.TYPE_UPDATE_TASK -> {
                Gitee.fetchAllTask(true)
                saveReadMessage(bean)
            }
            MessageBean.TYPE_OPEN_URL -> {
                if (!bean.url.isNullOrEmpty()) {
                    saveReadMessage(bean)
                    //dslTbsOpen(url = bean.url)
                    app().openUrl(bean.url)
                }
            }
            MessageBean.TYPE_UPDATE_MEMORY_CONFIG -> {
                Gitee.fetchMemoryConfig()
                saveReadMessage(bean)
            }
            MessageBean.TYPE_UPDATE_USER -> {
                UserHelper.updateUserInfo { user, ex -> }
                saveReadMessage(bean)
            }
        }

        return bean.interrupt
    }

    /**是否需要忽略此消息*/
    fun checkIgnoreMessage(bean: MessageBean): Boolean {
        if (!bean.enable) {
            //未激活的消息
            return true
        }

        if (isReadMessage(bean.messageId)) {
            //消息已读, 忽略此消息
            return true
        }

        if (bean.codeList != null) {
            if (bean.codeList?.contains(getAppVersionCode().toInt()) == true) {

            } else {
                //版本不匹配
                return true
            }
        }

        if (bean.ignoreUserNames != null &&
            bean.ignoreUserNames.have(UserHelper.userTable?.username)
        ) {
            //被忽略的用户
            return true
        }
        if (bean.userNames != null &&
            !bean.userNames.have(UserHelper.userTable?.username)
        ) {
            //不是指定接收消息的用户
            return true
        }
        return false
    }

    /**指定消息, 已读*/
    fun saveReadMessage(bean: MessageBean) {
        if (bean.forever) {
            return
        }
        //保存消息已读
        saveMessageBean.readMessageId =
            "${saveMessageBean.readMessageId ?: ""},${bean.messageId}"
        saveMessageBean.toJson()?.apply {
            MESSAGE_KEY.hawkPut(this)
        }
    }

    /**指定消息是否已读*/
    fun isReadMessage(messageId: Long): Boolean {
        return saveMessageBean.readMessageId?.contains(",$messageId") == true
    }
}