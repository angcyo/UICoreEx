package com.angcyo.engrave.dslitem.preview

import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.R
import com.angcyo.engrave.moduleCalibrationDialog
import com.angcyo.widget.DslViewHolder

/**
 * 模块校准
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/11/01
 */
class ModuleCalibrationItem : DslAdapterItem() {

    companion object {
        /**最后一次是否完成了握笔*/
        var lastIsModuleCalibration = false
    }

    /**是否校准完成*/
    var isModuleCalibration = lastIsModuleCalibration

    init {
        itemLayoutId = R.layout.item_module_calibration_layout

        itemClick = {
            it.context.moduleCalibrationDialog {
                onModuleCalibrationAction = {
                    lastIsModuleCalibration = it
                    isModuleCalibration = it
                    updateAdapterItem()
                }
            }
        }
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
        itemHolder.view(R.id.lib_check_view)?.isSelected = isModuleCalibration
    }

}