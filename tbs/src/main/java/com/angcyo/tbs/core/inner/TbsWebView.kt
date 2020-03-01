package com.angcyo.tbs.core.inner

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.View
import com.angcyo.library.L
import com.angcyo.library.utils.getMember
import com.tencent.smtt.export.external.interfaces.IX5WebViewBase
import com.tencent.smtt.export.external.interfaces.WebResourceRequest
import com.tencent.smtt.sdk.WebChromeClient
import com.tencent.smtt.sdk.WebView
import com.tencent.smtt.sdk.WebViewClient


/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/03/01
 */
open class TbsWebView(context: Context, attributeSet: AttributeSet? = null) :
    WebView(context, attributeSet) {

    //<editor-fold desc="回调">

    var onProgressChanged: (url: String?, progress: Int) -> Unit = { _, _ ->

    }

    var onReceivedTitle: (title: String?) -> Unit = {}


    //</editor-fold desc="回调">


    //<editor-fold desc="WebViewClient">

    var _loadUrl: String? = null

    val webClient: WebViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(webView: WebView, url: String?): Boolean {
            _loadUrl = url
            L.d("${webView.title}->$url")
            return shouldOverrideUrlLoading(this, webView, url)
        }

        override fun shouldOverrideUrlLoading(
            webView: WebView,
            requeset: WebResourceRequest?
        ): Boolean {
            L.d("${webView.title} $requeset")
            return super.shouldOverrideUrlLoading(webView, requeset)
        }

        override fun onPageStarted(webView: WebView, url: String?, bitmap: Bitmap?) {
            super.onPageStarted(webView, url, bitmap)
            onProgressChanged(url, 0)
        }

        override fun onPageFinished(webView: WebView, url: String?) {
            super.onPageFinished(webView, url)
            onProgressChanged(url, 100)
        }
    }

    //</editor-fold desc="WebViewClient">

    //<editor-fold desc="WebChromeClient">

    val chromeClient: WebChromeClient = object : WebChromeClient() {
        override fun onReceivedTitle(webView: WebView, title: String?) {
            super.onReceivedTitle(webView, title)
            this@TbsWebView.onReceivedTitle(title)
        }

        override fun onProgressChanged(webView: WebView, progress: Int) {
            super.onProgressChanged(webView, progress)
            onProgressChanged(_loadUrl, progress)
        }
    }

    //</editor-fold desc="WebChromeClient">

    init {
        webViewClient = webClient
        webChromeClient = chromeClient

        TbsWeb.initWebView(this)

        this.view.isClickable = true

        resetOverScrollMode()
    }

    /**加载url*/
    open fun shouldOverrideUrlLoading(
        webClient: WebViewClient,
        webView: WebView,
        url: String?
    ): Boolean {
        webView.loadUrl(url)
        return true
    }

    /**去掉[OVER_SCROLL]效果*/
    fun resetOverScrollMode() {
        view.overScrollMode = View.OVER_SCROLL_NEVER
        val f: Any? = getMember(WebView::class.java, "f")
        val g: Any? = getMember(WebView::class.java, "g")
        if (f is IX5WebViewBase) {
            try {
                f.view.overScrollMode = View.OVER_SCROLL_NEVER
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (g is View) {
            g.overScrollMode = View.OVER_SCROLL_NEVER
        }
    }
}