package com.angcyo.tbs.core.inner

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.view.NestedScrollingChild3
import androidx.core.view.NestedScrollingChildHelper
import androidx.core.view.ViewCompat
import com.angcyo.library.L
import com.angcyo.library.component.appBean
import com.angcyo.library.component.dslIntentQuery
import com.angcyo.library.ex.decode
import com.angcyo.library.ex.encode
import com.angcyo.library.ex.fileSizeString
import com.angcyo.library.ex.nowTimeString
import com.angcyo.library.model.AppBean
import com.angcyo.library.model.FileChooserParam
import com.angcyo.library.utils.*
import com.hjhrq1991.library.tbs.TbsBridgeWebView
import com.tencent.smtt.export.external.extension.proxy.ProxyWebViewClientExtension
import com.tencent.smtt.export.external.interfaces.*
import com.tencent.smtt.sdk.*


/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/03/01
 */
open class TbsWebView(context: Context, attributeSet: AttributeSet? = null) :
    TbsBridgeWebView(context, attributeSet), NestedScrollingChild3 {

    //<editor-fold desc="回调">

    /**网页加载进度回调,
     * 等于0时, 表示页面开始加载
     * 等于100时, 表示页面完成加载
     * */
    var progressChangedAction: (url: String?, progress: Int) -> Unit = { _, _ ->

    }

    /**标题接收回调*/
    var receivedTitleAction: (title: String?) -> Unit = {}

    /**接收到的标题*/
    var receivedTitle: String? = null

    /**打开应用回调*/
    var openAppAction: (url: String, activityInfo: ActivityInfo, appBean: AppBean) -> Unit =
        { url, activityInfo, appBean -> L.d("打开应用:${appBean.appName} ${activityInfo.name}") }

    /**下载文件回调*/
    var downloadAction: (
        url: String /*下载地址*/,
        userAgent: String,
        contentDisposition: String,
        mime: String /*文件mime application/zip*/,
        length: Long /*文件大小 b*/
    ) -> Unit =
        { url, userAgent, contentDisposition, mime, length ->
            L.d(
                "下载:${
                    TbsWeb.getFileName(
                        url,
                        contentDisposition
                    )
                } ${length.fileSizeString()}\n$url $mime\n$userAgent $contentDisposition "
            )
        }

    /**选择文件回调, 选择文件后请务必[onReceiveValue]方法*/
    var fileChooseAction: (param: FileChooserParam) -> Unit = {}

    /**请求拦截回调*/
    var shouldInterceptRequestAction: ((
        view: WebView,
        request: WebResourceRequest?,
        bundle: Bundle?
    ) -> WebResourceResponse?)? = null

    /**加载[url], 需要设置的请求头*/
    var headerAction: (url: String) -> Map<String, String>? = {
        null
    }

    /**是否需要拦截[url]的加载*/
    var shouldOverrideUrlLoadAction: (
        webClient: WebViewClient,
        webView: WebView,
        url: String?
    ) -> Boolean = { webClient, webView, url ->
        false
    }

    //</editor-fold desc="回调">

    //<editor-fold desc="WebViewClient">

    var _loadUrl: String? = null

    //上传文件需要的回调
    var _filePathCallback: ValueCallback<Uri?>? = null
    var _filePathCallbacks: ValueCallback<Array<Uri?>>? = null

    /*
    * 当点击网页里的一个链接,伪流程:    ...代表多个
    * shouldOverrideUrlLoading
    * onPageStarted
    * onProgressChanged
    * onLoadResource...
    * onReceivedTitle
    * onProgressChanged...
    * onPageFinished
    */
    val webClient: WebViewClient = object : WebViewClient() {

        //加载资源, css js ttf html等
        override fun onLoadResource(view: WebView, url: String?) {
            super.onLoadResource(view, url)
            appendWebLog("load:$url")
        }

        override fun onReceivedError(
            view: WebView,
            errorCode: Int,
            description: String?,
            failingUrl: String?
        ) {
            super.onReceivedError(view, errorCode, description, failingUrl)
        }

        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            super.onReceivedError(view, request, error)
            appendWebLog("异常[${request?.method}]:${request?.url}->${error?.errorCode}:${error?.description}")
        }

        override fun onReceivedHttpError(
            view: WebView,
            request: WebResourceRequest?,
            response: WebResourceResponse?
        ) {
            super.onReceivedHttpError(view, request, response)
            appendWebLog("http异常:${response?.statusCode}:${response?.mimeType}:[${request?.method}]:${request?.url}")
        }

        override fun shouldInterceptRequest(view: WebView, url: String?): WebResourceResponse? {
            return super.shouldInterceptRequest(view, url)
        }

        override fun shouldInterceptRequest(
            view: WebView,
            request: WebResourceRequest?
        ): WebResourceResponse? {
            return super.shouldInterceptRequest(view, request)
        }

        //1: 拦截请求, 所有请求都会通过这里. 比如css js等
        override fun shouldInterceptRequest(
            view: WebView,
            request: WebResourceRequest?,
            bundle: Bundle?
        ): WebResourceResponse? {
            appendWebLog("请求[${request?.method}]:${request?.url}")
            return shouldInterceptRequestAction?.invoke(view, request, bundle)
                ?: super.shouldInterceptRequest(view, request, bundle)
        }

        override fun shouldOverrideUrlLoading(webView: WebView, url: String?): Boolean {
            val urlLog =
                "加载:${url?.decode()}\no:${webView.originalUrl?.decode()}\nu:${webView.url?.decode()}\ntitle:${webView.title}"
            L.d(urlLog)

            appendWebLog(urlLog)
            return onShouldOverrideUrlLoading(this, webView, url)
        }

        override fun shouldOverrideUrlLoading(
            webView: WebView,
            requeset: WebResourceRequest?
        ): Boolean {
            L.d("${webView.title} q:$requeset")
            return super.shouldOverrideUrlLoading(webView, requeset)
        }

        //开始加载页面
        override fun onPageStarted(webView: WebView, url: String?, bitmap: Bitmap?) {
            super.onPageStarted(webView, url, bitmap)
            progressChangedAction(url, 0)
            appendWebLog("开始加载页面:[${webView.title}]:$url\n")
        }

        //页面加载完成
        override fun onPageFinished(webView: WebView, url: String?) {
            super.onPageFinished(webView, url)
            progressChangedAction(url, 100)
            appendWebLog("完成加载页面:[${webView.title}]:$url\n")
        }
    }

    //</editor-fold desc="WebViewClient">

    //<editor-fold desc="WebChromeClient">

    val chromeClient: WebChromeClient = object : WebChromeClient() {

        //<editor-fold desc="基础回调">

        //接收页面标题,在[onPageStarted]之后
        override fun onReceivedTitle(webView: WebView?, title: String?) {
            super.onReceivedTitle(webView, title)
            receivedTitle = title
            L.d("${webView?.originalUrl} ${webView?.url} $title")
            this@TbsWebView.receivedTitleAction(title)
        }

        override fun onProgressChanged(webView: WebView?, progress: Int) {
            super.onProgressChanged(webView, progress)
            //L.d("${webView.originalUrl} ${webView.url} $progress")
            progressChangedAction(webView?.url, progress)
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
            L.i("openFileChooser 1:$acceptType")
            _filePathCallback = uploadMsg
            openFileChooseProcess(FileChooserParam(false, acceptType))
        }

        // For Android < 3.0
        fun openFileChooser(uploadMsg: ValueCallback<Uri?>) {
            L.i("openFileChooser 2:")
            _filePathCallback = uploadMsg
            openFileChooseProcess()
        }

        // For Android  > 4.1.1
        override fun openFileChooser(
            uploadMsg: ValueCallback<Uri?>,
            acceptType: String?,
            capture: String?
        ) {
            L.i("openFileChooser 3:$acceptType $capture")
            _filePathCallback = uploadMsg
            openFileChooseProcess(FileChooserParam(false, acceptType))
        }

        // For Android  >= 5.0
        override fun onShowFileChooser(
            webView: WebView,
            filePathCallback: ValueCallback<Array<Uri?>>,
            fileChooserParams: FileChooserParams
        ): Boolean {
            L.i(
                "openFileChooser 4:$filePathCallback ${fileChooserParams.mode}",
                fileChooserParams.acceptTypes
            )
            _filePathCallbacks = filePathCallback
            openFileChooseProcess(
                FileChooserParam(
                    fileChooserParams.mode == FileChooserParams.MODE_OPEN_MULTIPLE,
                    fileChooserParams.acceptTypes?.firstOrNull()
                )
            )
            return true
        }

        //<editor-fold desc="WebChromeClient文件选择">

        //<editor-fold desc="其他">

        override fun onGeolocationPermissionsShowPrompt(
            origin: String?,
            callback: GeolocationPermissionsCallback?
        ) {
            //可以关闭定位功能，内核默认是开启的
            //mWebView.getSettings().setGeolocationEnabled(false);

            //在此可以弹窗提示用户
            //处理后需要回调
            //参数的意义见上面的接口说明
            super.onGeolocationPermissionsShowPrompt(origin, callback)
            L.i("$origin")
            appendWebLog("权限:onGeolocationPermissionsShowPrompt:${origin}")
        }

        override fun onPermissionRequest(permissionRequest: PermissionRequest?) {
            super.onPermissionRequest(permissionRequest)
            L.i(permissionRequest)
            appendWebLog("权限:onPermissionRequest:${permissionRequest?.origin}:${permissionRequest?.resources}")
        }

        override fun onPermissionRequestCanceled(permissionRequest: PermissionRequest?) {
            super.onPermissionRequestCanceled(permissionRequest)
            L.i(permissionRequest)
            appendWebLog("权限:onPermissionRequestCanceled:${permissionRequest?.origin}:${permissionRequest?.resources}")
        }

        override fun onGeolocationPermissionsHidePrompt() {
            super.onGeolocationPermissionsHidePrompt()
            L.i()
            appendWebLog("权限:onGeolocationPermissionsHidePrompt")
        }

        override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
            L.d("${consoleMessage.sourceId()}#${consoleMessage.lineNumber()}:${consoleMessage.message()}")
            return super.onConsoleMessage(consoleMessage)
        }

        //</editor-fold desc="其他">
    }

    //</editor-fold desc="WebChromeClient">

    val childHelper: NestedScrollingChildHelper by lazy {
        NestedScrollingChildHelper(this)
    }

    val scrollOffset = IntArray(2)
    val scrollConsumed = IntArray(2)

    init {
        webViewClient = webClient
        webChromeClient = chromeClient

        TbsWeb.initWebView(this)

        this.view.isClickable = true

        resetOverScrollMode()

        //下载
        setDownloadListener { url, userAgent, contentDisposition, mime, length ->
            downloadAction(url.decode(), userAgent, contentDisposition, mime, length)
        }

        //touch事件
        val webViewEventHandler = TbsWebViewEventHandler()
        webViewClientExtension = webViewEventHandler
        setWebViewCallbackClient(webViewEventHandler)

        isNestedScrollingEnabled = true

        //setLayerType(View.LAYER_TYPE_HARDWARE, null)
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

        if (shouldOverrideUrlLoadAction(webClient, webView, url)) {
            return true
        }

        url?.run {
            if (startsWith("http")) {
                //additionalHttpHeaders
                webView.loadUrl(url, headerAction(url) ?: emptyMap())
            } else {
                //查询是否是app intent
                dslIntentQuery {
                    queryData = Uri.parse(url)
                    queryCategory = listOf(Intent.CATEGORY_BROWSABLE)
                }.apply {
                    if (isNotEmpty()) {
                        //找到了
                        first().activityInfo.run {
                            openAppAction(url.decode(), this, packageName.appBean()!!)
                        }
                    }
                }
            }
        }

        return true
    }

    override fun loadUrl(url: String?) {
        if (url?.startsWith("http") == true) {
            _loadUrl = url
        }
        if (url.isNullOrEmpty()) {
            super.loadUrl(url)
        } else {
            loadUrl(url, headerAction(url) ?: emptyMap())
        }
    }

    override fun loadUrl(url: String?, map: Map<String, String>) {
        if (url?.startsWith("http") == true) {
            _loadUrl = url
        }
        super.loadUrl(url, map)
    }

    /**https://x5.tencent.com/docs/tbsapi.html*/
    override fun loadData(data: String?, mimeType: String?, encoding: String?) {
        super.loadData(data, mimeType, encoding)
    }

    open fun loadData2(data: String?, mimeType: String = "text/html", encoding: String = "utf-8") {
        loadData(data?.encode(encoding), mimeType, encoding)
    }

    override fun loadDataWithBaseURL(
        baseUrl: String?,
        data: String?,
        mimeType: String?,
        encoding: String?,
        historyUrl: String?
    ) {
        super.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, historyUrl)
    }

    open fun loadDataWithBaseURL2(
        data: String?,
        mimeType: String = "text/html",
        encoding: String = "utf-8",
        baseUrl: String? = null,
        historyUrl: String? = null
    ) {
        loadDataWithBaseURL(baseUrl, data, mimeType, encoding, historyUrl)
    }

    /**写入web log*/
    open fun appendWebLog(log: String) {
        "${nowTimeString()} $log \n".writeTo(LogFile.webview.toLogFilePath(), true)
    }

    //</editor-fold desc="初始化相关">

    //<editor-fold desc="文件选择">

    open fun openFileChooseProcess(param: FileChooserParam = FileChooserParam()) {
        //val i = Intent(Intent.ACTION_GET_CONTENT)
        //i.addCategory(Intent.CATEGORY_OPENABLE)
        //i.type = "*/*"
        //startActivityForResult(Intent.createChooser(i, "test"), 0)
        fileChooseAction(param)
    }

    fun onFileChooseResultList(files: List<Uri?>?) {
        onReceiveValue(files?.toTypedArray())
    }

    fun onFileChooseResult(files: Array<Uri?>?) {
        onReceiveValue(files)
    }

    fun onFileChooseResultSingle(file: Uri?) {
        if (file == null) {
            onReceiveValue(null)
        } else {
            onReceiveValue(arrayOf(file))
        }
    }

    /**选择文件后, 调用此方法, 通知给web*/
    fun onReceiveValue(files: Array<Uri?>?) {
        _filePathCallback?.onReceiveValue(files?.firstOrNull())
        _filePathCallbacks?.onReceiveValue(files)
        _filePathCallback = null
        _filePathCallbacks = null
    }

    //</editor-fold desc="文件选择">

    //<editor-fold desc="内嵌滚动支持">

    var nestedYOffset = 0

    fun tbs_dispatchTouchEvent(event: MotionEvent, view: View): Boolean {
        //L.i("touch ... 1")
        //_handleTouchEvent(event, view)
        return super_dispatchTouchEvent(event)
    }

    fun tbs_onInterceptTouchEvent(event: MotionEvent, view: View): Boolean {
        //L.i("touch ... 2")
        _handleTouchEvent(event, view)
        return super_onInterceptTouchEvent(event)
    }

    fun tbs_onTouchEvent(event: MotionEvent, view: View): Boolean {
        //L.i("touch ... 3")

        val actionMasked: Int = event.getActionMasked()

        if (actionMasked == MotionEvent.ACTION_DOWN) {
            nestedYOffset = 0
        }

        //offsetLocation 这一点很重要, 否则下层计算dx, dy时, 会有偏差
        val vtev = MotionEvent.obtain(event)
        vtev.offsetLocation(0f, nestedYOffset.toFloat())

        _handleTouchEvent(vtev, view)
        super_onTouchEvent(vtev)

        vtev.recycle()
        return true
    }


    fun _handleTouchEvent(event: MotionEvent, view: View) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> startNestedScroll(
                ViewCompat.SCROLL_AXIS_VERTICAL,
                ViewCompat.TYPE_TOUCH
            )

            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                stopNestedScroll(ViewCompat.TYPE_TOUCH)
            }
        }
    }

    //原本需要滚动的距离, 用于计算内嵌滚动消耗量和未消耗量
    var _targetScrollDx = 0
    var _targetScrollDy = 0
    var _oldScrollX = 0
    var _oldScrollY = 0

    /**[WebView] */
    fun tbs_overScrollBy(
        deltaX: Int, deltaY: Int,  /*本次滚动多少距离*/
        scrollX: Int, scrollY: Int,  /*总共滚动了多少距离*/
        scrollRangeX: Int, scrollRangeY: Int,  /*滚动的范围*/
        maxOverScrollX: Int, maxOverScrollY: Int,  /**/
        isTouchEvent: Boolean, view: View
    ): Boolean {
        parent?.requestDisallowInterceptTouchEvent(true)

        _targetScrollDx = deltaX
        _targetScrollDy = deltaY
        _oldScrollX = scrollX
        _oldScrollY = scrollY
        scrollConsumed.fill(0)
        dispatchNestedPreScroll(deltaX, deltaY, scrollConsumed, scrollOffset, ViewCompat.TYPE_TOUCH)
        val dY = deltaY - scrollConsumed[1]
        _targetScrollDy = dY

        nestedYOffset += scrollOffset[1]

//        L.d(
//            "deltaX:$deltaX deltaY:$deltaY dY:$dY ${scrollConsumed[1]} " +
//                    "scrollX:$scrollX scrollY:$scrollY " +
//                    "scrollRangeX:$scrollRangeX scrollRangeY:$scrollRangeY " +
//                    "maxX:$maxOverScrollX maxY:$maxOverScrollY " +
//                    "isTouchEvent:$isTouchEvent"
//        )
        super_overScrollBy(
            deltaX, dY,
            scrollX, scrollY,
            scrollRangeX, scrollRangeY,
            maxOverScrollX, maxOverScrollY,
            isTouchEvent
        )
        //L.e("滚动:$deltaY 消耗:${scrollConsumed[1]} 实际滚动:$dY")
        return true
    }

    fun tbs_onOverScrolled(
        scrollX: Int,
        scrollY: Int,
        clampedX: Boolean,
        clampedY: Boolean,
        view: View
    ) {
        //L.w("scrollX:$scrollX scrollY:$scrollY clampedX:$clampedX clampedY:$clampedY ")
        //super_onOverScrolled(scrollX, if(scrollY>0) 1 else if(scrollY<0) -1 else 0, clampedX, clampedY)
        super_onOverScrolled(scrollX, scrollY, clampedX, clampedY)

        val scrolledDeltaY: Int = scrollY - _oldScrollY
        val unconsumedY: Int = _targetScrollDy - scrolledDeltaY

        //L.e("滚动:$scrolledDeltaY 未消耗:${unconsumedY}")

        dispatchNestedScroll(
            0, scrolledDeltaY,
            0, unconsumedY,
            scrollOffset, ViewCompat.TYPE_TOUCH, scrollConsumed
        )
        nestedYOffset += scrollOffset[1]
    }

    fun tbs_onScrollChanged(left: Int, top: Int, oldLeft: Int, oldTop: Int, view: View) {
//        L.i(
//            "left:$left top:$top oldLeft:$oldLeft oldTop:$oldTop " +
//                    "scrollOffset:${scrollOffset[0]},${scrollOffset[1]} scrollConsumed:${scrollConsumed[0]},${scrollConsumed[1]}"
//        )
        super_onScrollChanged(left, top, oldLeft, oldTop)
    }

    fun tbs_computeScroll(view: View) {
        super_computeScroll()
    }

    override fun setNestedScrollingEnabled(enabled: Boolean) {
        childHelper.isNestedScrollingEnabled = enabled
    }

    override fun isNestedScrollingEnabled(): Boolean {
        return childHelper.isNestedScrollingEnabled
    }

    override fun startNestedScroll(axes: Int): Boolean {
        return childHelper.startNestedScroll(axes)
    }

    override fun startNestedScroll(axes: Int, type: Int): Boolean {
        return childHelper.startNestedScroll(axes, type)
    }

    override fun dispatchNestedPreScroll(
        dx: Int,
        dy: Int,
        consumed: IntArray?,
        offsetInWindow: IntArray?,
        type: Int
    ): Boolean {
        return childHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type)
    }

    override fun dispatchNestedPreScroll(
        dx: Int,
        dy: Int,
        consumed: IntArray?,
        offsetInWindow: IntArray?
    ): Boolean {
        return childHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow)
    }

    override fun dispatchNestedScroll(
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        offsetInWindow: IntArray?,
        type: Int,
        consumed: IntArray
    ) {
        childHelper.dispatchNestedScroll(
            dxConsumed,
            dyConsumed,
            dxUnconsumed,
            dyUnconsumed,
            offsetInWindow,
            type,
            consumed
        )
    }

    override fun dispatchNestedScroll(
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        offsetInWindow: IntArray?
    ): Boolean {
        return childHelper.dispatchNestedScroll(
            dxConsumed,
            dyConsumed,
            dxUnconsumed,
            dyUnconsumed,
            offsetInWindow
        )
    }

    override fun dispatchNestedScroll(
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        offsetInWindow: IntArray?,
        type: Int
    ): Boolean {
        return childHelper.dispatchNestedScroll(
            dxConsumed,
            dyConsumed,
            dxUnconsumed,
            dyUnconsumed,
            offsetInWindow,
            type
        )
    }

    override fun dispatchNestedPreFling(velocityX: Float, velocityY: Float): Boolean {
        return childHelper.dispatchNestedPreFling(velocityX, velocityY)
    }

    override fun dispatchNestedFling(
        velocityX: Float,
        velocityY: Float,
        consumed: Boolean
    ): Boolean {
        return childHelper.dispatchNestedFling(velocityX, velocityY, consumed)
    }

    override fun stopNestedScroll() {
        childHelper.stopNestedScroll()
    }

    override fun stopNestedScroll(type: Int) {
        childHelper.stopNestedScroll(type)
    }

    override fun hasNestedScrollingParent(): Boolean {
        return childHelper.hasNestedScrollingParent()
    }

    override fun hasNestedScrollingParent(type: Int): Boolean {
        return childHelper.hasNestedScrollingParent(type)
    }

    //</editor-fold desc="内嵌滚动支持">

    //<editor-fold desc="WebViewCallbackClient">

    inner class TbsWebViewEventHandler : ProxyWebViewClientExtension(), WebViewCallbackClient {
        override fun invalidate() {
            ViewCompat.postInvalidateOnAnimation(this@TbsWebView)
        }

        override fun onTouchEvent(event: MotionEvent, view: View): Boolean {
            return tbs_onTouchEvent(event, view)
        }

        override fun overScrollBy(
            deltaX: Int, deltaY: Int,  /*本次滚动多少距离*/
            scrollX: Int, scrollY: Int,  /*总共滚动了多少距离*/
            scrollRangeX: Int, scrollRangeY: Int,  /*滚动的范围*/
            maxOverScrollX: Int, maxOverScrollY: Int,  /**/
            isTouchEvent: Boolean, view: View
        ): Boolean {
            return tbs_overScrollBy(
                deltaX,
                deltaY,
                scrollX,
                scrollY,
                scrollRangeX,
                scrollRangeY,
                maxOverScrollX,
                maxOverScrollY,
                isTouchEvent,
                view
            )
        }

        override fun computeScroll(view: View) {
            tbs_computeScroll(view)
        }

        override fun dispatchTouchEvent(event: MotionEvent, view: View): Boolean {
            return tbs_dispatchTouchEvent(event, view)
        }

        override fun onInterceptTouchEvent(event: MotionEvent, view: View): Boolean {
            return tbs_onInterceptTouchEvent(event, view)
        }

        override fun onOverScrolled(
            scrollX: Int,
            scrollY: Int,
            clampedX: Boolean,
            clampedY: Boolean,
            view: View
        ) {
            tbs_onOverScrolled(scrollX, scrollY, clampedX, clampedY, view)
        }

        override fun onScrollChanged(left: Int, top: Int, oldLeft: Int, oldTop: Int, view: View) {
            tbs_onScrollChanged(left, top, oldLeft, oldTop, view)
        }
    }

    //</editor-fold desc="WebViewCallbackClient">
}