package com.angcyo.bluetooth.fsc.laserpacker

import com.angcyo.bluetooth.fsc.R
import com.angcyo.bluetooth.fsc.ReceivePacket
import com.angcyo.library.annotation.CallPoint
import com.angcyo.library.ex._string
import com.angcyo.library.ex.nowTime
import com.angcyo.library.toastQQ

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/01/12
 */
object LaserPeckerCmdStatistics {

    /**指令最后一次错误的时间*/
    var lastErrorTime: Long = 0

    /**指令连续错误的此时*/
    var errorCount: Long = 0

    @CallPoint
    fun onReceive(bean: ReceivePacket?, error: Exception?) {
        if (error == null) {
            errorCount = 0
        } else {
            val nowTime = nowTime()
            if (nowTime - lastErrorTime < 10 * 60 * 1000L) {
                //10分钟之内的错误统计
                errorCount++
            } else {
                errorCount = 0
            }
            lastErrorTime = nowTime

            if (errorCount > 3) {
                errorCount = 0

                toastQQ(_string(R.string.command_send_error_tip))
            }
        }
    }

}