package com.angcyo.server.file

import com.angcyo.library.app
import com.angcyo.library.getAppName
import com.angcyo.server.def.AndServerService

/**
 * 文件服务
 *
 * [com.angcyo.server.file.FileWebConfig]
 * [com.angcyo.server.LogFileInterceptor]
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/09/22
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class FileServerService : AndServerService() {

    companion object {

        /**[com.yanzhenjie.andserver.AndServer.webServer(android.content.Context, java.lang.String)]*/
        const val GROUP_NAME = "file"
    }

    init {
        notifyName = "${app().getAppName()}-FileServer"
        notifyChannelName = "FileServer"
        group = GROUP_NAME
    }

    override fun initServer() {
        super.initServer()
    }

    override fun updateNotify() {
        super.updateNotify()
    }
}