package com.angcyo.quickjs.api.core

import android.webkit.JavascriptInterface
import androidx.activity.result.ActivityResultCaller
import androidx.annotation.Keep
import androidx.core.net.toUri
import com.angcyo.core.tgStrokeLoading
import com.angcyo.dialog.loadLoadingBottomCaller
import com.angcyo.dialog.messageDialog
import com.angcyo.dialog.other.singleImageDialog
import com.angcyo.library.component.lastContext
import com.angcyo.library.component.onMain
import com.angcyo.quickjs.api.BaseJSInterface

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
    fun singleImageDialog(url: String?, cancel: Boolean) {
        url ?: return
        onMain {
            lastContext.singleImageDialog(url.toUri()) {
                cancelable = cancel
            }
        }
    }

    //endregion ---dialog---

}