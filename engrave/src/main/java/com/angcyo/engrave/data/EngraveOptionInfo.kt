package com.angcyo.engrave.data

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.library.unit.MmValueUnit
import com.angcyo.objectbox.laser.pecker.entity.EngraveHistoryEntity

/**
 * 打印选项参数信息
 *
 * [com.angcyo.engrave.dslitem.EngraveOptionWheelItem]
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/02
 */
data class EngraveOptionInfo(
    //材料名称
    var material: String,
    //功率 100% [0~100]
    var power: Byte = 100,
    //打印深度 10% [0~100]
    //com.angcyo.bluetooth.fsc.laserpacker.command.EngraveCmd.toHexCommandString
    var depth: Byte = 10,
    //打印次数, 最大255
    var time: Byte = 1,
    //图片雕刻时的起始坐标
    var x: Int = 0x0,
    var y: Int = 0x0,
    //l_type：雕刻激光类型选择，0为1064nm激光 (白光-雕)，1为450nm激光 (蓝光-烧)。(L3max新增)
    var type: Byte = LaserPeckerHelper.LASER_TYPE_BLUE,
    /**雕刻物体直径, 这里用像素作为单位
     * [com.angcyo.bluetooth.fsc.laserpacker.command.EngraveCmd.diameter]
     *
     * [com.angcyo.engrave.data.EngraveDataInfo.width]
     * [com.angcyo.engrave.data.EngraveDataInfo.height]
     * */
    var diameterPixel: Float = 0f,
    /* 在[com.angcyo.engrave.data.EngraveDataInfo.px]中设置
    //分辨率
    var px: Byte = LaserPeckerHelper.DEFAULT_PX,*/
    //0x01 从头开始打印文件，0x02继续打印文件，0x03结束打印，0x04暂停打印
    var state: Byte = 0x01,
) {
    /**更新数据到[EngraveHistoryEntity]*/
    fun updateToEntity(entity: EngraveHistoryEntity) {
        entity.material = material
        entity.power = power
        entity.depth = depth
        entity.type = type
        entity.diameter = MmValueUnit().convertPixelToValue(diameterPixel)
    }
}
