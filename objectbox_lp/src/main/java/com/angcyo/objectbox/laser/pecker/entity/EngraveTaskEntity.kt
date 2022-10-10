package com.angcyo.objectbox.laser.pecker.entity

import androidx.annotation.Keep
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

/**
 * 雕刻任务总管理
 *
 * 管理任务下, 所有的雕刻索引, 所有的耗时时长, 总进度等
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/10/09
 */

@Keep
@Entity
data class EngraveTaskEntity(
    @Id var entityId: Long = 0L,

    /**当前雕刻任务的id*/
    var taskId: String? = null,

    /**当前正在雕刻的数据索引*/
    var currentIndex: Int = -1,

    /**任务需要雕刻的索引集合,
     * 数据库只支持[List<String>]类型, 不支持[List<Int>]类型
     * */
    var dataIndexList: List<String>? = null,

    //---

    /**
     * 雕刻任务的状态
     * [com.angcyo.engrave.model.EngraveModel.ENGRAVE_STATE_START]
     * [com.angcyo.engrave.model.EngraveModel.ENGRAVE_STATE_PAUSE]
     * [com.angcyo.engrave.model.EngraveModel.ENGRAVE_STATE_FINISH]
     * */
    var state: Int = 0,

    /**任务开始的时间, 毫秒*/
    var startTime: Long = -1,

    /**任务完成的时间, 毫秒*/
    var finishTime: Long = -1,
)
