package com.angcyo.acc2.app.http.bean

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/08/05
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */

data class AdaptiveVersionBean(
    //描述
    var des: String? = null,
    //高版本不会被低版本覆盖
    var version: Long = 0,
    //时间
    var time: String? = null,
    //数据
    var data: List<AppVersionBean>? = null
)

data class AppVersionBean(
    //描述
    var des: String? = null,
    //程序名
    var name: String? = null,
    //包名
    var packageName: String? = null,
    //适配的版本code
    var versionCodeList: List<Int>? = null,
    //适配的版本名
    var versionNameList: List<String>? = null
)