package com.hjhrq1991.library.tbs

/**
 * @author hjhrq1991 created at 8/22/16 14 41.
 * 配置文件
 */
object BridgeConfig {

    /**需要加载桥接js文件*/
    const val toLoadJs = "WebViewJavascriptBridge.js"

    /**
     * 默认桥名
     */
    const val defaultJs = "WebViewJavascriptBridge"

    /**
     * 自定义桥名, 代码中使用此对象调用
     */
    var customJs = "androidJs"
}