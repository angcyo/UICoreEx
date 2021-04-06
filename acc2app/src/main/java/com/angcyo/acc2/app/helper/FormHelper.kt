package com.angcyo.acc2.app.helper

import com.angcyo.acc2.app.component.Task
import com.angcyo.acc2.bean.FormBean
import com.angcyo.acc2.bean.handleParams
import com.angcyo.http.GET
import com.angcyo.http.base.jsonObject
import com.angcyo.http.get
import com.angcyo.http.post
import com.angcyo.http.rx.observer
import com.angcyo.library.L
import com.angcyo.library.ex.isDebugType
import com.angcyo.library.ex.toStr
import com.google.gson.JsonElement
import io.reactivex.disposables.Disposable
import retrofit2.Response

/**
 * [AutoParseAction]完成后, 或者[AutoParseInterceptor]流程结束后, 要提交的表单数据
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/07/29
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */

/**发送表单请求*/
fun FormBean.request(
    result: (
        data: Response<JsonElement>?,
        error: Throwable?
    ) -> Unit = { _, _ -> },
    configParams: (params: HashMap<String, Any>) -> Unit = {}
): Disposable? {
    return if (url.isNullOrEmpty()) {
        L.w("form url is null/empty.")

        if (isDebugType()) {
            val requestParams = handleParams(configParams)
            L.i("表单参数:", requestParams)
        }
        null
    } else {
        if (method == GET) {
            get {
                url = this@request.url!!

                //请求参数
                query = handleParams(configParams)
            }
        } else {
            post {
                url = this@request.url!!

                //请求参数
                val requestParams = handleParams(configParams)

                //请求体
                if (contentType == FormBean.CONTENT_TYPE_FORM) {
                    formMap = requestParams
                } else {
                    body = jsonObject {
                        requestParams.forEach { entry ->
                            add(entry.key, entry.value)
                        }
                    }
                }
            }
        }.observer {
            onObserverEnd = { data, error ->
                error.let {
                    //错误日志, 写入acc
                    Task.control.accPrint.log(it?.toStr())
                }
                result(data, error)
                L.d(data, error)
            }
        }
    }
}

///**错误信息的赋值等*/
//fun HashMap<String, Any>.bindErrorCode(error: ActionException?) {
//    this[FormBean.KEY_CODE] = when (error) {
//        null -> 200 //本地执行成功
//        is ActionInterruptedNextException -> 301 //本地执行中断, 但是需要继续任务
//        is ActionInterruptedException -> 300 //本地执行中断, 任务终止.
//        else -> 500 //本地执行错误, 任务终止.
//    }
//}