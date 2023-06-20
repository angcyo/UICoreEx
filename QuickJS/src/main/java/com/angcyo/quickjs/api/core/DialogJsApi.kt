package com.angcyo.quickjs.api.core

import android.webkit.JavascriptInterface
import androidx.activity.result.ActivityResultCaller
import androidx.annotation.Keep
import androidx.core.net.toUri
import com.angcyo.core.tgStrokeLoading
import com.angcyo.dialog.loadLoadingBottomCaller
import com.angcyo.dialog.messageDialog
import com.angcyo.dialog.other.singleImageDialog
import com.angcyo.download.version.VersionUpdateBean
import com.angcyo.download.version.versionUpdate
import com.angcyo.library.component.lastContext
import com.angcyo.library.component.onMain
import com.angcyo.quickjs.api.BaseJSInterface
import com.angcyo.quickjs.api.getOrBoolean
import com.angcyo.quickjs.api.getOrInt
import com.angcyo.quickjs.api.getOrLong
import com.angcyo.quickjs.api.getOrString
import com.quickjs.JSObject

/**
 * 弹窗api, 对话框
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/06/12
 */
@Keep
class DialogJsApi : BaseJSInterface() {

    override val interfaceName: String = "dialog"

    //region ---loading---

    /**显示转圈圈
     * ```
     * AppJs.dialog.loading();
     * ```
     * */
    @JavascriptInterface
    fun loading() {
        onMain {
            lastContext.tgStrokeLoading(false, false) { isCancel, loadEnd ->
                //no op
            }
        }
    }

    @JavascriptInterface
    fun bottomLoading(tip: String?, successTip: String?) {
        onMain {
            val context = lastContext
            if (context is ActivityResultCaller) {
                context.loadLoadingBottomCaller(
                    tip,
                    successTip,
                    false,
                    false
                ) { isCancel, loadEnd ->

                }
            } else {
                loading()
            }
        }
    }

    /**隐藏最后一次弹出的对话框
     * ```
     * AppJs.dialog.hideLoading();
     * ```*/
    @JavascriptInterface
    fun hideLoading() {
        com.angcyo.dialog.hideLoading {
            //no op
        }
    }

    //endregion ---loading---

    //region ---message---

    /**显示一个对话框提示
     * ```
     * AppJs.dialog.message("title", "message");
     * ```
     * */
    @JavascriptInterface
    fun message(title: String?, message: String?) {
        onMain {
            lastContext.messageDialog {
                dialogTitle = title
                dialogMessage = message
            }
        }
    }

    //endregion ---message---

    //region ---dialog---

    /**显示一个简单的图片对话框
     * [url] 图片的地址
     * [cancel] 是否可以取消*/
    @JavascriptInterface
    fun singleImageDialog(url: String?, cancel: Boolean, openUrl: String?) {
        url ?: return
        onMain {
            lastContext.singleImageDialog(url.toUri()) {
                cancelable = cancel
                this.openUrl = openUrl
            }
        }
    }

    /**版本更新对话框*/
    @JavascriptInterface
    fun versionUpdateDialog(jsObject: JSObject) {
        onMain {
            val bean = VersionUpdateBean()
            for (key in jsObject.keys) {
                try {
                    when (key) {
                        "versionCode" -> bean.versionCode = jsObject.getOrLong(key)
                        "versionType" -> bean.versionType = jsObject.getOrInt(key)
                        "versionName" -> bean.versionName = jsObject.getOrString(key)
                        "versionDesTip" -> bean.versionDesTip = jsObject.getOrString(key)
                        "versionDes" -> bean.versionDes = jsObject.getOrString(key)
                        "versionUrl" -> bean.versionUrl = jsObject.getOrString(key)
                        "link" -> bean.link = jsObject.getOrBoolean(key)
                        "toMarketDetails" -> bean.toMarketDetails = jsObject.getOrBoolean(key)
                        "versionForce" -> bean.versionForce = jsObject.getOrBoolean(key)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            lastContext.versionUpdate(bean)
        }
    }

    /**显示一个App被禁用的对话框
     * [versionRange] 需要禁用的版本范围 [com.angcyo.library.component.VersionMatcher.matches] "*" 表示所有版本
     * [reason] 原因
     * */
    @JavascriptInterface
    fun forbiddenDialog(versionRange: String, reason: String) {
        onMain {
            val bean = VersionUpdateBean()
            bean.forbiddenVersionRange = versionRange
            bean.forbiddenReason = reason
            lastContext.versionUpdate(bean)
        }
    }

    //endregion ---dialog---

}