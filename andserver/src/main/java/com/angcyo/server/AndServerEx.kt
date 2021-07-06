package com.angcyo.server

import android.graphics.Bitmap
import com.angcyo.library.ex.toBytes
import com.angcyo.library.ex.toInputStream
import com.yanzhenjie.andserver.framework.body.StreamBody
import com.yanzhenjie.andserver.http.HttpResponse
import com.yanzhenjie.andserver.util.MediaType
import java.io.InputStream

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/07/06
 * Copyright (c) 2020 angcyo. All rights reserved.
 */

/**发送图片*/
fun HttpResponse.sendBitmap(bitmap: Bitmap, mediaType: MediaType = MediaType.IMAGE_PNG) {
    val bytes = bitmap.toBytes()
    val inputStream: InputStream? = bytes?.toInputStream()
    setBody(StreamBody(inputStream, (bytes?.size ?: 0).toLong(), mediaType))
}