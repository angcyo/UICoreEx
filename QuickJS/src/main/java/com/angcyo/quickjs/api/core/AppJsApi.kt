package com.angcyo.quickjs.api.core

import android.webkit.JavascriptInterface
import androidx.annotation.Keep
import com.angcyo.library.component.RBackground
import com.angcyo.quickjs.api.IJSInterface

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/06/11
 */
@Keep
class AppJsApi : IJSInterface {

    override val interfaceName: String = "AppJs"

    /**App是否被切到后台
     * ```
     * AppJs.isBackground()
     * ```
     * */
    @JavascriptInterface
    fun isBackground(): Boolean {
        return RBackground.isBackground()
    }
}