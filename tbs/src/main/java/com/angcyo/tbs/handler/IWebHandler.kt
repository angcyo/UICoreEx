package com.angcyo.tbs.handler

import androidx.fragment.app.Fragment
import com.angcyo.tbs.core.inner.TbsWebView
import com.hjhrq1991.library.tbs.CallBackFunction

/**
 * 注入到TBS内核中的方法
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/08/11
 */
interface IWebHandler {

    /**注入的处理方法名称*/
    val handlerName: String

    /**开始处理*/
    fun onHandler(
        fragment: Fragment,
        webView: TbsWebView,
        data: String?,
        function: CallBackFunction?
    )

}