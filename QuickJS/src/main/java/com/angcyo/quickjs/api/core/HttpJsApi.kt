package com.angcyo.quickjs.api.core

import android.webkit.JavascriptInterface
import androidx.annotation.Keep
import androidx.core.net.toUri
import com.angcyo.http.DslHttp
import com.angcyo.http.bodyString
import com.angcyo.http.jsonBody
import com.angcyo.library.ex.string
import com.angcyo.library.ex.toStr
import com.angcyo.quickjs.api.IJSInterface
import com.angcyo.quickjs.api.toHeaders
import com.angcyo.quickjs.api.toJsonElement
import com.quickjs.JSArray
import com.quickjs.JSFunction
import com.quickjs.JSObject
import okhttp3.Request

/**
 * http请求api, 网络请求
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/06/11
 */
@Keep
class HttpJsApi : IJSInterface {

    override val interfaceName: String = "http"

    /**
     * 进行get网络请求, 返回字符串数据
     * ```
     * AppJs.http.get(
     *     "https://server.hingin.com/login",
     *     null,
     *     null,
     *     (body, error) => {
     *         AppJs.L.i("返回:" + body);
     *         AppJs.L.e("错误:" + error);
     *         if (error == null) {
     *             AppJs.T.showQQ(body);
     *         }
     *     }
     * );
     * ```
     * */
    @JavascriptInterface
    fun get(url: String, header: JSObject?, query: JSObject?, onEnd: JSFunction?) {
        var bodyString: String? = null
        var errorString: String? = null
        try {
            //如果url不是 网址, 会报错
            val newUrl = url.toUri().buildUpon().apply {
                query?.let {
                    for (key in it.keys) {
                        try {
                            appendQueryParameter(key, it.getString(key))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }.build().toStr()
            val request = Request.Builder().url(newUrl)
                .method("GET", null)
                .apply {
                    header?.toHeaders()?.let { headers(it) }
                }
                .build()
            val call = DslHttp.client.newCall(request)
            val response = call.execute()
            bodyString = response.bodyString()
        } catch (e: Exception) {
            e.printStackTrace()
            errorString = e.string()
        }
        onEnd?.call(onEnd, JSArray(onEnd.context).apply {
            push(bodyString)
            push(errorString)
        })
    }

    /**进行post网络请求, 返回字符串数据
     * ```
     * AppJs.http.postJson(
     *         "https://server.hingin.com/login",
     *         null,
     *         {
     *             email: "angcyo@126.com",
     *             credential: "angcyo",
     *         },
     *         (body, error) => {
     *             AppJs.L.i("返回:" + body);
     *             AppJs.L.e("错误:" + error);
     *             if (error == null) {
     *                 let data = JSON.parse(body);
     *                 if (data.code == 200) {
     *                     AppJs.T.showQQ("登录成功");
     *                 } else {
     *                     AppJs.T.showQQ(data.errMsg);
     *                 }
     *             }
     *         }
     *     );
     * ```
     *
     * All QuickJS methods must be called on the same thread.
     * Invalid QuickJS thread access: current thread is Thread[main,5,main] while the locker has thread Thread[RxCachedThreadScheduler-2,5,main]
     * */
    @JavascriptInterface
    fun postJson(url: String, header: JSObject?, body: JSObject?, onEnd: JSFunction?) {
        var bodyString: String? = null
        var errorString: String? = null
        try {
            //如果url不是 网址, 会报错
            val request = Request.Builder().url(url)
                .method("POST", jsonBody(body?.toJsonElement()?.toStr() ?: ""))
                .apply {
                    header?.toHeaders()?.let { headers(it) }
                }
                .build()
            val call = DslHttp.client.newCall(request)
            val response = call.execute()
            bodyString = response.bodyString()
        } catch (e: Exception) {
            e.printStackTrace()
            errorString = e.string()
        }
        onEnd?.call(onEnd, JSArray(onEnd.context).apply {
            push(bodyString)
            push(errorString)
        })

        /*syncSingle {
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
                val bodyString = data?.body()?.readString(true)
                val errorString = if (error == null) {
                    null
                } else {
                    data?.errorBody()?.readString(true) ?: error.message
                }
                onEnd?.call(onEnd, JSArray(onEnd.context).apply {
                    push(bodyString)
                    push(errorString)
                })
                //
                it.countDown()
            }
        }*/
    }

}