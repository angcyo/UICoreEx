package com.hjhrq1991.library.tbs

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Message
import android.view.KeyEvent
import com.hjhrq1991.library.tbs.BridgeUtil.webViewLoadLocalJs
import com.tencent.smtt.export.external.interfaces.*
import com.tencent.smtt.sdk.WebView
import com.tencent.smtt.sdk.WebViewClient
import java.io.UnsupportedEncodingException
import java.net.URLDecoder

/**
 * @author hjhrq1991 created at 8/22/16 14 41.
 */
class BridgeWebViewClient(val webView: TbsBridgeWebView) : WebViewClient() {

    /**
     * 是否重定向，避免web为渲染即跳转导致系统未调用onPageStarted就调用onPageFinished方法引起的js桥初始化失败
     */
    private var isRedirected = false

    /**
     * onPageStarted连续调用次数,避免渲染立马跳转可能连续调用onPageStarted多次并且调用shouldOverrideUrlLoading后不调用onPageStarted引起的js桥未初始化问题
     */
    private var onPageStartedCount = 0

    private var bridgeWebViewClientListener: WebViewClient? = null

    fun setBridgeWebViewClientListener(bridgeWebViewClientListener: WebViewClient?) {
        this.bridgeWebViewClientListener = bridgeWebViewClientListener
    }

