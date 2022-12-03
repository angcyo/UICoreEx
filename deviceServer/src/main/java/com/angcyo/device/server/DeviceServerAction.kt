package com.angcyo.device.server

import com.angcyo.core.dslitem.DslLastDeviceInfoItem
import com.angcyo.device.core.actionBody
import com.angcyo.device.core.actionString
import com.angcyo.library.annotation.CallPoint
import com.angcyo.library.ex.toSizeString
import com.koushikdutta.async.http.body.ByteBufferListRequestBody
import com.koushikdutta.async.http.server.AsyncHttpServer

/**
 * 支持的所有接口
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/12/02
 */
object DeviceServerAction {

    /**初始化所有接口*/
    @CallPoint
    fun initServerAction(server: AsyncHttpServer) {
        server.actionString("/") { request, response, body ->
            response.send(body)
        }
        server.actionString("/device") { request, response, body ->
            response.send("${DslLastDeviceInfoItem.deviceInfo()}")
        }
        //大数据字节测试
        server.actionBody("/bytes", ByteBufferListRequestBody()) { request, response ->
            val body = request.getBody<ByteBufferListRequestBody>()
            val length = body.length()
            response.send("共收到数据:$length bytes ${length.toSizeString()}")
        }
    }

}