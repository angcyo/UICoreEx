package com.hjhrq1991.library.tbs

interface WebViewJavascriptBridge {
    fun send(data: String?)
    fun send(data: String?, responseCallback: CallBackFunction?)
}