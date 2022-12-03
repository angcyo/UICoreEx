package com.angcyo.device.core

import com.koushikdutta.async.http.Headers
import com.koushikdutta.async.http.body.AsyncHttpRequestBody
import com.koushikdutta.async.http.body.StringBody
import com.koushikdutta.async.http.server.AsyncHttpRequestBodyProvider

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/12/02
 */
class StringRequestBodyProvider : AsyncHttpRequestBodyProvider {
    override fun getBody(headers: Headers?): AsyncHttpRequestBody<*> = StringBody()
}