package com.angcyo.server.website

import com.angcyo.library.app
import com.angcyo.library.component.NetUtils
import com.angcyo.library.ex.readAssets
import com.yanzhenjie.andserver.framework.body.StringBody
import com.yanzhenjie.andserver.framework.website.BasicWebsite
import com.yanzhenjie.andserver.http.HttpRequest
import com.yanzhenjie.andserver.http.HttpResponse
import com.yanzhenjie.andserver.http.ResponseBody
import com.yanzhenjie.andserver.util.MediaType

/**
 * http://192.168.2.103:9200/ws
 *
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
        var html = app().readAssets("LogWSWebsite.html")
        val address = NetUtils.localIPAddress ?: "localhost"
        html = html?.replace("{ADDRESS}", "ws:/$address:9300")
        return StringBody(html, MediaType.TEXT_HTML)
    }

}