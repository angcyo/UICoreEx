package com.angcyo.engrave.data

/**
 * 数据传输状态数据结构
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/29
 */
data class TransferTaskStateData(
    /**任务id*/
    val taskId: String?,
    /**传输总进度[0~100]*/
    val progress: Int = 0,
    /**传输是否有异常信息*/
    val error: Throwable? = null,
    /**是否传输完成*/
    val isFinish: Boolean = false
)
