package com.angcyo.jsoup

import com.angcyo.coroutine.CoroutineErrorHandler
import com.angcyo.coroutine.launchSafe
import com.angcyo.coroutine.onBack
import com.angcyo.library.L
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import kotlin.coroutines.CoroutineContext

/**
 * https://jsoup.org/cookbook/extracting-data/selector-syntax
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/03/14
 */

class DslJsoup : CoroutineScope {

    /**需要解析的url*/
    var url: String? = null

    /**获取url数据的方法[GET] or [POST]*/
    var method: Connection.Method = Connection.Method.GET

    /**协程域*/
    var scope: CoroutineScope = GlobalScope

    /**文档准备完成, 协程线程回调*/
    var onDocumentReady: suspend (document: Document) -> Unit = {

    }

    /**主线程回调*/
    var onErrorAction: suspend (exception: Throwable) -> Unit = {
        L.e("异常->")
        it.printStackTrace()
    }

    //获取文档
    fun _document(): Document {
        val connect = Jsoup.connect(url).apply {
            //userAgent()
            //超时, 模式30秒
            timeout(30_000)
            ignoreHttpErrors(true)
            ignoreContentType(true)
        }
        return when (method) {
            Connection.Method.POST -> connect.post()
            else -> connect.get()
        }
    }

    /**执行*/
    fun doIt() {
        scope.launchSafe(Dispatchers.Main + CoroutineErrorHandler {
            scope.launchSafe {
                onErrorAction(it)
            }
        }) {
            onBack {
                onDocumentReady(_document())
            }
        }
    }

    override val coroutineContext: CoroutineContext
        get() = scope.coroutineContext
}

fun dslJsoup(action: DslJsoup.() -> Unit): DslJsoup {
    return DslJsoup().apply {
        action()
        doIt()
    }
}