package com.angcyo.acc2.app.http.bean

/**
 * 对应的配置文件名
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/02/24
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
data class FileConfigBean(
    var memoryConfig: String? = null,
    var function: String? = null,
    var version: String? = null,
    var task: List<String>? = null,
    var action: List<String>? = null,
    var backAction: List<String>? = null,
    var check: List<String>? = null,
)
