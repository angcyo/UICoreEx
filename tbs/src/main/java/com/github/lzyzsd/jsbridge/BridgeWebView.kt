package com.github.lzyzsd.jsbridge

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Looper
import android.os.SystemClock
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.webkit.JavascriptInterface
import androidx.collection.ArrayMap
import com.angcyo.http.base.gson
import com.angcyo.tbs.BuildConfig
import com.github.lzyzsd.jsbridge.BridgeWebViewClient.OnLoadJSListener
import com.google.gson.Gson
import com.tencent.smtt.sdk.WebSettings
import com.tencent.smtt.sdk.WebView
import com.tencent.smtt.sdk.WebViewClient
import java.net.URLEncoder
import java.util.*

/**
 * 2020-08-08 1.0.4
 * https://github.com/lzyzsd/JsBridge
 */
@SuppressLint("SetJavaScriptEnabled")
open class BridgeWebView : WebView, WebViewJavascriptBridge, OnLoadJSListener {

    val URL_MAX_CHARACTER_NUM = 2097152
    val callbackMap: MutableMap<String, OnBridgeCallback> = ArrayMap()
    var messageList: MutableList<Any>? = ArrayList()
    var bridgeWebViewClient: BridgeWebViewClient? = null
    var uniqueId: Long = 0

    var isJSLoaded = false

    var gson: Gson? = gson()

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init()
    }

    constructor(context: Context?) : super(context) {
        init()
    }

    private fun init() {
        clearCache(true)
        settings.useWideViewPort = true
        //		webView.getSettings().setLoadWithOverviewMode(true);
        settings.cacheMode = WebSettings.LOAD_NO_CACHE
        settings.javaScriptEnabled = true
        //        mContent.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        settings.javaScriptCanOpenWindowsAutomatically = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && BuildConfig.DEBUG) {
            setWebContentsDebuggingEnabled(true)
        }
        bridgeWebViewClient = BridgeWebViewClient(this)
        super.setWebViewClient(bridgeWebViewClient)
    }

    override fun setWebViewClient(client: WebViewClient) {
        bridgeWebViewClient?.setWebViewClient(client)
    }

    override fun onLoadStart() {
        isJSLoaded = false
    }

    override fun onLoadFinished() {
        isJSLoaded = true
        if (messageList != null) {
            for (message in messageList!!) {
                dispatchMessage(message)
            }
            messageList = null
        }
    }

    override fun sendToWeb(data: Any?) {
        sendToWeb(data, null as OnBridgeCallback?)
    }

    override fun sendToWeb(data: Any?, responseCallback: OnBridgeCallback?) {
        doSend(null, data, responseCallback)
    }

    /**
     * call javascript registered handler
     * 调用javascript处理程序注册
     *
     * @param handlerName handlerName
     * @param data        data
     * @param callBack    OnBridgeCallback
     */
    fun callHandler(handlerName: String?, data: String?, callBack: OnBridgeCallback?) {
        doSend(handlerName, data, callBack)
    }

    override fun sendToWeb(function: String?, vararg values: Any?) {
        // 必须要找主线程才会将数据传递出去 --- 划重点
        if (Thread.currentThread() === Looper.getMainLooper().thread) {
            var jsCommand = String.format(function!!, *values)
            jsCommand = String.format(BridgeUtil.JAVASCRIPT_STR, jsCommand)
            loadUrl(jsCommand)
        }
    }

    /**
     * 保存message到消息队列
     *
     * @param handlerName      handlerName
     * @param data             data
     * @param responseCallback OnBridgeCallback
     */
    private fun doSend(handlerName: String?, data: Any?, responseCallback: OnBridgeCallback?) {
        if (data !is String && gson == null) {
            return
        }
        val request = JSRequest()
        if (data != null) {
            request.data = if (data is String) data else gson!!.toJson(data)
        }
        if (responseCallback != null) {
            val callbackId = String.format(
                BridgeUtil.CALLBACK_ID_FORMAT,
                (++uniqueId).toString() + (BridgeUtil.UNDERLINE_STR + SystemClock.currentThreadTimeMillis())
            )
            callbackMap[callbackId] = responseCallback
            request.callbackId = callbackId
        }
        if (!TextUtils.isEmpty(handlerName)) {
            request.handlerName = handlerName
        }
        queueMessage(request)
    }

    /**
     * list<message> != null 添加到消息集合否则分发消息
     *
     * @param message Message
    </message> */
    private fun queueMessage(message: Any) {
        if (messageList != null) {
            messageList!!.add(message)
        } else {
            dispatchMessage(message)
        }
    }

    /**
     * 分发message 必须在主线程才分发成功
     *
     * @param message Message
     */
    private fun dispatchMessage(message: Any) {
        if (gson == null) {
            return
        }
        var messageJson = gson!!.toJson(message)
        //escape special characters for json string  为json字符串转义特殊字符
        messageJson = messageJson.replace("(\\\\)([^utrn])".toRegex(), "\\\\\\\\$1$2")
        messageJson = messageJson.replace("(?<=[^\\\\])(\")".toRegex(), "\\\\\"")
        messageJson = messageJson.replace("(?<=[^\\\\])(\')".toRegex(), "\\\\\'")
        messageJson = messageJson.replace("%7B".toRegex(), URLEncoder.encode("%7B"))
        messageJson = messageJson.replace("%7D".toRegex(), URLEncoder.encode("%7D"))
        messageJson = messageJson.replace("%22".toRegex(), URLEncoder.encode("%22"))
        val javascriptCommand = String.format(BridgeUtil.JS_HANDLE_MESSAGE_FROM_JAVA, messageJson)
        // 必须要找主线程才会将数据传递出去 --- 划重点
        if (Thread.currentThread() === Looper.getMainLooper().thread) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && javascriptCommand.length >= URL_MAX_CHARACTER_NUM) {
                evaluateJavascript(javascriptCommand, null)
            } else {
                this.loadUrl(javascriptCommand)
            }
        }
    }

    fun sendResponse(data: Any?, callbackId: String?) {
        if (data !is String && gson == null) {
            return
        }
        if (!TextUtils.isEmpty(callbackId)) {
            val response = JSResponse()
            response.responseId = callbackId
            response.responseData = if (data is String) data else gson!!.toJson(data)
            if (Thread.currentThread() === Looper.getMainLooper().thread) {
                dispatchMessage(response)
            } else {
                post { dispatchMessage(response) }
            }
        }
    }

    override fun destroy() {
        super.destroy()
        callbackMap.clear()
    }

    abstract class BaseJavascriptInterface(val mCallbacks: MutableMap<String, OnBridgeCallback>) {

        @JavascriptInterface
        fun send(data: String, callbackId: String): String {
            Log.d("chromium", "$data, callbackId: $callbackId " + Thread.currentThread().name)
            return send(data)
        }

        @JavascriptInterface
        fun response(data: String, responseId: String) {
            Log.d("chromium", "$data, responseId: $responseId " + Thread.currentThread().name)
            if (!TextUtils.isEmpty(responseId)) {
                val function = mCallbacks.remove(responseId)
                function?.onCallBack(data)
            }
        }

        abstract fun send(data: String?): String
    }
}