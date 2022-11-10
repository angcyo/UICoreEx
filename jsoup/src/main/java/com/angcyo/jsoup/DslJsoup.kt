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
import org.jsoup.helper.HttpConnection
import org.jsoup.internal.StringUtil
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import java.net.URL
import kotlin.coroutines.CoroutineContext

/**
 * https://jsoup.org/cookbook/extracting-data/selector-syntax
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/03/14
 */

class DslJsoup : CoroutineScope {

    companion object {
        const val DEFAULT_UA =
            "Mozilla/5.0 (Macintosh; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.132 Safari/537.36 chrome-extension"
    }

    /**需要解析的url*/
    var url: String? = null

    /**获取url数据的方法[GET] or [POST]*/
    var method: Connection.Method = Connection.Method.GET

    var connectTimeOut = 10_000

    /**协程域*/
    var scope: CoroutineScope = GlobalScope

    var onConfigConnection: (Connection) -> Unit = {}

    /**文档准备完成, 协程线程回调*/
    var onDocumentReady: suspend (document: Document) -> Unit = {

    }

    /**主线程回调*/
    var onErrorAction: suspend (exception: Throwable) -> Unit = {
        L.e("异常->")
        it.printStackTrace()
    }

    var _document: Document? = null

    //获取文档
    fun _document(): Document {
        val connect = Jsoup.connect(url).apply {
            userAgent(DEFAULT_UA)
            header(HttpConnection.CONTENT_ENCODING, "gzip")
            header(HttpConnection.CONTENT_TYPE, "text/html;charset=utf-8")
            header(
                "accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9"
            )
            //header("accept-encoding", "gzip, deflate, br")//会出现乱码
            header("accept-language", "zh-CN,zh;q=0.9,en;q=0.8")
            header("cache-control", "max-age=0")

            header("dnt", "1")
            header("sec-fetch-dest", "document")
            header("sec-fetch-mode", "navigate")
            header("sec-fetch-site", "none")
            header("sec-fetch-user", "?1")
            header("upgrade-insecure-requests", "1")

            //超时, Jsoup默认是30秒
            timeout(connectTimeOut)
            ignoreHttpErrors(true)
            ignoreContentType(false)

            onConfigConnection(this)
        }
        return when (method) {
            Connection.Method.POST -> connect.post()
            else -> connect.get()
        }
    }

    /**查询*/
    fun select(cssQuery: String?): Elements? {
        if (cssQuery.isNullOrBlank()) {
            return null
        }
        return _document?.select(cssQuery)
    }

    /**执行*/
    fun doIt() {
        scope.launchSafe(Dispatchers.Main + CoroutineErrorHandler { context, exception ->
            scope.launchSafe {
                onErrorAction(exception)
            }
        }) {
            onBack {
                val document = _document()
                _document = document
                onDocumentReady(document)
            }
        }
    }

    override val coroutineContext: CoroutineContext
        get() = scope.coroutineContext
}

fun String.toAbsUrl(baseUrl: URL): URL? {
    return try {
        StringUtil.resolve(baseUrl, this)
    } catch (e: Exception) {
        null
    }
}

fun String.toAbsUrl(baseUrl: String): String? {
    return try {
        StringUtil.resolve(baseUrl, this)
    } catch (e: Exception) {
        null
    }
}

fun dslJsoup(action: DslJsoup.() -> Unit): DslJsoup {
    return DslJsoup().apply {
        action()
        doIt()
    }
}

fun dslJsoup(url: String?, onReady: suspend (Document) -> Unit): DslJsoup {
    return dslJsoup {
        this.url = url
        onDocumentReady = onReady
    }
}