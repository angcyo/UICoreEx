package com.angcyo.acc2.app.server

import com.angcyo.library.ex.nowTimeString
import com.angcyo.library.toastQQ
import com.yanzhenjie.andserver.annotation.GetMapping
import com.yanzhenjie.andserver.annotation.PathVariable
import com.yanzhenjie.andserver.annotation.RestController
import com.yanzhenjie.andserver.framework.body.StringBody
import com.yanzhenjie.andserver.http.HttpRequest
import com.yanzhenjie.andserver.http.HttpResponse
import com.yanzhenjie.andserver.http.ResponseBody

/**
 * https://yanzhenjie.com/AndServer/annotation/Controller.html
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/07/06
 * Copyright (c) 2020 angcyo. All rights reserved.
 */

@RestController
class FunController {

    /*
    */
    /**屏幕截图*//*
    @GetMapping("/shot")
    fun screenshot(request: HttpRequest, response: HttpResponse) {
        val await = await(1)
        Screenshot.capture(app(), object : Screenshot.OnCaptureListener {
            override fun onCapture(bitmap: Bitmap, filePath: String?) {
                response.sendBitmap(bitmap)
                await.countDown()
            }
        }).startToShot()
        await.await()
    }*/

    @GetMapping("/test")
    fun test(request: HttpRequest, response: HttpResponse): ResponseBody {
        //response.setBody()
        return StringBody(nowTimeString())
    }

    @GetMapping("/toast/{msg}")
    fun toast(
        request: HttpRequest,
        response: HttpResponse,
        @PathVariable("msg", required = false) msg: String? = null
    ): ResponseBody {
        val message = msg ?: nowTimeString()
        toastQQ(message)
        return StringBody(message)
    }

}