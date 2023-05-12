package com.angcyo.laserpacker.project

import com.angcyo.http.base.toJson
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.laserpacker.bean.LPProjectBean
import com.angcyo.laserpacker.toProjectBean
import com.angcyo.library.ex.file
import com.angcyo.library.ex.readEntryBitmap
import com.angcyo.library.ex.readEntryString
import com.angcyo.library.ex.readText
import com.angcyo.library.ex.renameKeepExt
import com.angcyo.library.ex.replaceZipEntry
import com.angcyo.library.ex.toBitmapOfBase64
import com.angcyo.library.ex.zipFileRead
import com.angcyo.library.utils.writeTo
import java.io.File

/**
 * 工程管理, 用来实现保存/读取工程
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2023/04/12
 * Copyright (c) 2020 angcyo. All rights reserved.
 */
object LPProjectHelper {

    /**重命名工程的文件名*/
    fun renameProjectFileName(projectBean: LPProjectBean?, newName: String): Boolean {
        projectBean ?: return false
        val filePath = projectBean._filePath ?: return false
        val newFile = filePath.file().renameKeepExt(newName)
        projectBean._filePath = newFile?.absolutePath
        return newFile != null
    }

    /**重命名工程*/
    fun renameProjectName(projectBean: LPProjectBean?, newName: String): Boolean {
        projectBean ?: return false
        val filePath = projectBean._filePath ?: return false
        projectBean.file_name = newName
        return if (projectBean.version == 2) {
            //V2第二版结构
            filePath.file().replaceZipEntry {
                if (name == LPDataConstant.PROJECT_V2_DEFAULT_NAME) {
                    //替换工程结构数据
                    projectBean.toJson()
                } else {
                    null
                }
            }
        } else {
            projectBean.toJson()?.writeTo(
                filePath,
                false
            ) != null
        }
    }
}

/**读取项目结构[LPProjectBean], sub不读取, 支持V1/V2*/
fun File?.readProjectBean(): LPProjectBean? {
    val file = this ?: return null
    return if (file.name.endsWith(LPDataConstant.PROJECT_EXT2, true)) {
        var projectBean: LPProjectBean? = null
        zipFileRead(file.absolutePath) {
            projectBean =
                readEntryString(LPDataConstant.PROJECT_V2_DEFAULT_NAME)?.toProjectBean()
            projectBean?._previewImgBitmap =
                readEntryBitmap(LPDataConstant.PROJECT_V2_PREVIEW_NAME)
        }
        projectBean
    } else {
        val json = file.readText()
        json?.toProjectBean()?.apply {
            _previewImgBitmap = preview_img?.toBitmapOfBase64()
        }
    }?.apply {
        _filePath = file.absolutePath
    }
}
