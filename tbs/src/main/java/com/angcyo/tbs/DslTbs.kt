package com.angcyo.tbs

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.angcyo.DslAHelper
import com.angcyo.core.component.file.writeTo
import com.angcyo.library.L
import com.angcyo.library.app
import com.angcyo.library.component.ThreadExecutor
import com.angcyo.library.ex.file
import com.angcyo.library.ex.isFileExist
import com.angcyo.tbs.core.TbsWebActivity
import com.angcyo.tbs.core.TbsWebConfig
import com.angcyo.tbs.core.TbsWebFragment
import com.tencent.smtt.export.external.TbsCoreSettings
import com.tencent.smtt.sdk.*
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

                //首次初始化冷启动优化 https://x5.tencent.com/docs/access.html
                // 在调用TBS初始化、创建WebView之前进行如下配置
                val map = hashMapOf<String, Any>()
                map[TbsCoreSettings.TBS_SETTINGS_USE_SPEEDY_CLASSLOADER] = true
                map[TbsCoreSettings.TBS_SETTINGS_USE_DEXLOADER_SERVICE] = true
                QbSdk.initTbsSettings(map)

                QbSdk.initBuglyAsync(true)
                QbSdk.setDownloadWithoutWifi(true)
                QbSdk.setNeedInitX5FirstTime(true)
                //QbSdk.setTbsLogClient()

                QbSdk.setTbsListener(object : TbsListener {
                    override fun onInstallFinish(var1: Int) {
                        L.d("onInstallFinish $var1".apply {
                            writeTo()
                        })
                    }

                    override fun onDownloadFinish(var1: Int) {
                        L.d("onDownloadFinish $var1".apply {
                            writeTo()
                        })
                    }

                    override fun onDownloadProgress(progress: Int) {
                        L.d("onDownloadProgress $progress".apply {
                            writeTo()
                        })
                    }
                })

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
                //TbsFileInterfaceImpl.initEngine(appContext)
                //TbsFileInterfaceImpl.setProviderSetting(FileProvider::class.java.name)

                try {
                    QbSdk.getMiniQBVersion(appContext).apply {
                        L.i("MiniQBVersion:$this".apply {
                            writeTo()
                        })
                    }
                } catch (e: Exception) {
                    L.w(e)
                }
            }
        }

        fun tbsLogReport(context: Context, enable: Boolean) {
            TbsLogReport.getInstance(context).shouldUploadEventReport = enable
        }

        /**判断当前Tbs播放器是否已经可以使用。*/
        fun canUseTbsPlayer(context: Context = app()): Boolean {
            return TbsVideo.canUseTbsPlayer(context)
        }

        /**
         * 直接调用播放接口，传入视频流的url
         * extraData对象是根据定制需要传入约定的信息，没有需要可以传如null
         * extraData可以传入key: "screenMode", 值: 102, 来控制默认的播放UI
         * 类似: extraData.putInt("screenMode", 102); 来实现默认全屏+控制栏等UI
         * */
        fun openVideo(context: Context = app(), videoUrl: String, extraData: Bundle? = null) {
            TbsVideo.openVideo(context, videoUrl, extraData)
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
        fun canOpenFileTbs(tbsReaderView: TbsReaderView, fileExt: String): Boolean {
            return tbsReaderView.preOpen(fileExt.toLowerCase(), false)/* &&
                    TbsReaderView.isSupportExt(app(), fileExt)*/
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

        /**
         * 一次性删除所有缓存
         *  https://x5.tencent.com/docs/webview.html
         * */
        fun clearAllWebViewCache(context: Context, isClearCookie: Boolean = false) {
            //清除cookie
            QbSdk.clearAllWebViewCache(context, isClearCookie)

            ////清除cookie
            //CookieManager.getInstance().removeAllCookies(null);
            ////清除storage相关缓存
            //WebStorage.getInstance().deleteAllData();;
            ////清除用户密码信息
            //WebViewDatabase.getInstance(Context context).clearUsernamePassword();
            ////清除httpauth信息
            //WebViewDatabase.getInstance(Context context).clearHttpAuthUsernamePassword();
            ////清除表单数据
            //WebViewDatabase.getInstance(Context context).clearFormData();
            ////清除页面icon图标信息
            //WebIconDatabase.getInstance().removeAllIcons();
            ////删除地理位置授权，也可以删除某个域名的授权（参考接口类）
            //GeolocationPermissions.getInstance().clearAll();
        }
    }
}

/**打开url, 文件, 媒体, 等...*/
fun DslAHelper.open(
    url: String? = null,
    cls: Class<out TbsWebActivity> = TbsWebActivity::class.java,
    config: TbsWebConfig.() -> Unit = {}
) {
    start(Intent(context, cls).apply {
        putExtra(TbsWebFragment.KEY_CONFIG, TbsWebConfig().apply {
            try {
                if (!url.isNullOrEmpty()) {
                    uri = if (url.isFileExist()) {
                        Uri.fromFile(url.file())
                    } else {
                        Uri.parse(url)
                    }
                }
            } catch (e: Exception) {
                L.w(e)
            }
            config()
        })
    })
}