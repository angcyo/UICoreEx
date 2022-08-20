package com.angcyo.server.website

import com.yanzhenjie.andserver.framework.body.StringBody
import com.yanzhenjie.andserver.framework.website.BasicWebsite
import com.yanzhenjie.andserver.http.HttpRequest
import com.yanzhenjie.andserver.http.HttpResponse
import com.yanzhenjie.andserver.http.ResponseBody
import com.yanzhenjie.andserver.util.MediaType

/**
 * 实时输出log
 * [com.angcyo.websocket.LogServerService]
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2022/08/20
 * Copyright (c) 2020 angcyo. All rights reserved.
 */
class LogWSWebsite : BasicWebsite() {

    override fun intercept(request: HttpRequest): Boolean {
        val httpPath = request.path
        return httpPath == "/ws"
    }

    override fun getBody(request: HttpRequest, response: HttpResponse): ResponseBody {
        return StringBody("<h3>test</h3>", MediaType.TEXT_HTML)
    }

}