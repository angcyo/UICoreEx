package com.github.lzyzsd.jsbridge

import android.graphics.Bitmap
import android.os.Build
import android.os.Message
import android.view.KeyEvent
import com.tencent.smtt.export.external.interfaces.*
import com.tencent.smtt.sdk.WebView
import com.tencent.smtt.sdk.WebViewClient

/**
 * 如果要自定义WebViewClient必须要集成此类
 * Created by bruce on 10/28/15.
 */
class BridgeWebViewClient(val mListener: OnLoadJSListener) : WebViewClient() {

    private var mClient: WebViewClient? = null

    fun setWebViewClient(client: WebViewClient?) {
        mClient = client
    }

    override fun shouldOverrideUrlLoading(
        view: WebView,
        url: String
    ): Boolean {
        view.loadUrl(url)
        return if (mClient != null) {
            mClient!!.shouldOverrideUrlLoading(view, url)
        } else super.shouldOverrideUrlLoading(view, url)
    }

    override fun shouldOverrideUrlLoading(
        view: WebView,
        request: WebResourceRequest
    ): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            view.loadUrl(request.url.authority)
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && mClient != null) {
            mClient!!.shouldOverrideUrlLoading(view, request)
        } else super.shouldOverrideUrlLoading(
            view,
            request
        )
    }

    override fun onPageStarted(
        view: WebView,
        url: String,
        favicon: Bitmap
    ) {
        if (mClient != null) {
            mClient!!.onPageStarted(view, url, favicon)
        } else {
            super.onPageStarted(view, url, favicon)
        }
    }

    override fun onPageFinished(
        view: WebView,
        url: String
    ) {
        if (mClient != null) {
            mClient!!.onPageFinished(view, url)
        } else {
            super.onPageFinished(view, url)
        }
        mListener.onLoadStart()
        BridgeUtil.webViewLoadLocalJs(view, BridgeUtil.JAVA_SCRIPT)
        mListener.onLoadFinished()
    }

    override fun onLoadResource(
        view: WebView,
        url: String
    ) {
        if (mClient != null) {
            mClient!!.onLoadResource(view, url)
        } else {
            super.onLoadResource(view, url)
        }
    }

    override fun onPageCommitVisible(
        view: WebView,
        url: String
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mClient != null) {
            mClient!!.onPageCommitVisible(view, url)
        } else {
            super.onPageCommitVisible(view, url)
        }
    }

    override fun shouldInterceptRequest(
        view: WebView,
        url: String
    ): WebResourceResponse? {
        return if (mClient != null) {
            mClient!!.shouldInterceptRequest(view, url)
        } else super.shouldInterceptRequest(view, url)
    }

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mClient != null) {
            mClient!!.shouldInterceptRequest(view, request)
        } else super.shouldInterceptRequest(
            view,
            request
        )
    }

    override fun onTooManyRedirects(
        view: WebView,
        cancelMsg: Message,
        continueMsg: Message
    ) {
        if (mClient != null) {
            mClient!!.onTooManyRedirects(view, cancelMsg, continueMsg)
        } else {
            super.onTooManyRedirects(view, cancelMsg, continueMsg)
        }
    }

    override fun onReceivedError(
        view: WebView,
        errorCode: Int,
        description: String,
        failingUrl: String
    ) {
        if (mClient != null) {
            mClient!!.onReceivedError(view, errorCode, description, failingUrl)
        } else {
            super.onReceivedError(view, errorCode, description, failingUrl)
        }
    }

    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: WebResourceError
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mClient != null) {
            mClient!!.onReceivedError(view, request, error)
        } else {
            super.onReceivedError(view, request, error)
        }
    }

    override fun onReceivedHttpError(
        view: WebView,
        request: WebResourceRequest,
        errorResponse: WebResourceResponse
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mClient != null) {
            mClient!!.onReceivedHttpError(view, request, errorResponse)
        } else {
            super.onReceivedHttpError(view, request, errorResponse)
        }
    }

    override fun onFormResubmission(
        view: WebView,
        dontResend: Message,
        resend: Message
    ) {
        if (mClient != null) {
            mClient!!.onFormResubmission(view, dontResend, resend)
        } else {
            super.onFormResubmission(view, dontResend, resend)
        }
    }

    override fun doUpdateVisitedHistory(
        view: WebView,
        url: String,
        isReload: Boolean
    ) {
        if (mClient != null) {
            mClient!!.doUpdateVisitedHistory(view, url, isReload)
        } else {
            super.doUpdateVisitedHistory(view, url, isReload)
        }
    }

    override fun onReceivedSslError(
        view: WebView,
        handler: SslErrorHandler,
        error: SslError
    ) {
        if (mClient != null) {
            mClient!!.onReceivedSslError(view, handler, error)
        } else {
            super.onReceivedSslError(view, handler, error)
        }
    }

    override fun onReceivedClientCertRequest(
        view: WebView,
        request: ClientCertRequest
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mClient != null) {
            mClient!!.onReceivedClientCertRequest(view, request)
        } else {
            super.onReceivedClientCertRequest(view, request)
        }
    }

    override fun onReceivedHttpAuthRequest(
        view: WebView,
        handler: HttpAuthHandler,
        host: String,
        realm: String
    ) {
        if (mClient != null) {
            mClient!!.onReceivedHttpAuthRequest(view, handler, host, realm)
        } else {
            super.onReceivedHttpAuthRequest(view, handler, host, realm)
        }
    }

    override fun shouldOverrideKeyEvent(
        view: WebView,
        event: KeyEvent
    ): Boolean {
        return if (mClient != null) {
            mClient!!.shouldOverrideKeyEvent(view, event)
        } else super.shouldOverrideKeyEvent(view, event)
    }

    override fun onUnhandledKeyEvent(
        view: WebView,
        event: KeyEvent
    ) {
        if (mClient != null) {
            mClient!!.onUnhandledKeyEvent(view, event)
        } else {
            super.onUnhandledKeyEvent(view, event)
        }
    }

    override fun onScaleChanged(
        view: WebView,
        oldScale: Float,
        newScale: Float
    ) {
        if (mClient != null) {
            mClient!!.onScaleChanged(view, oldScale, newScale)
        } else {
            super.onScaleChanged(view, oldScale, newScale)
        }
    }

    override fun onReceivedLoginRequest(
        view: WebView,
        realm: String,
        account: String?,
        args: String
    ) {
        if (mClient != null) {
            mClient!!.onReceivedLoginRequest(view, realm, account, args)
        } else {
            super.onReceivedLoginRequest(view, realm, account, args)
        }
    }

//    fun onRenderProcessGone(
//        view: WebView?,
//        detail: RenderProcessGoneDetail?
//    ): Boolean {
//        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && mClient != null) {
//            mClient.onRenderProcessGone(view, detail)
//        } else super.onRenderProcessGone(view, detail)
//    }
//
//    fun onSafeBrowsingHit(
//        view: WebView?,
//        request: WebResourceRequest?,
//        threatType: Int,
//        callback: SafeBrowsingResponse?
//    ) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 && mClient != null) {
//            mClient.onSafeBrowsingHit(view, request, threatType, callback)
//        } else {
//            super.onSafeBrowsingHit(view, request, threatType, callback)
//        }
//    }

    interface OnLoadJSListener {
        fun onLoadStart()
        fun onLoadFinished()
    }

}