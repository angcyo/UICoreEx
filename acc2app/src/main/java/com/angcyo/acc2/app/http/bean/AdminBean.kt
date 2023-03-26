package com.angcyo.acc2.app.http.bean

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/08/08
 * Copyright (c) 2020 angcyo. All rights reserved.
 */

data class AdminBean(
    //描述
    var des: String? = null,
    //高版本不会被低版本覆盖
    var version: Long = 0,
    //时间
    var time: String? = null,
    //数据
    var data: List<String>? = null,
    //设备码
    var devices: List<String>? = null,
    var debugDevices: List<String>? = null,
    var superDevices: List<String>? = null,
    var selfDevices: List<String>? = null,
    //被禁止使用的设备列表
    var disableDevices: List<String>? = null,
    //vip的设备列表
    var vip: List<String>? = null,
)
