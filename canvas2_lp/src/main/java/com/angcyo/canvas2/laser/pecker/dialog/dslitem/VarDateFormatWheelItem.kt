package com.angcyo.canvas2.laser.pecker.dialog.dslitem

import com.angcyo.bluetooth.fsc.laserpacker._deviceSettingBean
import com.angcyo.dialog2.dslitem.LPLabelWheelItem
import com.angcyo.dialog2.dslitem.itemWheelList

/**
 * 预设的日期格式选择item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/09/06
 */
class VarDateFormatWheelItem : LPLabelWheelItem() {

    init {
        itemWheelList = _deviceSettingBean?.dateFormatList
    }

}