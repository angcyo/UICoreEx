package com.angcyo.server

import android.content.Context
import com.yanzhenjie.andserver.annotation.Config
import com.yanzhenjie.andserver.framework.config.WebConfig


/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/07/06
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */

@Config
class AndServerConfig : WebConfig {
    override fun onConfig(context: Context, delegate: WebConfig.Delegate) {
        // 添加一个网站图标
        delegate.addWebsite(FaviconWebsite())
    }
}