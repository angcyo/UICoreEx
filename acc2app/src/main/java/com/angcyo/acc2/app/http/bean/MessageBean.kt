package com.angcyo.acc2.app.http.bean

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/08/29
 * Copyright (c) 2020 angcyo. All rights reserved.
 */
data class MessageBean(
    //消息id, 已读后. 不提示
    var messageId: Long = -1,
    //激活
    var enable: Boolean = false,
    var message: String? = null,
    var title: String? = null,
    //需要跳转的网页
    var url: String? = null,
    var urlLabel: String? = null,
    //需要展示的图片地址
    var imageUrl: String? = null,
    //指定需要接收消息的用户
    var userNames: String? = null,
    //忽略消息的用户
    var ignoreUserNames: String? = null,
    //消息永不过期
    var forever: Boolean = false,
    //强提示, 不允许关闭
    var force: Boolean = false,
    //是否中断后续消息
    var interrupt: Boolean = false,

    //消息的类型, 比如更新check等
    var type: String? = null,

    //只有指定的版本才会收到的消息, 不指定, 表示所有版本
    var codeList: List<Int>? = null,

    /*-----以下是本地存储数据字段-------*/

    //已读消息记录
    var readMessageId: String? = null
) {
    companion object {
        //退出程序
        const val TYPE_KILL_APP = "killApp"

        //显示一条通知
        const val TYPE_NOTIFY = "notify"
        const val TYPE_UPDATE_CHECK = "updateCheck"
        const val TYPE_UPDATE_ACTION = "updateAction"
        const val TYPE_UPDATE_TASK = "updateTask"
        const val TYPE_UPDATE_INDUSTRY = "updateIndustry"
        const val TYPE_UPDATE_AREA = "updateArea"
        const val TYPE_UPDATE_MEMORY_CONFIG = "updateMemoryConfig"

        //更新用户信息
        const val TYPE_UPDATE_USER = "updateUser"

        //在首页打开url
        const val TYPE_OPEN_URL = "openUrl"
        const val TYPE_OPEN_LEFT_URL = "openLeftUrl"
    }
}
