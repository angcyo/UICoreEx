package com.angcyo.engrave.data

import com.angcyo.engrave.data.TransferState.Companion.TRANSFER_STATE_FINISH

/**
 * 数据传输状态数据结构
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/29
 */
data class TransferState(
    /**任务id*/
    val taskId: String?,
    /**传输的状态*/
    var state: Int = TRANSFER_STATE_NORMAL,
    /**传输总进度[0~100]
     * 如果是-1时, 表示正在生成数据
     * */
    var progress: Int = 0,
    /**传输是否有异常信息, 此时的状态依旧是[TRANSFER_STATE_FINISH]*/
    var error: Throwable? = null,
) {
    companion object {
        const val TRANSFER_STATE_NORMAL = 0
        const val TRANSFER_STATE_FINISH = 2
        const val TRANSFER_STATE_CANCEL = 3
    }
}
