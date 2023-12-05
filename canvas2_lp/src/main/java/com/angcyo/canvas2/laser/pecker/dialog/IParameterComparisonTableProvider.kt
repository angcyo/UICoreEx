package com.angcyo.canvas2.laser.pecker.dialog

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.laserpacker.device.DeviceHelper
import com.angcyo.library.annotation.MM

/**
 * 参数表PCT需要的一些数据提供者
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/12/05
 */
interface IParameterComparisonTableProvider {

    /**不同类型元素之间的距离
     * [2f] */
    @MM
    var elementMargin: Float

    /**指定激光类型
     * [LaserPeckerHelper.LASER_TYPE_BLUE]
     * [LaserPeckerHelper.LASER_TYPE_WHITE]
     * [DeviceHelper.getProductLaserType]*/
    var gridPrintType: Byte

    /**标签 Power/Depth 文本字体大小*/
    @MM
    val labelTextFontSize: Float
        get() = ParameterComparisonTableDialogConfig.pctTextFontSize * ParameterComparisonTableDialogConfig.pctLabelTextFontScale

    @MM
    val titleTextFontSize: Float
        get() = ParameterComparisonTableDialogConfig.pctTextFontSize * ParameterComparisonTableDialogConfig.pctTitleTextFontScale

}