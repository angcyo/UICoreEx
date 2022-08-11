package com.angcyo.wifip2p.task

import androidx.annotation.WorkerThread
import com.angcyo.library.component.Speed

/**
 * 传输进度监听
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/08/10
 */
@WorkerThread
interface WifiP2pProgressListener {

    companion object {
        /**被取消*/
        const val FINISH_CANCEL = -1

        /**异常结束*/
        const val FINISH_ERROR = -2

        /**正常完成*/
        const val FINISH_SUCCESS = 1
    }

    /**传输进度
     * [progress] 0~100
     * [speed] 每秒传输的字节数
     * [size] 总长度, -1表示未获取到长度*/
    @WorkerThread
    fun onProgress(progress: Int, speed: Speed, size: Long)

    /**传输完成, 发送或者接收
     * [exception]异常结束时的信息
     * [FINISH_CANCEL]
     * [FINISH_SUCCESS]
     * [FINISH_ERROR]
     * */
    @WorkerThread
    fun onFinish(reason: Int, exception: Exception?)

}