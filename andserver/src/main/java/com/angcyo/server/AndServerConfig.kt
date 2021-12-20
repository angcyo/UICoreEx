package com.angcyo.server

import android.content.Context
import com.yanzhenjie.andserver.annotation.Config
import com.yanzhenjie.andserver.framework.config.Multipart
import com.yanzhenjie.andserver.framework.config.WebConfig
import java.io.File


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
        delegate.setMultipart(
            Multipart.newBuilder()
                // 单个请求上传文件总大小
                .allFileMaxSize(1024 * 1024 * 20L)
                // 单个文件的最大大小
                .fileMaxSize(1024 * 1024 * 5L)
                // 保存上传文件时buffer大小
                .maxInMemorySize(1024 * 10)
                // 文件保存目录
                .uploadTempDir(File(context.cacheDir, "_server_upload_cache_"))
                .build()
        )
    }
}