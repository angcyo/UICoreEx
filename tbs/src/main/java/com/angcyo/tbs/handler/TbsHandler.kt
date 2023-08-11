package com.angcyo.tbs.handler

import androidx.fragment.app.Fragment
import com.angcyo.library.annotation.CallPoint
import com.angcyo.tbs.core.inner.TbsWebView

/**
 * 注入到TBS内核中的方法
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/08/11
 */
object TbsHandler {

    /**注入列表*/
    val injectList = mutableListOf<IWebInject>().apply {
        add(CoreInject())
    }

    /**注入的处理器列表*/
    val handlerList = mutableListOf<IWebHandler>()

    /**注入入口
     *
     * [com.hjhrq1991.library.tbs.BridgeConfig.customJs]
     * */
    @CallPoint
    fun inject(fragment: Fragment, webView: TbsWebView) {
        TbsImagePager.register(fragment, webView)

        //注入器
        injectList.forEach {
            try {
                it.inject(fragment, webView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        //处理器
        handlerList.forEach {
            try {
                webView.registerHandler(it.handlerName) { data, function ->
                    it.onHandler(fragment, webView, data, function)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    }

}