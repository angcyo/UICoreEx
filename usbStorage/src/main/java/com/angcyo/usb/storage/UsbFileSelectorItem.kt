package com.angcyo.usb.storage

import com.angcyo.core.dslitem.DslFileSelectorItem
import com.angcyo.library.ex.size
import me.jahnen.libaums.core.fs.UsbFile
import java.io.File

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/09/23
 */
class UsbFileSelectorItem : DslFileSelectorItem() {

    /**U盘文件*/
    val itemUsbFile: UsbFile?
        get() = itemData as? UsbFile

    override fun getItemFileName(file: File?): CharSequence? {
        return itemUsbFile?.name
    }

    override fun getItemFileLastModified(file: File?): Long? {
        return itemUsbFile?.lastModified()
    }

    override fun getSubFileCount(): Long {
        return itemUsbFile?.run { if (isDirectory) list().size().toLong() else 0L } ?: 0L
    }

    override fun getItemFileLength(file: File?) = itemUsbFile?.length

    override fun itemIsFile(file: File?): Boolean {
        return !itemIsFolder(file)
    }

    override fun itemIsFolder(file: File?): Boolean {
        return itemUsbFile?.isDirectory == true
    }

    override fun itemCanExecute(file: File?): Boolean {
        return !itemIsFolder(file)
    }

    override fun itemCanRead(file: File?): Boolean {
        return true
    }

    override fun itemCanWrite(file: File?): Boolean {
        return true
    }
}