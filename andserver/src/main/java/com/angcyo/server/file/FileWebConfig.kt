package com.angcyo.server.file

import android.content.Context
import com.angcyo.library.libFolderPath
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

        /**是否激活文件服务*/
        var enable = true

        /**文件服务路径
         * FileUtils.appRootExternalFolder().absolutePath*/
        var fileWebPath = libFolderPath("")
    }

    override fun onConfig(context: Context, delegate: WebConfig.Delegate) {
        // 添加一个文件浏览器网站
        if (enable) {
            //delegate.addWebsite(FileBrowser(FileUtils.appRootExternalFolder().absolutePath))
            delegate.addWebsite(FileBrowser(fileWebPath))
        }
    }
}