package com.angcyo.wifip2p.data

import com.angcyo.library.component.Speed
import com.angcyo.wifip2p.task.WifiP2pProgressListener.Companion.FINISH_CANCEL
import com.angcyo.wifip2p.task.WifiP2pProgressListener.Companion.FINISH_ERROR
import com.angcyo.wifip2p.task.WifiP2pProgressListener.Companion.FINISH_SUCCESS

/**
 * 数据传输的进度信息
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/08/11
 */
data class ProgressInfo(
    /**目标设备mac地址*/
    val deviceAddress: String?,
    /**传输状态
     * [FINISH_CANCEL]
     * [FINISH_SUCCESS]
     * [FINISH_ERROR]
     * */
    val state: Int,
    /**传输进度[0~100]*/
    val progress: Int,
    /**传输速率时间, 统计*/
    val speed: Speed,
    /**异常时的信息*/
    val exception: Exception? = null
)
