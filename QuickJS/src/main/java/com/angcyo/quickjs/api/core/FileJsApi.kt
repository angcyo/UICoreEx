package com.angcyo.quickjs.api.core

import android.webkit.JavascriptInterface
import androidx.annotation.Keep
import com.angcyo.library.ex.deleteRecursivelySafe
import com.angcyo.library.ex.file
import com.angcyo.library.ex.shareFile
import com.angcyo.library.ex.writeText
import com.angcyo.library.libAppFile
import com.angcyo.library.libCacheFile
import com.angcyo.library.libFile
import com.angcyo.library.utils.appFolderPath
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

    /**删除指令路径文件
     * 支持删除文件
     * 支持删除文件夹, 支持递归删除
     * */
    @JavascriptInterface
    fun deleteFile(path: String): Boolean = path.file().deleteRecursivelySafe()

    /**指定的文件是否存在*/
    @JavascriptInterface
    fun exists(path: String): Boolean = path.file().exists()

    /**获取文件列表*/
    @JavascriptInterface
    fun listFiles(path: String?): JSArray {
        val result = JSArray(jsContext)
        (path ?: appFolderPath()).file().listFiles()?.forEach {
            result.push(it.absolutePath)
        }
        return result
    }

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

    //---

    /**分享文件
     * ```
     * AppJs.file.shareFile(path);
     * ```
     * */
    @JavascriptInterface
    fun shareFile(path: String) = path.file().shareFile()

    //---

    private fun _writeCacheFile(name: String, text: String, append: Boolean): String? {
        return try {
            val file = libCacheFile(name)
            file.writeText(text, append)
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private fun _writeAppFile(name: String, text: String, append: Boolean): String? {
        return try {
            val file = libAppFile(name)
            file.writeText(text, append)
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    /**写入缓存文件
     *
     * @return 承购返回对应文件路径, 否则返回null*/
    @JavascriptInterface
    fun writeCacheFile(name: String, text: String): String? {
        return _writeCacheFile(name, text, false)
    }

    /**追加写入缓存文件*/
    @JavascriptInterface
    fun appendCacheFile(name: String, text: String): String? {
        return _writeCacheFile(name, text, true)
    }

    /**写入files文件*/
    @JavascriptInterface
    fun writeAppFile(name: String, text: String): String? {
        return _writeAppFile(name, text, false)
    }

    /**追加写入files文件*/
    @JavascriptInterface
    fun appendAppFile(name: String, text: String): String? {
        return _writeAppFile(name, text, true)
    }

}