    fun removeListener() {
        if (bridgeWebViewClientListener != null) {
            bridgeWebViewClientListener = null
        }
    }

    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        //modify：hjhrq1991，web为渲染即跳转导致系统未调用onPageStarted就调用onPageFinished方法引起的js桥初始化失败
        var url = url
        if (onPageStartedCount < 2) {
            isRedirected = true
        }
        onPageStartedCount = 0
        try {
            url = URLDecoder.decode(url, "UTF-8")
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }
        return if (url?.startsWith(BridgeUtil.YY_RETURN_DATA) == true) { // 如果是返回数据
            webView.handlerReturnData(url)
            true
        } else if (url?.startsWith(BridgeUtil.YY_OVERRIDE_SCHEMA) == true) { //
            webView.flushMessageQueue()
            true
        } else {
            bridgeWebViewClientListener?.shouldOverrideUrlLoading(view, url)
                ?: super.shouldOverrideUrlLoading(view, url)
        }
    }

    override fun onPageStarted(
        view: WebView?,
        url: String?,
        favicon: Bitmap?
    ) {
        super.onPageStarted(view, url, favicon)
        //modify：hjhrq1991，web为渲染即跳转导致系统未调用onPageStarted就调用onPageFinished方法引起的js桥初始化失败
        isRedirected = false
        onPageStartedCount++
        bridgeWebViewClientListener?.onPageStarted(view, url, favicon)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        if (view == null || url == null) {
            return
        }
        //modify：hjhrq1991，web为渲染即跳转导致系统未调用onPageStarted就调用onPageFinished方法引起的js桥初始化失败
        if (!url.contains("about:blank") && !isRedirected) {
            webViewLoadLocalJs(
                view,
                BridgeConfig.toLoadJs,
                BridgeConfig.defaultJs,
                BridgeConfig.customJs
            )
        }
        if (webView.startupMessage != null) {
            for (m in webView.startupMessage!!) {
                webView.dispatchMessage(m)
            }
            webView.startupMessage = null
        }
        bridgeWebViewClientListener?.onPageFinished(view, url)
    }

    override fun onReceivedError(
        view: WebView?,
        errorCode: Int,
        description: String?,
        failingUrl: String?
    ) {
        bridgeWebViewClientListener?.onReceivedError(view, errorCode, description, failingUrl)
    }

    override fun onLoadResource(webView: WebView, s: String) {
        bridgeWebViewClientListener?.onLoadResource(webView, s)
    }

    override fun onReceivedHttpError(
        webView: WebView,
        webResourceRequest: WebResourceRequest,
        webResourceResponse: WebResourceResponse
    ) {
        if (bridgeWebViewClientListener != null) {
            bridgeWebViewClientListener!!.onReceivedHttpError(
                webView,
                webResourceRequest,
                webResourceResponse
            )
        }
    }

    override fun shouldInterceptRequest(webView: WebView?, s: String?): WebResourceResponse? {
        return bridgeWebViewClientListener?.shouldInterceptRequest(webView, s)
            ?: super.shouldInterceptRequest(webView, s)
    }

    override fun shouldInterceptRequest(
        webView: WebView?,
        webResourceRequest: WebResourceRequest?
    ): WebResourceResponse? {
        return if (bridgeWebViewClientListener != null) {
            bridgeWebViewClientListener!!.shouldInterceptRequest(webView, webResourceRequest)
        } else {
            super.shouldInterceptRequest(webView, webResourceRequest)
        }
    }

    override fun shouldInterceptRequest(
        webView: WebView?,
        webResourceRequest: WebResourceRequest?,
        bundle: Bundle?
    ): WebResourceResponse? {
        return if (bridgeWebViewClientListener != null) {
            bridgeWebViewClientListener!!.shouldInterceptRequest(
                webView,
                webResourceRequest,
                bundle
            )
        } else {
            super.shouldInterceptRequest(webView, webResourceRequest, bundle)
        }
    }

    override fun doUpdateVisitedHistory(webView: WebView?, s: String?, b: Boolean) {
        if (bridgeWebViewClientListener != null) {
            bridgeWebViewClientListener!!.doUpdateVisitedHistory(webView, s, b)
        }
    }

    override fun onFormResubmission(webView: WebView?, message: Message?, message1: Message?) {
        bridgeWebViewClientListener?.onFormResubmission(webView, message, message1)
            ?: super.onFormResubmission(webView, message, message1)
    }

    override fun onReceivedHttpAuthRequest(
        webView: WebView?,
        httpAuthHandler: HttpAuthHandler?,
        s: String?,
        s1: String?
    ) {
        bridgeWebViewClientListener?.onReceivedHttpAuthRequest(
            webView,
            httpAuthHandler,
            s,
            s1
        ) ?: super.onReceivedHttpAuthRequest(webView, httpAuthHandler, s, s1)
    }

    override fun onReceivedSslError(
        webView: WebView?,
        sslErrorHandler: SslErrorHandler?,
        sslError: SslError?
    ) {
        bridgeWebViewClientListener?.onReceivedSslError(webView, sslErrorHandler, sslError)
            ?: super.onReceivedSslError(webView, sslErrorHandler, sslError)
    }

    override fun onReceivedClientCertRequest(
        webView: WebView,
        clientCertRequest: ClientCertRequest
    ) {
        bridgeWebViewClientListener?.onReceivedClientCertRequest(
            webView,
            clientCertRequest
        ) ?: super.onReceivedClientCertRequest(webView, clientCertRequest)
    }

    override fun onScaleChanged(webView: WebView, v: Float, v1: Float) {
        bridgeWebViewClientListener?.onScaleChanged(webView, v, v1)
    }

    override fun onUnhandledKeyEvent(webView: WebView, keyEvent: KeyEvent) {
        bridgeWebViewClientListener?.onUnhandledKeyEvent(webView, keyEvent)
    }

    override fun shouldOverrideKeyEvent(webView: WebView, keyEvent: KeyEvent): Boolean {
        return bridgeWebViewClientListener?.shouldOverrideKeyEvent(webView, keyEvent)
            ?: super.shouldOverrideKeyEvent(webView, keyEvent)
    }

    override fun onTooManyRedirects(webView: WebView, message: Message, message1: Message) {
        bridgeWebViewClientListener?.onTooManyRedirects(webView, message, message1)
    }

    override fun onReceivedLoginRequest(webView: WebView, s: String, s1: String, s2: String) {
        bridgeWebViewClientListener?.onReceivedLoginRequest(webView, s, s1, s2)
    }

    override fun onDetectedBlankScreen(s: String, i: Int) {
        bridgeWebViewClientListener?.onDetectedBlankScreen(s, i)
    }

}