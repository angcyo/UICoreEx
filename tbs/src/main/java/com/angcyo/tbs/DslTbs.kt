package com.angcyo.tbs

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.angcyo.DslAHelper
import com.angcyo.core.component.file.writeTo
import com.angcyo.library.L
import com.angcyo.library.component.ThreadExecutor
import com.angcyo.tbs.core.TbsWebActivity
import com.angcyo.tbs.core.TbsWebConfig
import com.angcyo.tbs.core.TbsWebFragment
import com.tencent.smtt.sdk.CookieManager
import com.tencent.smtt.sdk.CookieSyncManager
import com.tencent.smtt.sdk.QbSdk
import com.tencent.smtt.sdk.WebView
import com.tencent.tbs.reader.TbsFileInterfaceImpl
import org.json.JSONException
import org.json.JSONObject
import java.util.*


/**
 * QB 服务用于打开网页, 打开视频.
 *
 * TBS 文件服务, 用于打开文档, 文本.
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/03/01
 */

class DslTbs {
    companion object {

        /**x5内核是否初始化成功*/
        var isX5Core = false

        /**初始化tbs*/
        fun init(context: Context) {
            val appContext = context.applicationContext

            ThreadExecutor.execute {
                //浏览器服务
                QbSdk.initX5Environment(appContext, object : QbSdk.PreInitCallback {
                    override fun onCoreInitFinished() {
                        L.d("onCoreInitFinished".apply {
                            writeTo()
                        })
                    }

                    override fun onViewInitFinished(isX5Core: Boolean) {
                        DslTbs.isX5Core = isX5Core
                        //x5內核初始化完成的回调，为true表示x5内核加载成功，否则表示x5内核加载失败，会自动切换到系统内核。
                        L.d("onViewInitFinished,isX5Core=$isX5Core".apply {
                            writeTo()
                        })
                    }
                })

                //QbSdk.forceSysWebView()

                //文档服务
                TbsFileInterfaceImpl.initEngine(appContext)
                TbsFileInterfaceImpl.setProviderSetting(FileProvider::class.java.name)

                QbSdk.getMiniQBVersion(appContext).apply {
                    L.i("MiniQBVersion:$this".apply {
                        writeTo()
                    })
                }
            }
        }

        /**获取tbs崩溃日志*/
        fun getCrashExtraMessage(context: Context): String {
            return WebView.getCrashExtraMessage(context.applicationContext)
        }

        /**弹出打开文件的Intent选择对话框(包含广告 QQ浏览器)*/
        fun openFileReader(
            context: Context,
            pathName: String?,
            callback: (String?) -> Unit = {}
        ): Int {
            val params = HashMap<String, String>()
            params["local"] = "true"
            params["entryId"] = "2"
            params["allowAutoDestory"] = "true"
            val jsonObject = JSONObject()
            try {
                jsonObject.put("pkgName", context.applicationContext.packageName)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            params["menuData"] = jsonObject.toString()
            return QbSdk.openFileReader(
                context, pathName, params
            ) { value -> callback.invoke(value) }
        }

        /**是否支持打开文件QB, 多数为视频格式*/
        fun canOpenFileQb(
            context: Context,
            filePath: String?,
            callback: (Boolean) -> Unit = {}
        ) {
            QbSdk.canOpenFile(context, filePath) { value -> callback.invoke(value) }
        }

        /**是否支持打开文件TBS, 多数为文档格式*/
        fun canOpenFileTbs(fileExt: String): Boolean {
            return TbsFileInterfaceImpl.canOpenFile(fileExt.toLowerCase())
        }

        /** 清除Cookie */
        fun removeCookie(context: Context) {
            CookieSyncManager.createInstance(context)
            val cookieManager = CookieManager.getInstance()
            cookieManager.removeAllCookie()
            CookieSyncManager.getInstance().sync()
        }


        /**
         * 设置Cookie
         * synCookies(this, "www.baidu.com", "age=20;sex=1;time=today");
         *
         * @param context
         * @param url
         * @param cookie  格式：uid=21233 如需设置多个，需要多次调用
         */
        fun synCookies(context: Context, url: String, cookie: String) {
            CookieSyncManager.createInstance(context)
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setCookie(url, cookie) //cookies是在HttpClient中获得的cookie
            CookieSyncManager.getInstance().sync()
        }
    }
}

/**打开url, 文件, 媒体, 等...*/
fun DslAHelper.open(config: TbsWebConfig.() -> Unit) {
    start(Intent(context, TbsWebActivity::class.java).apply {
        putExtra(TbsWebFragment.KEY_CONFIG, TbsWebConfig().apply {
            config()
        })
    })
}