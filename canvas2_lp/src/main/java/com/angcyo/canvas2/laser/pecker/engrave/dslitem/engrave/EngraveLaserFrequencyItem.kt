package com.angcyo.canvas2.laser.pecker.engrave.dslitem.engrave

import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.dialog2.dslitem.getSelectedWheelBean
import com.angcyo.dialog2.dslitem.itemSelectedIndex
import com.angcyo.dialog2.dslitem.itemWheelList
import com.angcyo.dialog2.dslitem.itemWheelSelectorAction
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.item.style.itemLabelText
import com.angcyo.library.ex._string
import com.angcyo.objectbox.laser.pecker.lpSaveEntity
import kotlin.math.max

/**
 * 雕刻参数, 白光出光频率
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023-10-30
 */
class EngraveLaserFrequencyItem : EngraveOptionWheelItem() {

    companion object {
        const val PAYLOAD_UPDATE_LASER_FREQUENCY = 0x1000
    }

    init {
        itemLabelText = _string(R.string.engrave_laser_frequency_label)

        itemUpdateAction = {
            if (it == PAYLOAD_UPDATE_LASER_FREQUENCY) {
                val list = itemWheelList as? List<Int>
                if (list != null) {
                    val value = itemEngraveConfigEntity?.laserFrequency
                        ?: itemEngraveItemBean?.laserFrequency ?: HawkEngraveKeys.lastLaserFrequency
                        ?: HawkEngraveKeys.defaultLaserFrequency
                    itemSelectedIndex = max(
                        0,
                        list.indexOf(list.find { it == value })
                    )
                    onSelfLaserFrequencyChange()
                }
            }
        }

        itemWheelSelectorAction = { dialog, index, item ->
            false
        }
    }

    /**初始化默认的参数*/
    fun initLaserFrequencyIfNeed() {
        itemEngraveConfigEntity?.apply {
            if (laserFrequency == null) {
                laserFrequency =
                    HawkEngraveKeys.lastLaserFrequency ?: HawkEngraveKeys.defaultLaserFrequency
                lpSaveEntity()
            }
        }
        itemEngraveItemBean?.apply {
            if (laserFrequency == null) {
                laserFrequency =
                    HawkEngraveKeys.lastLaserFrequency ?: HawkEngraveKeys.defaultLaserFrequency
            }
        }
    }

    override fun onItemChangeListener(item: DslAdapterItem) {
        super.onItemChangeListener(item)
        onSelfLaserFrequencyChange()
    }

    /**值改变时, 需要进行的操作*/
    fun onSelfLaserFrequencyChange() {
        val value = getSelectedWheelBean<Int>()
        itemEngraveConfigEntity?.apply {
            laserFrequency = value ?: HawkEngraveKeys.defaultLaserFrequency
            lpSaveEntity()
        }
        itemEngraveItemBean?.apply {
            laserFrequency = value ?: HawkEngraveKeys.defaultLaserFrequency
        }
    }
}