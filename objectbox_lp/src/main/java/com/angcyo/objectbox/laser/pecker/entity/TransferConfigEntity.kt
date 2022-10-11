package com.angcyo.objectbox.laser.pecker.entity

import androidx.annotation.Keep
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

/**
 * 用来存储生成机器数据所需要的参数配置信息
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/10/09
 */

@Keep
@Entity
data class TransferConfigEntity(
    @Id var entityId: Long = 0L,

    /**当前雕刻任务的id*/
    var taskId: String? = null,

    //---

    /**
     * 雕刻需要显示的文件名, 28个字节
     * [com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd.DEFAULT_NAME_BYTE_COUNT]
     * [com.angcyo.engrave.transition.EngraveTransitionManager.Companion.generateEngraveName]
     * */
    var name: String = "",

    /**数据需要处理的分辨率
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.DEFAULT_PX]
     * */
    var px: Byte = 0,

    //---

    /**是否要合并相同类型的数据, 比如GCode数据, 线段数据等
     * 合并数据的总开关*/
    var mergeData: Boolean = true,

    /**是否要合并路径数据*/
    var mergeBpData: Boolean = false,

    /**是否要强制指定数据处理的模式
     * [com.angcyo.canvas.utils.CanvasConstant.DATA_MODE_BLACK_WHITE]
     * [com.angcyo.canvas.utils.CanvasConstant.DATA_MODE_GCODE]
     * [com.angcyo.canvas.utils.CanvasConstant.DATA_MODE_DITHERING]
     * */
    var dataMode: Int? = null,
)
