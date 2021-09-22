package com.angcyo.server.file

import android.content.Context
import com.angcyo.library.utils.FileUtils
import com.yanzhenjie.andserver.annotation.Config
import com.yanzhenjie.andserver.framework.config.WebConfig
import com.yanzhenjie.andserver.framework.website.FileBrowser

/**
 * 继承此类, 实现文件浏览服务
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/09/22
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */

@Config
open class FileWebConfig : WebConfig {

    companion object {
        var enable = true
    }

    override fun onConfig(context: Context, delegate: WebConfig.Delegate) {
        // 添加一个文件浏览器网站
        if (enable) {
            delegate.addWebsite(FileBrowser(FileUtils.appRootExternalFolder()!!.absolutePath))
        }
    }
}