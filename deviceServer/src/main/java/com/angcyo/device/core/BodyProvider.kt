package com.angcyo.device.core

import com.koushikdutta.async.http.Headers
import com.koushikdutta.async.http.body.AsyncHttpRequestBody
import com.koushikdutta.async.http.server.AsyncHttpRequestBodyProvider

/**
 * 强制指定body类型, 而不是通过header识别
 *
 * [com.koushikdutta.async.http.body.StringBody]
 * [com.koushikdutta.async.http.body.JSONObjectBody]
 * [com.koushikdutta.async.http.body.JSONArrayBody]
 * [com.koushikdutta.async.http.body.StreamBody]
 * [com.koushikdutta.async.http.body.FileBody]
 * [com.koushikdutta.async.http.body.ByteBufferListRequestBody]
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/12/02
 */
class BodyProvider<T : AsyncHttpRequestBody<*>>(val body: T) : AsyncHttpRequestBodyProvider {
    override fun getBody(headers: Headers?): AsyncHttpRequestBody<*> = body
}