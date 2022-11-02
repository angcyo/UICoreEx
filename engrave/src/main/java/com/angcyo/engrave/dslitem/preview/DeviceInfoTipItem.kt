package com.angcyo.engrave.dslitem.preview

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.core.vmApp
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.R
import com.angcyo.library.ex._color
import com.angcyo.library.ex._string
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.span.span

/**
 * 设备信息提示, 水平角度
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/11/01
 */
class DeviceInfoTipItem : PreviewTipItem() {

    //产品模式
    val laserPeckerModel = vmApp<LaserPeckerModel>()

    init {
        itemTipTextColor = _color(R.color.text_sub_color)
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        val angle = laserPeckerModel.deviceStateData.value?.angle ?: 0
        itemTip = span {
            append(_string(R.string.device_angle))
            append(":$angle°")
        }
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
    }

}