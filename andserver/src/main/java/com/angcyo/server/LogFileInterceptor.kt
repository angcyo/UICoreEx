package com.angcyo.server

import com.angcyo.library.ex.readText
import com.angcyo.library.utils.FileUtils
import com.yanzhenjie.andserver.annotation.Interceptor
import com.yanzhenjie.andserver.framework.HandlerInterceptor
import com.yanzhenjie.andserver.framework.body.StringBody
import com.yanzhenjie.andserver.framework.handler.RequestHandler
import com.yanzhenjie.andserver.http.HttpRequest
import com.yanzhenjie.andserver.http.HttpResponse
import java.io.File


/**
 * 拦截*.log文件, 直接返回文件内容
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/07/05
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */

@Interceptor
class LogFileInterceptor : HandlerInterceptor {

    override fun onIntercept(
        request: HttpRequest,
        response: HttpResponse,
        handler: RequestHandler
    ): Boolean {
        val httpPath = request.path
        val valueMap = request.parameter
        if (valueMap["delete"]?.firstOrNull() == "true") {
            val logFile = File(FileUtils.appRootExternalFolder(), httpPath)
            response.setBody(StringBody("删除文件[${logFile.delete()}]:${logFile.absolutePath}"))
            return true
        }
        if (valueMap["raw"]?.firstOrNull() == "true") {
            return false
        }
        if (httpPath.endsWith(".log")) {
            val logFile = File(FileUtils.appRootExternalFolder(), httpPath)
            if (logFile.exists()) {
                response.setBody(StringBody(logFile.readText()))
                return true
            }
        }
        return false
    }
}