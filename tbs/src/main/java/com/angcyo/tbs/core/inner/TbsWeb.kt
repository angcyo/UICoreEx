package com.angcyo.tbs.core.inner

import android.net.Uri
import com.angcyo.library.ex.patternList
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
        webSetting.loadWithOverviewMode = true //this
        webSetting.setSupportMultipleWindows(false)//this
        // webSetting.setLoadWithOverviewMode(true);
        webSetting.setAppCacheEnabled(true)//this
        webSetting.databaseEnabled = true;//this
        webSetting.domStorageEnabled = true
        webSetting.setGeolocationEnabled(true)
        webSetting.setAppCacheMaxSize(Long.MAX_VALUE)
        // webSetting.setPageCacheCapacity(IX5WebSettings.DEFAULT_CACHE_CAPACITY);//this
        webSetting.pluginState = WebSettings.PluginState.ON //ON_DEMAND
        webSetting.setRenderPriority(WebSettings.RenderPriority.HIGH)//this
        webSetting.cacheMode = WebSettings.LOAD_DEFAULT//this
        // this.getSettingsExtension().setPageCacheCapacity(IX5WebSettings.DEFAULT_CACHE_CAPACITY);//extension
        // settings 的设计

        webSetting.defaultTextEncodingName = "utf-8"
        webSetting.setAppCachePath(webView.context.cacheDir.absolutePath)
        webSetting.databasePath = webView.context.cacheDir.absolutePath
        webSetting.setGeolocationDatabasePath(webView.context.cacheDir.absolutePath)

        webSetting.mediaPlaybackRequiresUserGesture = true//this

        webSetting.setUserAgent(webSetting.userAgentString + " angcyo")

        CookieSyncManager.createInstance(webView.context)
        CookieSyncManager.getInstance().sync()
    }

    /**
     * attachment;filename="百度手机助手(360手机助手).apk"
     * attachment;filename="baidusearch_AndroidPhone_1021446w.apk"
     */
    fun getFileName(url: String, attachment: String?): String? {
        var name: String? = null
        name = getFileNameFromAttachment(attachment)
        if (!name.isNullOrBlank()) {
            return name.trim('"')
        }

        name = getFileNameFromAttachment(url)
        if (!name.isNullOrBlank()) {
            return name.trim('"')
        }

        //最后一步, 截取url后面的文件名
        url.split("?").getOrNull(0)?.run {
            val indexOf = lastIndexOf("/")
            if (indexOf != -1) {
                name = this.substring(indexOf + 1, this.length)
            }
        }
        return name?.trim('"')
    }

    /**
     * attachment; filename=YYB.998886.4c1b4029188a9b5f2ad007e997da02d4.1004112.apk
     * attachment;filename="baidusearch_AndroidPhone_1021446w.apk"
     * */
    fun getFileNameFromAttachment(attachment: String?): String? {
        var name: String? = null
        attachment?.run {
            if (isNotEmpty()) {
                val decode = Uri.decode(this)

                //正则匹配filename
                decode.patternList("filename=\"(.*)\"").firstOrNull()?.run {
                    name = this.split("filename=").getOrNull(1)

                    if (!name.isNullOrBlank()) {
                        return name
                    }
                }
                decode.patternList("filename=(.*)").firstOrNull()?.run {
                    name = this.split("filename=").getOrNull(1)

                    if (!name.isNullOrBlank()) {
                        return name
                    }
                }

                //正则匹配name
                decode.patternList("name=\"(.*)\"").firstOrNull()?.run {
                    name = this.split("name=").getOrNull(1)

                    if (!name.isNullOrBlank()) {
                        return name
                    }
                }
                decode.patternList("name=(.*)").firstOrNull()?.run {
                    name = this.split("name=").getOrNull(1)

                    if (!name.isNullOrBlank()) {
                        return name
                    }
                }

                //uri查询
                Uri.parse(decode).run {
                    //filename
                    getQueryParameter("filename")?.run {
                        name = this

                        if (!name.isNullOrBlank()) {
                            return name
                        }
                    }
                    //name
                    getQueryParameter("name")?.run {
                        name = this

                        if (!name.isNullOrBlank()) {
                            return name
                        }
                    }
                }
            }
        }
        return name
    }
}