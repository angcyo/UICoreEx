package com.angcyo.server.file

import com.angcyo.server.def.DefCorsInterceptor
import com.yanzhenjie.andserver.annotation.Interceptor

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
@Interceptor(FileServerService.GROUP_NAME)
class FileCorsInterceptor : DefCorsInterceptor()