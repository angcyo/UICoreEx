package com.angcyo.server.def

import android.graphics.Bitmap
import com.angcyo.http.base.fromJson
import com.angcyo.http.base.toJson
import com.angcyo.library.ex.toBytes
import com.angcyo.library.ex.toInputStream
import com.angcyo.library.ex.toText
import com.yanzhenjie.andserver.annotation.Converter
import com.yanzhenjie.andserver.framework.MessageConverter
import com.yanzhenjie.andserver.framework.body.JsonBody
import com.yanzhenjie.andserver.framework.body.StreamBody
import com.yanzhenjie.andserver.framework.body.StringBody
import com.yanzhenjie.andserver.http.ResponseBody
import com.yanzhenjie.andserver.util.MediaType
import java.io.InputStream
import java.lang.reflect.Type

/**
 *
 * 同一组, 如果有多个转换器, 只有扫描到的最后一个有效
 * [com.yanzhenjie.andserver.register.OnRegister.onRegister]
 *
 * com.angcyo.acc2.app.server.DoControllerTaskHandler.onHandle
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/07/07
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */

@Converter
class MessageBodyConverter : MessageConverter {

    /**将返回的JavaBean数据, 转换成[ResponseBody]*/
    override fun convert(output: Any?, mediaType: MediaType?): ResponseBody? {
        if (output == null) {
            return StringBody("output is null.")
        }
        if ((mediaType == null && output is String) || mediaType == MediaType.TEXT_PLAIN) {
            //文本类型
            return StringBody(output.toString())
        } else if (output is ByteArray && (mediaType == null || mediaType == MediaType.APPLICATION_OCTET_STREAM)) {
            //流类型
            val inputStream: InputStream = output.toInputStream()
            return StreamBody(inputStream, output.size.toLong())
        } else if (output is Bitmap) {
            //图片类型
            val bytes = output.toBytes()
            val inputStream: InputStream? = bytes?.toInputStream()
            return StreamBody(inputStream, (bytes?.size ?: 0).toLong(), MediaType.IMAGE_PNG)
        } else if (mediaType == null || mediaType == MediaType.APPLICATION_JSON) {
            //json类型
            return JsonBody(output.toJson { /*def*/ })
        }
        return null
    }

    /**将输入流, 转成JavaBean*/
    override fun <T : Any?> convert(stream: InputStream, mediaType: MediaType?, type: Type?): T? {
        if (mediaType == MediaType.APPLICATION_JSON && type != null) {
            return stream.toText().fromJson<T>(type)
        }
        return null
    }
}