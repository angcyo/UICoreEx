package com.angcyo.usb.storage

import androidx.annotation.AnyThread
import com.angcyo.http.rx.doMain
import com.angcyo.library.component.runOnBackground
import com.angcyo.library.ex.toListOf
import me.jahnen.libaums.core.fs.FileSystem
import me.jahnen.libaums.core.fs.UsbFile
import me.jahnen.libaums.core.fs.UsbFileStreamFactory
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File

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

    /**在指定文件夹中创建一个新的文件[newFileName]并写入数据*/
    fun UsbFile.writeNewFile(
        fileSystem: FileSystem?,
        newFileName: String,
        action: BufferedOutputStream.() -> Unit
    ): Boolean {
        fileSystem ?: return false
        if (isDirectory) {
            //判断文件是否已存在
            val newFile = search(newFileName)
            newFile?.delete()
            createFile(newFileName).write(fileSystem, action)
            return true
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

}