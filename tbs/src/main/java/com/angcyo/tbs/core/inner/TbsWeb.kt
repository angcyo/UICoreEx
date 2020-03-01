package com.angcyo.tbs.core.inner

import com.tencent.smtt.sdk.CookieSyncManager
import com.tencent.smtt.sdk.WebSettings
import com.tencent.smtt.sdk.WebView

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/03/01
 */
object TbsWeb {

    /**初始化*/
    fun initWebView(webView: WebView) {
        val webSetting: WebSettings = webView.settings
        webSetting.javaScriptEnabled = true
        webSetting.javaScriptCanOpenWindowsAutomatically = true
        webSetting.allowFileAccess = true
        webSetting.layoutAlgorithm = WebSettings.LayoutAlgorithm.NARROW_COLUMNS
        webSetting.setSupportZoom(true)//支持放大
        webSetting.builtInZoomControls = true//放大控制
        webSetting.displayZoomControls = false//放大控件
        webSetting.useWideViewPort = true
        webSetting.setSupportMultipleWindows(true)//this
        // webSetting.setLoadWithOverviewMode(true);
        webSetting.setAppCacheEnabled(true)//this
        webSetting.databaseEnabled = true;//this
        webSetting.domStorageEnabled = true
        webSetting.setGeolocationEnabled(true)
        webSetting.setAppCacheMaxSize(Long.MAX_VALUE)
        // webSetting.setPageCacheCapacity(IX5WebSettings.DEFAULT_CACHE_CAPACITY);//this
        webSetting.pluginState = WebSettings.PluginState.ON_DEMAND
        webSetting.setRenderPriority(WebSettings.RenderPriority.HIGH)//this
        webSetting.cacheMode = WebSettings.LOAD_NO_CACHE
        // this.getSettingsExtension().setPageCacheCapacity(IX5WebSettings.DEFAULT_CACHE_CAPACITY);//extension
        // settings 的设计

        webSetting.defaultTextEncodingName = "utf-8"
        webSetting.setAppCachePath(webView.context.cacheDir.absolutePath)
        webSetting.databasePath = webView.context.cacheDir.absolutePath
        webSetting.setGeolocationDatabasePath(webView.context.cacheDir.absolutePath)

        webSetting.setUserAgent(webSetting.userAgentString + " angcyo")

        CookieSyncManager.createInstance(webView.context)
        CookieSyncManager.getInstance().sync()
    }
}