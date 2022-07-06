package com.angcyo.objectbox.laser.pecker.entity

import androidx.annotation.Keep
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

/**
 * 雕刻历史记录表
 *
 * 数据说明参考:
 * [com.angcyo.engrave.data.EngraveDataInfo]
 *
 * 雕刻参数参考:
 * [com.angcyo.engrave.data.EngraveOptionInfo]
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/07/05
 */

@Keep
@Entity
data class EngraveHistoryEntity(
    @Id var entityId: Long = 0L,

    /**[com.angcyo.engrave.data.EngraveDataInfo]*/

    var dataType: Int = -1,
    var width: Int = -1,
    var height: Int = -1,
    var x: Int = -1,
    var y: Int = -1,
    var px: Byte = -1,

    var name: String? = null,
    var index: Int? = null,
    var lines: Int = -1,

    /**[com.angcyo.engrave.data.EngraveReadyDataInfo]*/

    var optionMode: Int? = null,

    var dataPath: String? = null,
    var previewDataPath: String? = null,

    var startEngraveTime: Long = -1,
    var stopEngraveTime: Long = -1,
    var printTimes: Int = 0,

    /**打印耗时, 总时长/打印次数. 毫秒*/
    var duration: Long = -1,

    /**[com.angcyo.engrave.data.EngraveOptionInfo]*/

    var material: String? = null,
    var power: Byte = -1,
    var depth: Byte = -1,
    var type: Byte = -1,
)
