package com.angcyo.canvas2.laser.pecker.manager

import com.angcyo.laserpacker.bean.LPProjectBean
import java.io.File

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/07/18
 */
data class ShareProjectInfo(
    val taskId: String?,
    val projectFile: File,
    val previewFile: File,
    val bean: LPProjectBean
)
