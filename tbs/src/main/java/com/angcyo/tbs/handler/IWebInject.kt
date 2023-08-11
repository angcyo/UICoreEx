package com.angcyo.tbs.handler

import androidx.fragment.app.Fragment
import com.angcyo.library.annotation.CallPoint
import com.angcyo.tbs.core.inner.TbsWebView

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/08/11
 */
interface IWebInject {

    /**开始注入*/
    @CallPoint
    fun inject(fragment: Fragment, webView: TbsWebView)

}