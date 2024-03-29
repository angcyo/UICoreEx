package com.angcyo.acc2.app.server

import android.content.Context
import com.angcyo.server.file.FileWebConfig
import com.angcyo.server.website.FaviconWebsite
import com.yanzhenjie.andserver.annotation.Config
import com.yanzhenjie.andserver.framework.config.WebConfig
import com.yanzhenjie.andserver.framework.website.FileBrowser


/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/07/05
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */

@Config("acc")
class AccServerConfig : WebConfig {

    override fun onConfig(context: Context, delegate: WebConfig.Delegate) {
        // 添加一个文件浏览器网站
        //delegate.addWebsite(FileBrowser(FileUtils.appRootExternalFolder().absolutePath))

        delegate.addWebsite(FaviconWebsite())
        delegate.addWebsite(FileBrowser(FileWebConfig.fileWebPath))
    }
}