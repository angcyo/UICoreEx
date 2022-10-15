package com.angcyo.objectbox.laser.pecker.entity

import androidx.annotation.Keep
import com.angcyo.library.ex.fileSizeString
import com.angcyo.library.ex.toElapsedTime
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
) {

    /**数据大小*/
    fun dataSize(): String {
        return dataTransferSize.fileSizeString()
    }

    /**传输速率*/
    fun speedString(): String {
        return "${dataTransferSpeed.toLong().toSizeString()}/s"
    }

    /**数据生成耗时*/
    fun dataMakeDuration(): String {
        return (dataMakeFinishTime - dataMakeStartTime).toElapsedTime(intArrayOf(1, 1))
    }

    /**数据传输耗时*/
    fun dataTransferDuration(endTime: Long = dataTransferFinishTime): String {
        return (endTime - dataTransferStartTime).toElapsedTime(intArrayOf(1, 1))
    }

}
