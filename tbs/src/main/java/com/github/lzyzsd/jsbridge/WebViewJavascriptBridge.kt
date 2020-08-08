package com.github.lzyzsd.jsbridge

interface WebViewJavascriptBridge {
    fun sendToWeb(data: Any?)
    fun sendToWeb(data: Any?, responseCallback: OnBridgeCallback?)
    fun sendToWeb(function: String?, vararg values: Any?)
}