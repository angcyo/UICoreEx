package com.angcyo.tbs.core.inner

import android.content.Context
import android.content.Intent
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
import com.tencent.smtt.export.external.interfaces.ConsoleMessage
import com.tencent.smtt.export.external.interfaces.IX5WebChromeClient
import com.tencent.smtt.export.external.interfaces.IX5WebViewBase
import com.tencent.smtt.export.external.interfaces.WebResourceRequest
import com.tencent.smtt.sdk.ValueCallback
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
    var onOpenAppListener: (url: String, activityInfo: ActivityInfo, appBean: AppBean) -> Unit =
        { url, activityInfo, appBean -> L.d("打开应用:${appBean.appName} ${activityInfo.name}") }

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

    /**选择文件回调, 选择文件后请务必[onReceiveValue]方法*/
    var onFileChooseListener: (param: FileChooserParam) -> Unit = {}

    //</editor-fold desc="回调">

    //<editor-fold desc="WebViewClient">

    var _loadUrl: String? = null

    //上传文件需要的回调
    var _filePathCallback: ValueCallback<Uri?>? = null
    var _filePathCallbacks: ValueCallback<Array<Uri?>>? = null

    val webClient: WebViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(webView: WebView, url: String?): Boolean {
            L.d("url:$url o:${webView.originalUrl} u:${webView.url} title:${webView.title} ")
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

        //<editor-fold desc="基础回调">

        override fun onReceivedTitle(webView: WebView, title: String) {
            super.onReceivedTitle(webView, title)
            L.d("${webView.originalUrl} ${webView.url} $title")
            this@TbsWebView.onReceivedTitle(title)
        }

        override fun onProgressChanged(webView: WebView, progress: Int) {
            super.onProgressChanged(webView, progress)
            //L.d("${webView.originalUrl} ${webView.url} $progress")
            onProgressChanged(webView.url, progress)
        }

        //</editor-fold desc="基础回调">

        //<editor-fold desc="全屏播放视频">
        var viewCallback: IX5WebChromeClient.CustomViewCallback? = null

        override fun onShowCustomView(
            view: View,
            viewCallback: IX5WebChromeClient.CustomViewCallback
        ) {
            super.onShowCustomView(view, viewCallback)
            // 此处的 view 就是全屏的视频播放界面，需要把它添加到我们的界面上
            this.viewCallback = viewCallback

            L.i(view, " ", viewCallback)
        }

        override fun onShowCustomView(
            view: View,
            requestedOrientation: Int,
            viewCallback: IX5WebChromeClient.CustomViewCallback
        ) {
            super.onShowCustomView(view, requestedOrientation, viewCallback)
            onShowCustomView(view, viewCallback)
        }

        override fun onHideCustomView() {
            super.onHideCustomView()
            // 退出全屏播放，我们要把之前添加到界面上的视频播放界面移除
            viewCallback?.onCustomViewHidden()

            L.i(viewCallback)
        }

        //</editor-fold desc="全屏播放视频">

        //<editor-fold desc="WebChromeClient文件选择">

        // For Android 3.0+
        fun openFileChooser(uploadMsg: ValueCallback<Uri?>, acceptType: String?) {
            L.i("openFileChooser 1 $acceptType")
            _filePathCallback = uploadMsg
            openFileChooseProcess(FileChooserParam(acceptType))
        }

        // For Android < 3.0
        fun openFileChooser(uploadMsg: ValueCallback<Uri?>) {
            L.i("openFileChooser 2")
            _filePathCallback = uploadMsg
            openFileChooseProcess()
        }

        // For Android  > 4.1.1
        override fun openFileChooser(
            uploadMsg: ValueCallback<Uri?>,
            acceptType: String?,
            capture: String?
        ) {
            L.i("openFileChooser 3 $acceptType $capture")
            _filePathCallback = uploadMsg
            openFileChooseProcess(FileChooserParam(acceptType))
        }

        // For Android  >= 5.0
        override fun onShowFileChooser(
            webView: WebView,
            filePathCallback: ValueCallback<Array<Uri?>>,
            fileChooserParams: FileChooserParams
        ): Boolean {
            L.i("openFileChooser 4:$filePathCallback ${fileChooserParams.acceptTypes}")
            _filePathCallbacks = filePathCallback
            openFileChooseProcess(FileChooserParam(fileChooserParams.acceptTypes?.firstOrNull()))
            return true
        }

        //<editor-fold desc="WebChromeClient文件选择">

        //<editor-fold desc="其他">

        override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
            L.d("${consoleMessage.sourceId()}#${consoleMessage.lineNumber()}:${consoleMessage.message()}")
            return super.onConsoleMessage(consoleMessage)
        }

        //</editor-fold desc="其他">
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

    //<editor-fold desc="初始化相关">

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
                    queryCategory = listOf(Intent.CATEGORY_BROWSABLE)
                }.apply {
                    if (isNotEmpty()) {
                        //找到了
                        first().activityInfo.run {
                            onOpenAppListener(url, this, packageName.appBean())
                        }
                    }
                }
            }
        }

        return true
    }

    override fun loadUrl(url: String?) {
        _loadUrl = url
        super.loadUrl(url)
    }

    override fun loadUrl(url: String?, map: MutableMap<String, String>?) {
        _loadUrl = url
        super.loadUrl(url, map)
    }

    //</editor-fold desc="初始化相关">

    //<editor-fold desc="文件选择">

    open fun openFileChooseProcess(param: FileChooserParam = FileChooserParam()) {
        //val i = Intent(Intent.ACTION_GET_CONTENT)
        //i.addCategory(Intent.CATEGORY_OPENABLE)
        //i.type = "*/*"
        //startActivityForResult(Intent.createChooser(i, "test"), 0)
        onFileChooseListener(param)
    }

    fun onFileChooseResult(files: Array<Uri?>?) {
        onReceiveValue(files)
    }

    /**选择文件后, 调用此方法, 通知给web*/
    fun onReceiveValue(files: Array<Uri?>?) {
        _filePathCallback?.onReceiveValue(files?.firstOrNull())
        _filePathCallbacks?.onReceiveValue(files)
        _filePathCallback = null
        _filePathCallbacks = null
    }

    //</editor-fold desc="文件选择">

}