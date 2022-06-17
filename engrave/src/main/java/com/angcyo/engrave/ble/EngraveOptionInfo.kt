package com.angcyo.engrave.ble

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.DEFAULT_PX

/**
 * 打印选项参数信息
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/02
 */
data class EngraveOptionInfo(
    //材料名称
    var material: String,
    //功率 100% [0~100]
    var power: Byte = 100,
    //打印深度 10% [0~100]
    var depth: Byte = 10,
    //打印次数
    var time: Byte = 1,
    //0x01 从头开始打印文件，0x02继续打印文件，0x03结束打印，0x04暂停打印
    var state: Byte = 0x01,
    //图片雕刻时的起始坐标
    var x: Int = 0x0,
    var y: Int = 0x0,
    //l_type：雕刻激光类型选择，0为1064nm激光 (白光)，1为450nm激光 (蓝光)。(L3max新增)
    var type: Byte = 0x0,
    //分辨率
    var px: Byte = DEFAULT_PX
)
