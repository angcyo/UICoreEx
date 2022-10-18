package com.angcyo.server.website

import com.angcyo.library.ex.*
import com.angcyo.server.DslAndServer
import com.yanzhenjie.andserver.framework.body.StreamBody
import com.yanzhenjie.andserver.framework.website.BasicWebsite
import com.yanzhenjie.andserver.http.HttpRequest
import com.yanzhenjie.andserver.http.HttpResponse
import com.yanzhenjie.andserver.http.ResponseBody
import com.yanzhenjie.andserver.util.MediaType
import java.io.InputStream

/**
 * 网站图标[favicon.ico]
 *
 * Accept: image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8
 * Accept-Encoding: gzip, deflate
 * Accept-Language: zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/07/06
 * Copyright (c) 2020 angcyo. All rights reserved.
 */

class FaviconWebsite : BasicWebsite() {

    override fun intercept(request: HttpRequest): Boolean {
        val httpPath = request.path
        return httpPath == "/favicon.ico" && request.getHeader("Accept").isImageMimeType()
    }

    override fun getBody(request: HttpRequest, response: HttpResponse): ResponseBody {
        val drawable = _drawable(DslAndServer.DEFAULT_NOTIFY_ICON!!)
        val bitmap = drawable?.toBitmap()
        val bytes = bitmap?.toBytes()
        val inputStream: InputStream? = bytes?.toInputStream()
        return StreamBody(inputStream, (bytes?.size ?: 0).toLong(), MediaType.IMAGE_PNG)
    }
}