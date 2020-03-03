package com.angcyo.tbs.core.inner

import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import com.angcyo.library.L
import com.angcyo.library.component.appBean
import com.angcyo.library.component.dslIntentQuery
import com.angcyo.library.ex.fileSizeString
import com.angcyo.library.model.AppBean
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

    /**网页加载进度回调*/
    var onProgressChanged: (url: String?, progress: Int) -> Unit = { _, _ ->

    }

    /**标题接收回调*/
    var onReceivedTitle: (title: String?) -> Unit = {}

    /**打开应用回调*/
    var onOpenAppListener: (activityInfo: ActivityInfo, appBean: AppBean) -> Unit =
        { activityInfo, appBean -> L.d("打开应用:${appBean.appName} ${activityInfo.name}") }

    /**下载文件回调*/
    var onDownloadListener: (
        url: String /*下载地址*/,
        userAgent: String,
        contentDisposition: String,
        mime: String /*文件mime application/zip*/,
        length: Long /*文件大小 b*/
    ) -> Unit =
        { url, userAgent, contentDisposition, mime, length ->
            L.d(
                "下载:${TbsWeb.getFileName(
                    url,
                    contentDisposition
                )} ${length.fileSizeString()}\n$url $mime\n$userAgent $contentDisposition "
            )
        }

    //</editor-fold desc="回调">

    //<editor-fold desc="WebViewClient">

    var _loadUrl: String? = null

    val webClient: WebViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(webView: WebView, url: String?): Boolean {
            L.d("${webView.originalUrl} ${webView.url} ${webView.title} $url")
            _loadUrl = url
            return onShouldOverrideUrlLoading(this, webView, url)
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
            L.d("${webView.originalUrl} ${webView.url} $title")
            this@TbsWebView.onReceivedTitle(title)
        }

        override fun onProgressChanged(webView: WebView, progress: Int) {
            super.onProgressChanged(webView, progress)
            //L.d("${webView.originalUrl} ${webView.url} $progress")
            onProgressChanged(webView.url, progress)
        }
    }

    //</editor-fold desc="WebChromeClient">

    init {
        webViewClient = webClient
        webChromeClient = chromeClient

        TbsWeb.initWebView(this)

        this.view.isClickable = true

        resetOverScrollMode()

        //下载
        setDownloadListener { url, userAgent, contentDisposition, mime, length ->
            onDownloadListener(url, userAgent, contentDisposition, mime, length)
        }
    }

    /**加载url*/
    open fun onShouldOverrideUrlLoading(
        webClient: WebViewClient,
        webView: WebView,
        url: String?
    ): Boolean {
        url?.run {
            if (startsWith("http")) {
                webView.loadUrl(url)
            } else {
                //查询是否是app intent
                dslIntentQuery {
                    queryData = Uri.parse(url)
                }.apply {
                    if (isNotEmpty()) {
                        //找到了
                        first().activityInfo.run {
                            onOpenAppListener(this, packageName.appBean())
                        }
                    }
                }
            }
        }

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