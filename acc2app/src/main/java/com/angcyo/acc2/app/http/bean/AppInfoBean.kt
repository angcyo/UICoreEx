package com.angcyo.acc2.app.http.bean

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/09/10
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */

data class AppInfoBean(
    var packageName: String? = null,
    var label: String? = null,
    var iconUrl: String? = null,
    /**是否是主业务*/
    var main: Boolean = false,
    /**
     * 1:抖音 2:快手 3:微信 4:小红书
     * 11:抖音极速 22:快手极速 44:今日头条急速 55:趣头条 66:刷宝
     * */
    var type: Int = -1,
    //指定需要接收的包名
    var packageNames: String? = null,
    //忽略消息的包名
    var ignorePackageNames: String? = null,
    //指定需要接收消息的用户
    var userNames: String? = null,
    //忽略消息的用户
    var ignoreUserNames: String? = null,
    var enable: Boolean = true,
    /**debug模式下可见*/
    var debug: Boolean = false,
    var hide: Boolean = false,
)