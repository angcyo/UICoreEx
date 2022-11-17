package com.angcyo.server.def

import com.yanzhenjie.andserver.annotation.Interceptor
import com.yanzhenjie.andserver.framework.HandlerInterceptor
import com.yanzhenjie.andserver.framework.handler.RequestHandler
import com.yanzhenjie.andserver.http.HttpHeaders
import com.yanzhenjie.andserver.http.HttpRequest
import com.yanzhenjie.andserver.http.HttpResponse

/**
 * 跨域拦截器
 *
 * has been blocked by CORS policy:
 * No 'Access-Control-Allow-Origin' header is present on the requested resource.
 * If an opaque response serves your needs, set the request's mode to 'no-cors' to fetch the resource with CORS disabled.
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/11/17
 */
@Interceptor
open class DefCorsInterceptor : HandlerInterceptor {
    override fun onIntercept(
        request: HttpRequest,
        response: HttpResponse,
        handler: RequestHandler
    ): Boolean {
        response.setHeader(HttpHeaders.Access_Control_Allow_Origin, "*")
        response.setHeader(HttpHeaders.EXPIRES, "0")
        response.setHeader(HttpHeaders.PRAGMA, "no-cache")
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store")
        response.setHeader("Cache", "no-store")
        return false
    }
}