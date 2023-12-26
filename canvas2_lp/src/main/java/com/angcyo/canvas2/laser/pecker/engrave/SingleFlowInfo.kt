package com.angcyo.canvas2.laser.pecker.engrave

import com.angcyo.bluetooth.fsc.laserpacker.bean.FileIndexBean

/**
 * 简单的流程信息
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/08/07
 */
data class SingleFlowInfo(
    /**流程任务id*/
    val flowId: String,
    /**文件名*/
    val fileBean: FileIndexBean,
)
