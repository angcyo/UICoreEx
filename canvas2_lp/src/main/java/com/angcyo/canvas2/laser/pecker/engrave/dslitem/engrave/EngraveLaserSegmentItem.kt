package com.angcyo.canvas2.laser.pecker.engrave.dslitem.engrave

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.EngraveSegmentScrollItem
import com.angcyo.item.style.itemCurrentIndex
import com.angcyo.laserpacker.device.DeviceHelper
import com.angcyo.library.ex._string

/**
 * 雕刻光源选择item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/04/04
 */
class EngraveLaserSegmentItem : EngraveSegmentScrollItem() {

    val typeList = LaserPeckerHelper.findProductSupportLaserTypeList()

    init {
        itemText = _string(R.string.laser_type)
        itemSegmentList = typeList
        itemCurrentIndex = typeList.indexOfFirst {
            it.type == DeviceHelper.getProductLaserType()
        }
    }

    fun currentLaserTypeInfo() = typeList[itemCurrentIndex]

}