package com.angcyo.acc2.app.http.bean

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/08/29
 * Copyright (c) 2020 angcyo. All rights reserved.
 */
data class MessageBean(

    //<editor-fold desc="消息配置">

    //消息id, 已读后. 不提示
    var messageId: Long = -1,
    //激活
    var enable: Boolean = false,
    //消息的类型, 比如更新check等
    var type: String? = null,
    //dialog中的标题
    var title: String? = null,
    //消息内容
    var message: String? = null,
    //需要跳转的网页
    var url: String? = null,

    //</editor-fold desc="消息配置">

    //<editor-fold desc="消息目标">

    //指定需要接收的包名
    var packageNames: String? = null,

    //忽略消息的包名
    var ignorePackageNames: String? = null,

    //指定需要接收消息的用户
    var userNames: String? = null,

    //忽略消息的用户
    var ignoreUserNames: String? = null,

    //只有指定的版本才会收到的消息, 不指定, 表示所有版本
    var codeList: List<Int>? = null,

    //</editor-fold desc="消息目标">

    //<editor-fold desc="消息其他属性">

    var urlLabel: String? = null,

    //需要展示的图片地址
    var imageUrl: String? = null,

    //消息永不过期
    var forever: Boolean = false,

    //强提示, 不允许关闭
    var force: Boolean = false,

    //是否中断后续消息
    var interrupt: Boolean = false,

    //</editor-fold desc="消息其他属性">


    /*-----以下是本地存储数据字段-------*/

    //已读消息记录
    var readMessageId: String? = null
) {
    companion object {
        //退出程序
        const val TYPE_KILL_APP = "killApp"

        //显示一条通知
        const val TYPE_NOTIFY = "notify"

        //弹窗提示
        const val TYPE_DIALOG = "dialog"

        //更新数据
        const val TYPE_UPDATE_CHECK = "updateCheck"
        const val TYPE_UPDATE_ACTION = "updateAction"
        const val TYPE_UPDATE_TASK = "updateTask"
        const val TYPE_UPDATE_MEMORY_CONFIG = "updateMemoryConfig"
        const val TYPE_UPDATE_USER = "updateUser"

        //在首页打开url
        const val TYPE_OPEN_URL = "openUrl"
    }
}
