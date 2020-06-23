package com.angcyo.jpush

import android.content.Context
import cn.jpush.android.api.JPushInterface

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/06/23
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
object JPush {

    /**初始化入口*/
    fun init(context: Context, debug: Boolean = BuildConfig.DEBUG) {
        JPushInterface.setDebugMode(debug)
        JPushInterface.init(context.applicationContext)
    }
}