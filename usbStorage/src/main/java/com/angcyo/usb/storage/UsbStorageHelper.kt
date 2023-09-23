package com.angcyo.usb.storage

import androidx.annotation.AnyThread
import com.angcyo.http.rx.doMain
import com.angcyo.library.L
import com.angcyo.library.component.runOnBackground
import com.angcyo.library.ex.toListOf
import me.jahnen.libaums.core.fs.FileSystem
import me.jahnen.libaums.core.fs.UsbFile
import me.jahnen.libaums.core.fs.UsbFileStreamFactory
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.util.concurrent.atomic.AtomicLong

/**
 * usb存储
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/09/22
 */
object UsbStorageHelper {

    /**加载U盘文件*/
    @AnyThread
    fun loadUsbFile(file: UsbFile, action: (List<UsbFile>?) -> Unit) {
        runOnBackground {
            if (file.isDirectory) {
                doMain {
                    action(file.listFiles().toList())
                }
            } else {
                doMain {
                    action(file.toListOf())
                }
            }
        }
    }

    //---

    /**写入数据到u盘文件[UsbFile]*/
    fun UsbFile.write(fileSystem: FileSystem?, action: BufferedOutputStream.() -> Unit) {
        fileSystem ?: return
        UsbFileStreamFactory.createBufferedOutputStream(this, fileSystem).use {
            it.action()
        }
    }

    fun UsbFile.write(fileSystem: FileSystem?, file: File) {
        fileSystem ?: return
        write(fileSystem) {
            file.inputStream().use { inputStream ->
                inputStream.copyTo(this)
            }
        }
    }

    //---

    /**删除文件/文件夹如果存在*/
    fun UsbFile.deleteIfExist(fileName: String): Boolean {
        if (isDirectory) {
            val file = search(fileName)
            file?.delete()
            return true
        }
        return false
    }

    /**获取或者创建一个文件夹, 并返回*/
    fun UsbFile.getOrCreateFolder(folderName: String): UsbFile {
        if (isDirectory) {
            val file = search(folderName)
            if (file == null) {
                return createDirectory(folderName)
            } else if (file.isDirectory) {
                return file
            } else {
                file.delete()
                return createDirectory(folderName)
            }
        } else {
            return this
        }
    }

    /**在指定文件夹中创建一个新的文件[newFileName]并写入数据*/
    fun UsbFile.writeNewFile(
        fileSystem: FileSystem?,
        newFileName: String,
        action: BufferedOutputStream.() -> Unit
    ): Boolean {
        fileSystem ?: return false
        if (isDirectory) {
            //判断文件是否已存在
            return try {
                val newFile = search(newFileName)
                newFile?.delete()
                createFile(newFileName).write(fileSystem, action)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                L.e(e)
                false
            }
        } else {
            return false
        }
    }

    fun UsbFile.writeNewFile(fileSystem: FileSystem?, file: File) {
        fileSystem ?: return
        writeNewFile(fileSystem, file.name) {
            file.inputStream().use { inputStream ->
                inputStream.copyTo(this)
            }
        }
    }

    //---

    /**读取文件数据流*/
    fun UsbFile.inputStream(fileSystem: FileSystem?): BufferedInputStream? {
        fileSystem ?: return null
        return UsbFileStreamFactory.createBufferedInputStream(this, fileSystem)
    }

    //---

    /**递归拷贝文件数据
     * [count] 已拷贝文件数
     * [action] 已经拷贝了多少个回调的回调*/
    fun UsbFile.recursivelyCopy(
        fileSystem: FileSystem?,
        file: File,
        count: AtomicLong = AtomicLong(0),
        action: (file: File, count: Long) -> Unit = { _, _ -> }
    ) {
        if (file.isDirectory) {
            val folder = getOrCreateFolder(file.name)
            file.listFiles()?.forEach {
                folder.recursivelyCopy(fileSystem, it, count, action)
            }
        } else {
            action(file, count.incrementAndGet())
            writeNewFile(fileSystem, file)
        }
    }
}