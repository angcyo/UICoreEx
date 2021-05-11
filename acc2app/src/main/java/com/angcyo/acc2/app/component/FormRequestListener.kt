package com.angcyo.acc2.app.component

import com.angcyo.acc2.bean.ActionBean
import com.angcyo.acc2.bean.FormBean
import com.angcyo.acc2.bean.FormResultBean
import com.angcyo.acc2.control.AccControl
import com.angcyo.acc2.control.log
import com.angcyo.acc2.parse.FormParse
import com.angcyo.http.GET
import com.angcyo.http.base.jsonObject
import com.angcyo.http.base.readString
import com.angcyo.http.post
import com.angcyo.http.rx.observer
import com.angcyo.http.toBean
import com.angcyo.library.ex.isDebugType
import com.angcyo.library.ex.toStr
import com.angcyo.library.ex.uuid
import java.util.concurrent.CountDownLatch

/**
 * 用于实现[FormBean]表单提交.
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/04/07
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class FormRequestListener : FormParse.RequestListener() {

    override fun initAction(actionBean: ActionBean): ActionBean {
        return actionBean.init()
    }

    /**发送表单请求*/
    override fun request(
        control: AccControl,
        formBean: FormBean,
        params: HashMap<String, Any?>?
    ): FormResultBean? {
        val taskBean = control._taskBean
        val formUrl = formBean.url

        if (taskBean == null) {
            control.log("taskBean is null")
            return null
        }
        if (formUrl.isNullOrEmpty()) {
            control.log("form url is null/empty.")
            return null
        }

        //debug
        if (formBean.debug && isDebugType()) {
            return null
        }

        var countDownLatch: CountDownLatch? = null
        if (formBean.sync) {
            countDownLatch = CountDownLatch(1)
        }

        val uuid = uuid()

        //result
        var formResultBean: FormResultBean? = null

        if (formBean.method == GET) {
            com.angcyo.http.get {
                url = formUrl

                //请求参数
                query = params ?: query

                control.log("请求表单的参数[$uuid] ${formUrl}↓\n${query}")
            }
        } else {
            post {
                url = formUrl

                //请求参数
                val requestParams = params ?: query

                control.log("请求表单的参数[$uuid] ${formUrl}↓\n${requestParams}")

                //请求体
                if (formBean.contentType == FormBean.CONTENT_TYPE_FORM) {
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
                val builder = StringBuilder()

                builder.appendLine("表单返回[$uuid] ${formBean}↓")

                data?.let {
                    builder.appendLine(it.body()?.toStr() ?: it.errorBody().readString())

                    formResultBean = it.toBean(FormResultBean::class.java)
                }

                error?.let {
                    //错误日志, 写入acc
                    builder.appendLine(it.message)
                    if (formBean.sync) {
                        control.error(it)
                    }
                }

                control.log(builder.toString())
                //L.d(data, error)

                //放行
                countDownLatch?.countDown()
            }
        }

        //同步时, 需要等待
        countDownLatch?.await()

        return formResultBean
    }
}