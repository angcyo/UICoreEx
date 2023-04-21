package com.angcyo.objectbox.laser.pecker.entity

import androidx.annotation.Keep
import com.angcyo.library.ex.fileSizeString
import com.angcyo.library.ex.toMsTime
import com.angcyo.library.ex.toSizeString
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

/**
 * 传输监视表
 * 用来记录数据创建时间/耗时
 * 数据传输速率/时间/耗时
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/10/15
 */

@Keep
@Entity
data class TransferMonitorEntity(
    @Id var entityId: Long = 0L,

    /**当前雕刻任务的id*/
    var taskId: String? = null,

    //---transfer

    /**数据创建的开始时间, 13位毫秒时间*/
    var dataMakeStartTime: Long = -1,
    var dataMakeFinishTime: Long = -1,

    //--send

    /**数据传输的开始时间, 13位毫秒时间*/
    var dataTransferStartTime: Long = -1,
    var dataTransferFinishTime: Long = -1,

    /**所有数据传输的总大小, 字节byte数量*/
    var dataTransferSize: Long = 0,
    /**数据传输总进度[0~100]*/
    var dataTransferProgress: Int = 0,
    /**数据发送速率 byte/s*/
    var dataTransferSpeed: Float = -1f,

    /**传输平均缩率 byte/s*/
    var dataAverageTransferSpeed: Float = -1f,

    /**数据发送最高的速率 byte/s*/
    var dataTransferMaxSpeed: Float = -1f,
) {

    /**数据大小*/
    fun dataSize(): String {
        if (dataTransferSize < 0) {
            return ""
        }
        return dataTransferSize.fileSizeString()
    }

    /**传输速率*/
    fun speedString(): String {
        if (dataTransferSpeed < 0) {
            return ""
        }
        return "${dataTransferSpeed.toLong().toSizeString()}/s"
    }

    /**最大传输速率*/
    fun maxSpeedString(): String {
        if (dataTransferMaxSpeed < 0) {
            return ""
        }
        return "${dataTransferMaxSpeed.toLong().toSizeString()}/s"
    }

    /**平均传输速率*/
    fun averageSpeedString(): String {
        if (dataAverageTransferSpeed < 0) {
            return ""
        }
        return "${dataAverageTransferSpeed.toLong().toSizeString()}/s"
    }

    /**数据生成耗时*/
    fun dataMakeDuration(): String {
        return (dataMakeFinishTime - dataMakeStartTime).toMsTime()!!
    }

    /**数据传输耗时*/
    fun dataTransferDuration(endTime: Long = dataTransferFinishTime): String {
        return (endTime - dataTransferStartTime).toMsTime()!!
    }

    /**通过平均速率总大小, 计算出剩余时间, 毫秒*/
    fun timeRemaining(): Long {
        /*if (!dataAverageTransferSpeed.isFinite() || dataAverageTransferSpeed <= 0) {
            return -1
        }
        return ((dataTransferSize * (100 - dataTransferProgress) / 100f) / dataAverageTransferSpeed).toLong()*/

        if (!dataTransferSpeed.isFinite() || dataTransferSpeed <= 0) {
            return -1
        }
        return ((dataTransferSize * (100 - dataTransferProgress) / 100f) / dataTransferSpeed * 1000).toLong()
    }

    /**剩余耗时*/
    fun timeRemainingDuration(): String {
        val timeRemaining = timeRemaining()
        if (timeRemaining <= 0) {
            return ""
        }
        return timeRemaining.toMsTime()!!
    }

}
