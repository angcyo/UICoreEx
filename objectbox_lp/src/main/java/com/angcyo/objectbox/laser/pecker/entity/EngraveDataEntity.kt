package com.angcyo.objectbox.laser.pecker.entity

import androidx.annotation.Keep
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

/**
 * 雕刻中一些信息实体
 *
 * 比如: 已经雕刻完成的索引
 * 雕刻索引开始的时间
 * 雕刻索引结束的时间
 * 雕刻索引当前雕刻的次数
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/10/09
 */

@Keep
@Entity
data class EngraveDataEntity(
    @Id var entityId: Long = 0L,

    /**当前雕刻任务的id*/
    var taskId: String? = null,

    /**
     * 雕刻数据的索引
     * [com.angcyo.objectbox.laser.pecker.entity.TransferDataEntity.index]*/
    var index: Int = -1,

    /**当前雕刻的次数, 从1开始
     * [com.angcyo.objectbox.laser.pecker.entity.EngraveConfigEntity.time] 总次数*/
    var printTimes: Int = 1,

    /**当前索引的雕刻进度[0~100]*/
    var progress: Int = 0,

    //---

    /**当前雕刻索引开始的时间, 毫秒*/
    var startTime: Long = -1,

    /**前雕刻索引完成的时间, 毫秒*/
    var finishTime: Long = -1,

    //---

    /**是否是来自设备历史的数据*/
    var isFromDeviceHistory: Boolean = false
)
