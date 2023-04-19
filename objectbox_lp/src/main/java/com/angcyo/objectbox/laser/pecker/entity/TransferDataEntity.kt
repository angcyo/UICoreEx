package com.angcyo.objectbox.laser.pecker.entity

import androidx.annotation.Keep
import com.angcyo.library.annotation.MM
import com.angcyo.library.ex.file
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

/**
 * 通过数据配置, 生成机器需要的雕刻数据
 * [com.angcyo.objectbox.laser.pecker.entity.TransferConfigEntity]
 *
 * [com.angcyo.objectbox.laser.pecker.entity.EngraveConfigEntity]
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/10/09
 */

@Keep
@Entity
data class TransferDataEntity(

    @Id var entityId: Long = 0L,

    /**当前雕刻任务的id*/
    var taskId: String? = null,

    /**产品名
     * L2 L3 L4等 */
    var productName: String? = null,

    /**蓝牙设备地址
     * DC:0D:30:00:1F:60*/
    var deviceAddress: String? = null,

    //---

    /**数据, 纯数据. 不包含文件头. 此数据不入库, 通过文件路径的方式入库
     * [String.toByteArray]
     * [ByteArray.toString]
     * [Charsets.UTF_8] 不能使用此编码
     * [Charsets.ISO_8859_1]
     * ```
     * "".toByteArray(Charsets.ISO_8859_1).toString(Charsets.ISO_8859_1)
     * ```
     * */
    //var data: String? = null,

    /**数据路径, 直接存储数据数据库会炸裂, 所以这里存储数据文本的路径.
     * 这个数据就是传输的字节数据
     * [com.angcyo.engrave.transition.EngraveTransitionManager.writeTransferDataPath]
     * [ByteArray.writeTransferDataPath]
     * */
    var dataPath: String? = null,

    /**下位机雕刻的数据类型
     * [com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd.ENGRAVE_TYPE_BITMAP_DITHERING]
     * [com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd.ENGRAVE_TYPE_GCODE]
     * [com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd.ENGRAVE_TYPE_BITMAP_PATH]
     * [com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd.ENGRAVE_TYPE_BITMAP]
     * */
    var engraveDataType: Int = -1,

    /**数据处理之前的坐标
     * [width] [height]*/
    @MM
    var originX: Float? = null,
    @MM
    var originY: Float? = null,

    /**数据处理之前的宽高
     * [width] [height]*/
    @MM
    var originWidth: Float? = null,
    @MM
    var originHeight: Float? = null,

    //---图片/GCode数据相关属性, px修正过后的

    /**
     * 图片的宽高, 需要使用px分辨率进行调整修正
     * GCode的宽高, 是mm*10后的值
     * [com.angcyo.engrave.data.EngraveOptionInfo.diameterPixel]
     * [com.angcyo.bluetooth.fsc.laserpacker.command.EngraveCmd.diameter]
     * */
    @MM
    var width: Int = -1,
    @MM
    var height: Int = -1,

    /**
     * 图片的xy, 需要使用px分辨率进行调整修正
     * GCode的xy, 是mm*10后的值
     * [com.angcyo.engrave.data.EngraveOptionInfo.diameterPixel]
     * [com.angcyo.bluetooth.fsc.laserpacker.command.EngraveCmd.diameter]
     * */
    @MM
    var x: Int = -1,
    @MM
    var y: Int = -1,

    /**分辨率*/
    var dpi: Float = -1f,

    //---

    /**雕刻数据的索引, 32位, 4个字节
     * [com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd]*/
    var index: Int = -1, //(System.currentTimeMillis() / 1000).toInt()
    /**雕刻显示的文件名, 36个字节*/
    var name: String? = null,

    //---

    /**
     * GCode数据的总行数, 下位机用来计算进度使用
     * 路径数据的总线段数
     * */
    var lines: Int = -1,

    //---

    /**当前的数据, 属于那个图层. 图层决定了雕刻参数
     * [com.angcyo.objectbox.laser.pecker.entity.EngraveConfigEntity.layerMode]
     *
     * [com.angcyo.laserpacker.device.data.EngraveLayerInfo]
     * [com.angcyo.laserpacker.device.LayerHelper.getEngraveLayerList]
     * */
    var layerId: String? = null,

    //---

    /**是否传输完成了*/
    var isTransfer: Boolean = false

) {
    /**获取字节数据*/
    fun bytes(): ByteArray? = dataPath?.file()?.readBytes()

    /**文件数据大小/字节byte*/
    fun bytesSize(): Long = dataPath?.file()?.length() ?: 0
}