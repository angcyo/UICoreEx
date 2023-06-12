package com.angcyo.quickjs.api.core

import android.webkit.JavascriptInterface
import androidx.annotation.Keep
import com.angcyo.library.ex.file
import com.angcyo.library.ex.writeText
import com.angcyo.library.libCacheFile
import com.angcyo.library.libFile
import com.angcyo.quickjs.api.BaseJSInterface
import com.angcyo.quickjs.api.toByteList
import com.quickjs.JSArray

/**
 * 文件操作相关api
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2023/06/12
 */
@Keep
class FileJsApi : BaseJSInterface() {

    override val interfaceName: String = "file"

    /**获取一个缓存文件的路径
     * ```
     * let path = AppJs.file.getCacheFilePath("temp.txt")
     * ```
     * */
    @JavascriptInterface
    fun getCacheFilePath(name: String?): String = libCacheFile(name ?: "").absolutePath

    /**获取一个App文件的路径*/
    @JavascriptInterface
    fun getFilePath(name: String?): String = libFile(name ?: "").absolutePath

    /**读取文件文本
     * ```
     * AppJs.file.readText(path);
     * ```
     * */
    @JavascriptInterface
    fun readText(path: String): String = path.file().readText()

    /**
     * ```
     * let bytes = AppJs.file.readBytes(path);
     * ```
     * */
    @JavascriptInterface
    fun readBytes(path: String): JSArray {
        val result = JSArray(jsContext)
        path.file().readBytes().forEach {
            result.push(it.toInt())
        }
        return result
    }

    /**
     * ```
     * AppJs.file.writeText(path, "temp")
     * ```
     * */
    @JavascriptInterface
    fun writeText(path: String, text: String): Boolean {
        return try {
            path.file().writeText(text, false)
            true
        } catch (e: Exception) {
            false
        }
    }

    @JavascriptInterface
    fun appendText(path: String, text: String): Boolean {
        return try {
            path.file().writeText(text, true)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * ```
     * AppJs.file.writeBytes(path, bytes);
     * ```
     * */
    @JavascriptInterface
    fun writeBytes(path: String, bytes: JSArray): Boolean {
        return try {
            path.file().writeBytes(bytes.toByteList().toByteArray())
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * ```
     * AppJs.file.appendBytes(path, bytes);
     * ```
     * */
    @JavascriptInterface
    fun appendBytes(path: String, bytes: JSArray): Boolean {
        return try {
            path.file().appendBytes(bytes.toByteList().toByteArray())
            true
        } catch (e: Exception) {
            false
        }
    }
}