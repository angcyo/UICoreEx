package com.angcyo.quickjs.api.core

import android.webkit.JavascriptInterface
import com.angcyo.http.base.jsonObject
import com.angcyo.http.base.readString
import com.angcyo.http.post2Body
import com.angcyo.http.rx.observe
import com.angcyo.library.ex.syncSingle
import com.angcyo.library.ex.toStr
import com.angcyo.quickjs.api.IJSInterface
import com.quickjs.JSObject

/**
 * http请求api, 网络请求
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/06/11
 */
class HttpJsApi : IJSInterface {

    override val interfaceName: String = "http"

    /**进行post网络请求, 返回字符串数据
     * ```
     * let result = AppJs.http.postJson("https://www.baidu.com", null, null);
     * ```
     * */
    @JavascriptInterface
    fun postJson(url: String, header: JSObject?, body: JSObject?): String? {
        var result: String? = null
        syncSingle {
            post2Body {
                this.url = url
                this.header = hashMapOf<String, String>().apply {
                    header?.keys?.forEach { key ->
                        header.get(key)?.let {
                            this[key] = it.toStr()
                        }
                    }
                }
                this.body = jsonObject {
                    body?.keys?.forEach { key ->
                        body.get(key)?.let {
                            add(key, it)
                        }
                    }
                }
            }.observe { data, error ->
                result = if (error == null) {
                    data?.body()?.readString(true)
                } else {
                    data?.errorBody()?.readString(true) ?: error.message
                }
                //
                it.countDown()
            }
        }
        return result
    }

}