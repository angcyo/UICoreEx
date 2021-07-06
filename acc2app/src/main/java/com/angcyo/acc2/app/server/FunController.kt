package com.angcyo.acc2.app.server

import android.graphics.Bitmap
import com.angcyo.acc2.app.app
import com.angcyo.core.component.Screenshot
import com.angcyo.library.ex.await
import com.angcyo.server.sendBitmap
import com.yanzhenjie.andserver.annotation.Controller
import com.yanzhenjie.andserver.annotation.GetMapping
import com.yanzhenjie.andserver.http.HttpRequest
import com.yanzhenjie.andserver.http.HttpResponse

/**
 * https://yanzhenjie.com/AndServer/annotation/Controller.html
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/07/06
 * Copyright (c) 2020 angcyo. All rights reserved.
 */

@Controller
class FunController {

    /*
    *//**屏幕截图*//*
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

}