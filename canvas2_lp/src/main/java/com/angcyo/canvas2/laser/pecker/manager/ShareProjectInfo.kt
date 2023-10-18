package com.angcyo.canvas2.laser.pecker.manager

import com.angcyo.laserpacker.bean.LPProjectBean
import java.io.File

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/07/18
 */
data class ShareProjectInfo(
    val taskId: String?,
    /**工程文件路径*/
    val projectFile: File,
    /**工程预览文件路径*/
    val previewFile: File,
    /**工程内容结构数据*/
    val bean: LPProjectBean,
    /**工程名称, 如果有*/
    val projectName: CharSequence? = null
)
