package com.hjhrq1991.library.tbs

import android.graphics.Bitmap
import android.net.Uri
import android.os.Message
import android.view.View
import com.tencent.smtt.export.external.interfaces.*
import com.tencent.smtt.sdk.ValueCallback
import com.tencent.smtt.sdk.WebChromeClient
import com.tencent.smtt.sdk.WebStorage
import com.tencent.smtt.sdk.WebView

/**
 * @author hjhrq1991 created at 16/11/21 10 23.
 */
interface OnWebChromeClientListener {
    fun onReceivedTitle(
        view: WebView?,
        title: String?
    )

    fun onProgressChanged(view: WebView?, newProgress: Int)
    fun onExceededDatabaseQuota(
        url: String?,
        databaseIdentifier: String?,
        quota: Long,
        estimatedDatabaseSize: Long,
        totalQuota: Long,
        quotaUpdater: WebStorage.QuotaUpdater?
    )

    val defaultVideoPoster: Bitmap?
    fun getVisitedHistory(valueCallback: ValueCallback<Array<String?>?>?)
    fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean
    fun onCreateWindow(
        webView: WebView?,
        isDialog: Boolean,
        isUserGesture: Boolean,
        message: Message?
    ): Boolean

    fun onGeolocationPermissionsHidePrompt()
    fun onGeolocationPermissionsShowPrompt(
        origin: String?,
        geolocationPermissionsCallback: GeolocationPermissionsCallback?
    )

    fun onHideCustomView()
    fun onJsAlert(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?
    ): Boolean

    fun onJsConfirm(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?
    ): Boolean

    fun onJsPrompt(
        view: WebView?,
        url: String?,
        message: String?,
        defaultValue: String?,
        result: JsPromptResult?
    ): Boolean

    fun onJsBeforeUnload(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?
    ): Boolean

    fun onJsTimeout(): Boolean
    fun onReachedMaxAppCacheSize(
        requiredStorage: Long,
        quota: Long,
        quotaUpdater: WebStorage.QuotaUpdater?
    )

    fun onReceivedIcon(webView: WebView?, bitmap: Bitmap?)
    fun onReceivedTouchIconUrl(
        webView: WebView?,
        url: String?,
        precomposed: Boolean
    )

    fun onRequestFocus(webView: WebView?)
    fun onShowCustomView(
        view: View?,
        customViewCallback: IX5WebChromeClient.CustomViewCallback?
    )

    fun onShowCustomView(
        view: View?,
        requestedOrientation: Int,
        customViewCallback: IX5WebChromeClient.CustomViewCallback?
    )

    fun onCloseWindow(webView: WebView?)
    val videoLoadingProgressView: View?
    fun openFileChooser(
        valueCallback: ValueCallback<Uri?>?,
        s: String?,
        s1: String?
    )

    fun onShowFileChooser(
        webView: WebView?,
        valueCallback: ValueCallback<Array<Uri?>?>?,
        fileChooserParams: WebChromeClient.FileChooserParams?
    ): Boolean
}