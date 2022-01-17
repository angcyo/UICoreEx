package com.hjhrq1991.library.tbs

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Looper
import android.os.SystemClock
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import com.hjhrq1991.library.tbs.BridgeUtil.getDataFromReturnUrl
import com.hjhrq1991.library.tbs.BridgeUtil.getFunctionFromReturnUrl
import com.hjhrq1991.library.tbs.BridgeUtil.parseFunctionName
import com.hjhrq1991.library.tbs.Message.Companion.toArrayList
import com.tencent.smtt.export.external.interfaces.*
import com.tencent.smtt.sdk.*
import java.util.*


/**
 * 2020-08-08 1.0.7
 * https://github.com/hjhrq1991/JsBridge*/
@SuppressLint("SetJavaScriptEnabled")
open class TbsBridgeWebView : WebView, WebViewJavascriptBridge {

    private var bridgeWebViewClient: BridgeWebViewClient? = null
    var responseCallbacks: MutableMap<String, CallBackFunction> = HashMap()
    var messageHandlers: MutableMap<String, BridgeHandler> = HashMap()
    var defaultHandler: BridgeHandler = DefaultHandler()
    var startupMessage: MutableList<Message>? = ArrayList()

    private var uniqueId: Long = 0

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
        this.isVerticalScrollBarEnabled = false
        this.isHorizontalScrollBarEnabled = false
        this.settings.javaScriptEnabled = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            setWebContentsDebuggingEnabled(true)
        }

        super.setWebViewClient(generateBridgeWebViewClient())
    }

    /**拦截2020-08-08*/
    override fun setWebViewClient(webViewClient: WebViewClient?) {
        bridgeWebViewClient?.setBridgeWebViewClientListener(webViewClient)
            ?: super.setWebViewClient(webViewClient)
    }

    fun generateBridgeWebViewClient(): BridgeWebViewClient {
        return BridgeWebViewClient(this).also { bridgeWebViewClient = it }
    }

    fun handlerReturnData(url: String?) {
        val functionName = getFunctionFromReturnUrl(url!!)
        val f = responseCallbacks[functionName]
        val data = getDataFromReturnUrl(url)
        if (f != null) {
            f.onCallBack(data)
            responseCallbacks.remove(functionName)
            return
        }
    }

    override fun send(data: String?) {
        send(data, null)
    }

    override fun send(data: String?, responseCallback: CallBackFunction?) {
        doSend(null, data, responseCallback)
    }

    private fun doSend(handlerName: String?, data: String?, responseCallback: CallBackFunction?) {
        val m = Message()
        if (!TextUtils.isEmpty(data)) {
            m.data = data
        }
        if (responseCallback != null) {
            val callbackStr = String.format(
                BridgeUtil.CALLBACK_ID_FORMAT,
                (++uniqueId).toString() + (BridgeUtil.UNDERLINE_STR + SystemClock.currentThreadTimeMillis())
            )
            responseCallbacks[callbackStr] = responseCallback
            m.callbackId = callbackStr
        }
        if (!TextUtils.isEmpty(handlerName)) {
            m.handlerName = handlerName
        }
        queueMessage(m)
    }

    private fun queueMessage(m: Message) {
        if (startupMessage != null) {
            startupMessage!!.add(m)
        } else {
            dispatchMessage(m)
        }
    }

    fun dispatchMessage(m: Message) {
        var messageJson = m.toJson()
        //escape special characters for json string
        messageJson = messageJson!!.replace("(\\\\)([^utrn])".toRegex(), "\\\\\\\\$1$2")
        messageJson = messageJson.replace("(?<=[^\\\\])(\")".toRegex(), "\\\\\"")
        val javascriptCommand = String.format(
            BridgeUtil.JS_HANDLE_MESSAGE_FROM_JAVA.replace(
                BridgeConfig.defaultJs,
                BridgeConfig.customJs
            ), messageJson
        )
        if (Thread.currentThread() === Looper.getMainLooper().thread) {
            this.loadUrl(javascriptCommand)
        }
    }

    fun flushMessageQueue() {
        if (Thread.currentThread() === Looper.getMainLooper().thread) {
            loadUrl(
                BridgeUtil.JS_FETCH_QUEUE_FROM_JAVA.replace(
                    BridgeConfig.defaultJs,
                    BridgeConfig.customJs
                ), object : CallBackFunction {
                    override fun onCallBack(data: String?) {
                        // deserializeMessage
                        var list: List<Message?>? = null
                        list = try {
                            toArrayList(data)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            return
                        }
                        if (list.isNullOrEmpty()) {
                            return
                        }
                        for (i in list.indices) {
                            val m: Message? = list[i]
                            val responseId = m?.responseId
                            // 是否是response
                            if (!TextUtils.isEmpty(responseId)) {
                                val function =
                                    responseCallbacks[responseId]
                                val responseData = m?.responseData
                                function!!.onCallBack(responseData)
                                responseCallbacks.remove(responseId)
                            } else {
                                var responseFunction: CallBackFunction? =
                                    null
                                // if had callbackId
                                val callbackId = m?.callbackId
                                if (!TextUtils.isEmpty(callbackId)) {
                                    responseFunction =
                                        object : CallBackFunction {
                                            override fun onCallBack(data: String?) {
                                                val responseMsg =
                                                    Message()
                                                responseMsg.responseId = callbackId
                                                responseMsg.responseData = data
                                                queueMessage(responseMsg)
                                            }
                                        }
                                } else {
                                    responseFunction =
                                        object : CallBackFunction {
                                            override fun onCallBack(data: String?) {
                                                // do nothing
                                            }
                                        }
                                }
                                val handler: BridgeHandler? =
                                    if (!TextUtils.isEmpty(m?.handlerName)) {
                                        messageHandlers[m?.handlerName]
                                    } else {
                                        defaultHandler
                                    }
                                handler?.handler(m?.data, responseFunction)
                            }
                        }
                    }
                })
        }
    }

    fun loadUrl(jsUrl: String?, returnCallback: CallBackFunction) {
        this.loadUrl(jsUrl)
        responseCallbacks[parseFunctionName(jsUrl!!, BridgeConfig.customJs)] = returnCallback
    }

    /**
     * register handler,so that javascript can call it
     *
     * @param handlerName handlerName
     * @param handler     Handler
     */
    fun registerHandler(handlerName: String, handler: BridgeHandler?) {
        if (handler != null) {
            messageHandlers[handlerName] = handler
        }
    }

    fun registerHandler(
        handlerName: String,
        handler: (data: String?, function: CallBackFunction?) -> Unit
    ) {
        registerHandler(handlerName, object : BridgeHandler {
            override fun handler(data: String?, function: CallBackFunction?) {
                handler.invoke(data, function)
            }
        })
    }

    /**
     * call javascript registered handler
     *
     * @param handlerName handlerName
     * @param data        data
     * @param callBack    callBack
     */
    fun callHandler(handlerName: String?, data: String?, callBack: CallBackFunction?) {
        doSend(handlerName, data, callBack)
    }

    /**
     * 销毁时调用，移除listener
     */
    fun removeListener() {
        if (bridgeWebViewClient != null) {
            bridgeWebViewClient!!.removeListener()
        }
        if (onWebChromeClientListener != null) {
            onWebChromeClientListener = null
        }
    }

    private var onWebChromeClientListener: OnWebChromeClientListener? = null

    fun setWebChromeClientListener(onWebChromeClientListener: OnWebChromeClientListener?) {
        this.onWebChromeClientListener = onWebChromeClientListener
        webChromeClient = newWebChromeClient()
    }

    fun setWebChromeClientListener(webChromeClientListener: WebChromeClientListener?) {
        onWebChromeClientListener = webChromeClientListener
        webChromeClient = newWebChromeClient()
    }

    private fun newWebChromeClient(): WebChromeClient {
        return object : WebChromeClient() {
            override fun onExceededDatabaseQuota(
                s: String,
                s1: String,
                l: Long,
                l1: Long,
                l2: Long,
                quotaUpdater: WebStorage.QuotaUpdater
            ) {
                if (onWebChromeClientListener != null) {
                    onWebChromeClientListener!!.onExceededDatabaseQuota(
                        s,
                        s1,
                        l,
                        l1,
                        l2,
                        quotaUpdater
                    )
                } else {
                    super.onExceededDatabaseQuota(s, s1, l, l1, l2, quotaUpdater)
                }
            }

            override fun getDefaultVideoPoster(): Bitmap {
                return if (onWebChromeClientListener != null) onWebChromeClientListener!!.defaultVideoPoster!! else super.getDefaultVideoPoster()
            }

            override fun getVisitedHistory(valueCallback: ValueCallback<Array<String?>?>?) {
                if (onWebChromeClientListener != null) {
                    onWebChromeClientListener!!.getVisitedHistory(valueCallback)
                } else {
                    super.getVisitedHistory(valueCallback)
                }
            }

            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                return if (onWebChromeClientListener != null) onWebChromeClientListener!!.onConsoleMessage(
                    consoleMessage
                ) else super.onConsoleMessage(consoleMessage)
            }

            override fun onCreateWindow(
                webView: WebView,
                b: Boolean,
                b1: Boolean,
                message: android.os.Message
            ): Boolean {
                return if (onWebChromeClientListener != null) onWebChromeClientListener!!.onCreateWindow(
                    webView,
                    b,
                    b1,
                    message
                ) else super.onCreateWindow(webView, b, b1, message)
            }

            override fun onGeolocationPermissionsHidePrompt() {
                if (onWebChromeClientListener != null) {
                    onWebChromeClientListener!!.onGeolocationPermissionsHidePrompt()
                } else {
                    super.onGeolocationPermissionsHidePrompt()
                }
            }

            override fun onGeolocationPermissionsShowPrompt(
                s: String,
                geolocationPermissionsCallback: GeolocationPermissionsCallback
            ) {
                if (onWebChromeClientListener != null) {
                    onWebChromeClientListener!!.onGeolocationPermissionsShowPrompt(
                        s,
                        geolocationPermissionsCallback
                    )
                } else {
                    super.onGeolocationPermissionsShowPrompt(s, geolocationPermissionsCallback)
                }
            }

            override fun onHideCustomView() {
                if (onWebChromeClientListener != null) {
                    onWebChromeClientListener!!.onHideCustomView()
                } else {
                    super.onHideCustomView()
                }
            }

            override fun onJsAlert(
                webView: WebView,
                s: String,
                s1: String,
                jsResult: JsResult
            ): Boolean {
                return if (onWebChromeClientListener != null) onWebChromeClientListener!!.onJsAlert(
                    webView,
                    s,
                    s1,
                    jsResult
                ) else super.onJsAlert(webView, s, s1, jsResult)
            }

            override fun onJsConfirm(
                webView: WebView,
                s: String,
                s1: String,
                jsResult: JsResult
            ): Boolean {
                return if (onWebChromeClientListener != null) onWebChromeClientListener!!.onJsConfirm(
                    webView,
                    s,
                    s1,
                    jsResult
                ) else super.onJsConfirm(webView, s, s1, jsResult)
            }

            override fun onJsPrompt(
                webView: WebView,
                s: String,
                s1: String,
                s2: String,
                jsPromptResult: JsPromptResult
            ): Boolean {
                return if (onWebChromeClientListener != null) onWebChromeClientListener!!.onJsPrompt(
                    webView,
                    s,
                    s1,
                    s2,
                    jsPromptResult
                ) else super.onJsPrompt(webView, s, s1, s2, jsPromptResult)
            }

            override fun onJsBeforeUnload(
                webView: WebView,
                s: String,
                s1: String,
                jsResult: JsResult
            ): Boolean {
                return if (onWebChromeClientListener != null) onWebChromeClientListener!!.onJsBeforeUnload(
                    webView,
                    s,
                    s1,
                    jsResult
                ) else super.onJsBeforeUnload(webView, s, s1, jsResult)
            }

            override fun onJsTimeout(): Boolean {
                return if (onWebChromeClientListener != null) onWebChromeClientListener!!.onJsTimeout() else super.onJsTimeout()
            }

            override fun onProgressChanged(
                webView: WebView,
                i: Int
            ) {
                if (onWebChromeClientListener != null) {
                    onWebChromeClientListener!!.onProgressChanged(webView, i)
                } else {
                    super.onProgressChanged(webView, i)
                }
            }

            override fun onReachedMaxAppCacheSize(
                l: Long,
                l1: Long,
                quotaUpdater: WebStorage.QuotaUpdater
            ) {
                if (onWebChromeClientListener != null) {
                    onWebChromeClientListener!!.onReachedMaxAppCacheSize(l, l1, quotaUpdater)
                } else {
                    super.onReachedMaxAppCacheSize(l, l1, quotaUpdater)
                }
            }

            override fun onReceivedIcon(
                webView: WebView,
                bitmap: Bitmap
            ) {
                if (onWebChromeClientListener != null) {
                    onWebChromeClientListener!!.onReceivedIcon(webView, bitmap)
                } else {
                    super.onReceivedIcon(webView, bitmap)
                }
            }

            override fun onReceivedTouchIconUrl(
                webView: WebView,
                s: String,
                b: Boolean
            ) {
                if (onWebChromeClientListener != null) {
                    onWebChromeClientListener!!.onReceivedTouchIconUrl(webView, s, b)
                } else {
                    super.onReceivedTouchIconUrl(webView, s, b)
                }
            }

            override fun onReceivedTitle(
                webView: WebView,
                s: String
            ) {
                if (onWebChromeClientListener != null) {
                    onWebChromeClientListener!!.onReceivedTitle(webView, s)
                } else {
                    super.onReceivedTitle(webView, s)
                }
            }

            override fun onRequestFocus(webView: WebView) {
                if (onWebChromeClientListener != null) {
                    onWebChromeClientListener!!.onRequestFocus(webView)
                } else {
                    super.onRequestFocus(webView)
                }
            }

            override fun onShowCustomView(
                view: View,
                customViewCallback: IX5WebChromeClient.CustomViewCallback
            ) {
                if (onWebChromeClientListener != null) {
                    onWebChromeClientListener!!.onShowCustomView(view, customViewCallback)
                } else {
                    super.onShowCustomView(view, customViewCallback)
                }
            }

            override fun onShowCustomView(
                view: View,
                i: Int,
                customViewCallback: IX5WebChromeClient.CustomViewCallback
            ) {
                if (onWebChromeClientListener != null) {
                    onWebChromeClientListener!!.onShowCustomView(view, i, customViewCallback)
                } else {
                    super.onShowCustomView(view, i, customViewCallback)
                }
            }

            override fun onCloseWindow(webView: WebView) {
                if (onWebChromeClientListener != null) {
                    onWebChromeClientListener!!.onCloseWindow(webView)
                } else {
                    super.onCloseWindow(webView)
                }
            }

            override fun getVideoLoadingProgressView(): View {
                return if (onWebChromeClientListener != null) onWebChromeClientListener!!.videoLoadingProgressView!! else super.getVideoLoadingProgressView()
            }

            override fun openFileChooser(
                valueCallback: ValueCallback<Uri?>?,
                s: String,
                s1: String
            ) {
                if (onWebChromeClientListener != null) {
                    onWebChromeClientListener!!.openFileChooser(valueCallback, s, s1)
                } else {
                    super.openFileChooser(valueCallback, s, s1)
                }
            }

            override fun onShowFileChooser(
                webView: WebView,
                valueCallback: ValueCallback<Array<Uri?>?>?,
                fileChooserParams: FileChooserParams
            ): Boolean {
                return if (onWebChromeClientListener != null) onWebChromeClientListener!!.onShowFileChooser(
                    webView,
                    valueCallback,
                    fileChooserParams
                ) else super.onShowFileChooser(webView, valueCallback, fileChooserParams)
            }
        }
    }

    /**
     * @param customJs 自定义桥名，可为空，为空时使用默认桥名
     * 自定义桥名回调，如用自定义桥名，请copy一份WebViewJavascriptBridge.js替换文件名
     * 及脚本内所有包含"WebViewJavascriptBridge"的内容为你的自定义桥名
     * @author hjhrq1991 created at 6/20/16 17:32.
     */
    fun setCustom(customJs: String?) {
        BridgeConfig.customJs =
            if (!TextUtils.isEmpty(customJs)) customJs!! else BridgeConfig.defaultJs
    }

    /**
     * [function] 用于发送 response
     * */
    fun defaultHandle(handler: (data: String?, function: CallBackFunction?) -> Unit) {
        defaultHandler = object : BridgeHandler {
            override fun handler(data: String?, function: CallBackFunction?) {
                handler(data, function)
            }
        }
    }
}