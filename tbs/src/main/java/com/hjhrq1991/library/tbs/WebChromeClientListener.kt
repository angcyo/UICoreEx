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
 * @author hjhrq1991 created at 16/11/21 10 38.
 */
class WebChromeClientListener : OnWebChromeClientListener {
    override fun onReceivedTitle(
        view: WebView?,
        title: String?
    ) {
    }

    override fun onProgressChanged(
        view: WebView?,
        newProgress: Int
    ) {
    }

    override fun onExceededDatabaseQuota(
        url: String?,
        databaseIdentifier: String?,
        quota: Long,
        estimatedDatabaseSize: Long,
        totalQuota: Long,
        quotaUpdater: WebStorage.QuotaUpdater?
    ) {
    }

    override val defaultVideoPoster: Bitmap?
        get() = null

    override fun getVisitedHistory(valueCallback: ValueCallback<Array<String?>?>?) {}
    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        return false
    }

    override fun onCreateWindow(
        webView: WebView?,
        isDialog: Boolean,
        isUserGesture: Boolean,
        message: Message?
    ): Boolean {
        return false
    }

    override fun onGeolocationPermissionsHidePrompt() {}
    override fun onGeolocationPermissionsShowPrompt(
        origin: String?,
        geolocationPermissionsCallback: GeolocationPermissionsCallback?
    ) {
    }

    override fun onHideCustomView() {}
    override fun onJsAlert(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?
    ): Boolean {
        return false
    }

    override fun onJsConfirm(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?
    ): Boolean {
        return false
    }

    override fun onJsPrompt(
        view: WebView?,
        url: String?,
        message: String?,
        defaultValue: String?,
        result: JsPromptResult?
    ): Boolean {
        return false
    }

    override fun onJsBeforeUnload(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?
    ): Boolean {
        return false
    }

    override fun onJsTimeout(): Boolean {
        return false
    }

    override fun onReachedMaxAppCacheSize(
        requiredStorage: Long,
        quota: Long,
        quotaUpdater: WebStorage.QuotaUpdater?
    ) {
    }

    override fun onReceivedIcon(
        webView: WebView?,
        bitmap: Bitmap?
    ) {
    }

    override fun onReceivedTouchIconUrl(
        webView: WebView?,
        url: String?,
        precomposed: Boolean
    ) {
    }

    override fun onRequestFocus(webView: WebView?) {}
    override fun onShowCustomView(
        view: View?,
        customViewCallback: IX5WebChromeClient.CustomViewCallback?
    ) {
    }

    override fun onShowCustomView(
        view: View?,
        requestedOrientation: Int,
        customViewCallback: IX5WebChromeClient.CustomViewCallback?
    ) {
    }

    override fun onCloseWindow(webView: WebView?) {}
    override val videoLoadingProgressView: View?
        get() = null

    override fun openFileChooser(
        valueCallback: ValueCallback<Uri?>?,
        s: String?,
        s1: String?
    ) {
    }

    override fun onShowFileChooser(
        webView: WebView?,
        valueCallback: ValueCallback<Array<Uri?>?>?,
        fileChooserParams: WebChromeClient.FileChooserParams?
    ): Boolean {
        return false
    }
}