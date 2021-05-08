package com.angcyo.acc2.app.component

import com.angcyo.acc2.bean.ActionBean
import com.angcyo.acc2.bean.FormBean
import com.angcyo.acc2.bean.FormBean.Companion.KEY_CODE
import com.angcyo.acc2.bean.FormBean.Companion.KEY_DATA
import com.angcyo.acc2.bean.FormBean.Companion.KEY_MSG
import com.angcyo.acc2.bean.HandleBean
import com.angcyo.acc2.bean.handleParams
import com.angcyo.acc2.control.*
import com.angcyo.acc2.parse.HandleResult
import com.angcyo.http.GET
import com.angcyo.http.base.jsonObject
import com.angcyo.http.base.readString
import com.angcyo.http.post
import com.angcyo.http.rx.observer
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
class FormControlListener : ControlListener() {

    /**额外需要添加的参数回调*/
    var configParams: (formBean: FormBean, params: HashMap<String, Any?>) -> Unit = { _, _ -> }

    override fun onControlStateChanged(control: AccControl, oldState: Int, newState: Int) {
        super.onControlStateChanged(control, oldState, newState)

        control._taskBean?.form?.let {
            if (newState >= AccControl.CONTROL_STATE_FINISH) {
                //结束之后, 才请求form
                val params = hashMapOf<String, Any?>()
                when (newState) {
                    AccControl.CONTROL_STATE_FINISH -> params[KEY_CODE] = 200
                    AccControl.CONTROL_STATE_STOP -> params[KEY_CODE] = 300 //本地执行中断, 任务终止.
                    AccControl.CONTROL_STATE_ERROR -> params[KEY_CODE] = 500 //本地执行错误, 任务终止.
                }
                params[KEY_MSG] = control.finishReason
                params[KEY_DATA] = newState

                request(control, it, params)
            }
        }
    }

    override fun onHandleAction(
        controlContext: ControlContext,
        control: AccControl,
        handleBean: HandleBean,
        handleResult: HandleResult
    ) {
        super.onHandleAction(controlContext, control, handleBean, handleResult)
        handleBean.operate?.form?.let {
            handleForm(control, it, controlContext.action, handleResult)
        }
    }

    override fun onActionRunAfter(
        control: AccControl,
        actionBean: ActionBean,
        isPrimaryAction: Boolean,
        handleResult: HandleResult
    ) {
        super.onActionRunAfter(control, actionBean, isPrimaryAction, handleResult)
        actionBean.form?.let {
            handleForm(control, it, actionBean, handleResult)
        }
    }

    fun handleForm(
        control: AccControl,
        formBean: FormBean,
        actionBean: ActionBean?,
        handleResult: HandleResult
    ) {
        val params = hashMapOf<String, Any?>()
        if (handleResult.success || handleResult.forceSuccess) {
            params[KEY_CODE] = 200
        } else {
            params[KEY_CODE] = 301 //本地执行中断, 但是需要继续任务
        }
        params[KEY_MSG] = actionBean?.title
        params[KEY_DATA] = actionBean?.actionLog()

        if (formBean.checkSuccess) {
            if (handleResult.success || handleResult.forceSuccess) {
                request(control, formBean, params)
            }
        } else {
            request(control, formBean, params)
        }
    }

    /**发送表单请求*/
    fun request(control: AccControl, formBean: FormBean, params: HashMap<String, Any?>? = null) {
        val taskBean = control._taskBean
        val formUrl = formBean.url

        if (taskBean == null) {
            control.log("taskBean is null")
            return
        }
        if (formUrl.isNullOrEmpty()) {
            control.log("form url is null/empty.")
            return
        }

        //debug
        if (formBean.debug && isDebugType()) {
            return
        }

        var countDownLatch: CountDownLatch? = null
        if (formBean.sync) {
            countDownLatch = CountDownLatch(1)
        }

        val uuid = uuid()

        if (formBean.method == GET) {
            com.angcyo.http.get {
                url = formUrl

                //请求参数
                query = formBean.handleParams(control, taskBean, configParams).apply {
                    params?.let { putAll(it) }
                }

                control.log("请求表单的参数[$uuid] ${formUrl}↓\n${query}")
            }
        } else {
            post {
                url = formUrl

                //请求参数
                val requestParams = formBean.handleParams(control, taskBean, configParams).apply {
                    params?.let { putAll(it) }
                }

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