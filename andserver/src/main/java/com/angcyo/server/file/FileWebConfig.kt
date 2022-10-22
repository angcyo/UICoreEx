package com.angcyo.server.file

import android.content.Context
import com.angcyo.library.libFolderPath
import com.angcyo.server.website.FaviconWebsite
import com.angcyo.server.website.LogWSWebsite
import com.yanzhenjie.andserver.annotation.Config
import com.yanzhenjie.andserver.framework.config.WebConfig
import com.yanzhenjie.andserver.framework.website.BasicWebsite
import com.yanzhenjie.andserver.framework.website.FileBrowser

/**
 * 继承此类, 实现文件浏览服务
 *
 * [com.yanzhenjie.andserver.register.OnRegister]
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/09/22
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */

@Config("file")
class FileWebConfig : WebConfig {

    companion object {

        /**文件服务路径
         * FileUtils.appRootExternalFolder().absolutePath*/
        var fileWebPath = libFolderPath("")

        /**额外的网站配置*/
        val fileWebsiteList = mutableListOf<BasicWebsite>()
    }

    override fun onConfig(context: Context, delegate: WebConfig.Delegate) {
        delegate.addWebsite(FaviconWebsite())
        delegate.addWebsite(LogWSWebsite())
        fileWebsiteList.forEach {
            delegate.addWebsite(it)
        }
        // 添加一个文件浏览器网站
        delegate.addWebsite(FileBrowser(fileWebPath))
        //delegate.addWebsite(FileBrowser(FileUtils.appRootExternalFolder().absolutePath))
    }
}