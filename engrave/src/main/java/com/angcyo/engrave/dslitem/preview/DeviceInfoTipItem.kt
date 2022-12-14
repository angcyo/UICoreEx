package com.angcyo.engrave.dslitem.preview

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.core.vmApp
import com.angcyo.engrave.R
import com.angcyo.library.ex._color
import com.angcyo.library.ex._string
import com.angcyo.widget.span.span

/**
 * 设备信息提示, 水平角度
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/11/01
 */
class DeviceInfoTipItem : PreviewTipItem() {

    companion object {

        /**设备角度/温度信息*/
        fun deviceInfoTip(): CharSequence = span {
            //产品模式
            val laserPeckerModel = vmApp<LaserPeckerModel>()
            val stateParser = laserPeckerModel.deviceStateData.value
            val angle = stateParser?.angle ?: 0
            if (angle != 0) {
                append(_string(R.string.device_angle))
                append(":$angle° ")
            }
            val temp = stateParser?.temp ?: 0
            if (temp >= 60) {
                append(_string(R.string.device_temp))
                append(":$temp° ")
            }
        }
    }

    init {
        itemTipTextColor = _color(R.color.text_sub_color)
        itemTip = null
    }
}