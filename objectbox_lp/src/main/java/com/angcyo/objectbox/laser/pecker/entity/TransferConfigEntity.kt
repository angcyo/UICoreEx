package com.angcyo.objectbox.laser.pecker.entity

import androidx.annotation.Keep
import com.angcyo.library.annotation.MM
import com.angcyo.objectbox.laser.pecker.bean.TransferLayerConfigBean
import com.angcyo.objectbox.laser.pecker.bean.getLayerConfigList
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

    /**单元素雕刻传输配置的数据索引*/
    var index: String? = null,

    //---

    /**
     * 雕刻需要显示的文件名, 28个字节
     * [com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd.DEFAULT_NAME_BYTE_COUNT]
     * [com.angcyo.engrave.transition.EngraveTransitionManager.Companion.generateEngraveName]
     * */
    var name: String = "",

    /**图层json数据, 对应每个图层的dpi
     *
     * 每英寸内像素点的个数
     * 设备基准值: 254, 像素点间距0.1mm 最小能达到:0.0125 8倍
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.DPI_254]
     *
     * [List<TransferLayerConfigBean>]
     * [com.angcyo.objectbox.laser.pecker.bean.TransferLayerConfigBean]
     * */
    var layerJson: String? = null,

    /**工程总共占用的宽高
     * [com.angcyo.objectbox.laser.pecker.entity.TransferDataEntity.originWidth]
     * [com.angcyo.objectbox.laser.pecker.entity.TransferDataEntity.originHeight]
     * */
    @MM
    var originWidth: Float? = null,
    @MM
    var originHeight: Float? = null,

    /**
     * 0：从左到右，从上到下，X轴扫描；
     * 1：从上到下，从左到右，Y轴扫描。（2023/7/25）
     *
     * [android.widget.LinearLayout.HORIZONTAL] :0
     * [android.widget.LinearLayout.VERTICAL]   :1
     * */
    var dataDir: Int = 0,

    //---

    /**是否要合并相同类型的数据, 比如GCode数据, 线段数据等
     * 合并数据的总开关*/
    var mergeData: Boolean = false,

    /**是否要合并路径数据*/
    var mergeBpData: Boolean = false,

    /**是否要合并GCode数据*/
    var mergeGcodeData: Boolean = false,

    /**是否要强制指定数据处理的模式
     *
     * [com.angcyo.laserpacker.LPDataConstant.DATA_MODE_BLACK_WHITE]
     * [com.angcyo.laserpacker.LPDataConstant.DATA_MODE_GCODE]
     * [com.angcyo.laserpacker.LPDataConstant.DATA_MODE_DITHERING]
     * */
    var dataMode: Int? = null,
) {

    /**获取指定图层对应的dpi*/
    fun getLayerConfigDpi(layerId: String?): Float = getLayerConfig(layerId)?.dpi ?: 254f

    /**[layerId]图层*/
    fun getLayerConfig(layerId: String?): TransferLayerConfigBean? {
        return getLayerConfigList()?.find { it.layerId == layerId }
    }

    fun getLayerConfigList(): List<TransferLayerConfigBean>? = layerJson.getLayerConfigList()
}
