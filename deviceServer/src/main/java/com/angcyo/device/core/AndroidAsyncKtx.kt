package com.angcyo.device.core

import com.koushikdutta.async.http.AsyncHttpPost
import com.koushikdutta.async.http.body.AsyncHttpRequestBody
import com.koushikdutta.async.http.body.StringBody
import com.koushikdutta.async.http.server.AsyncHttpServer
import com.koushikdutta.async.http.server.AsyncHttpServerRequest
import com.koushikdutta.async.http.server.AsyncHttpServerResponse

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/12/02
 */

/**监听一个请求
 * [regex] 请求地址, 比如:[/] [/device]
 * [method] 请求的方法
 * [action] 回调
 *
 * [com.koushikdutta.async.http.server.AsyncHttpServerRouter.get]
 * [com.koushikdutta.async.http.server.AsyncHttpServerRouter.post]
 * [com.koushikdutta.async.http.server.AsyncHttpServerRouter.websocket]
 * */
fun AsyncHttpServer.action(
    regex: String,
    method: String = AsyncHttpPost.METHOD,
    action: (request: AsyncHttpServerRequest, response: AsyncHttpServerResponse) -> Unit
) {
    addAction(method, regex) { request, response ->
        action(request, response)
    }
}

/**将任意的请求体都解析成字符串*/
fun AsyncHttpServer.actionString(
    regex: String,
    method: String = AsyncHttpPost.METHOD,
    action: (request: AsyncHttpServerRequest, response: AsyncHttpServerResponse, body: String?) -> Unit
) {
    addAction(method, regex, { request, response ->
        action(request, response, request.getBody<StringBody>().get())
    }, StringRequestBodyProvider())
}

/**指定接收类型*/
fun AsyncHttpServer.actionBody(
    regex: String,
    body: AsyncHttpRequestBody<*>,
    method: String = AsyncHttpPost.METHOD,
    action: (request: AsyncHttpServerRequest, response: AsyncHttpServerResponse) -> Unit
) {
    addAction(method, regex, { request, response ->
        action(request, response)
    }, BodyProvider(body))
}