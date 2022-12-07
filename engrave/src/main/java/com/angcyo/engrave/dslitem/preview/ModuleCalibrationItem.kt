package com.angcyo.engrave.dslitem.preview

import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.R
import com.angcyo.engrave.moduleCalibrationDialog
import com.angcyo.widget.DslViewHolder

/**
 * 模块校准, 开始校准之前, 需要先发送预览中心点指令
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/11/01
 */
class ModuleCalibrationItem : BasePreviewItem() {

    companion object {
        /**最后一次是否完成了握笔*/
        var lastIsModuleCalibration = false
    }

    /**是否校准完成*/
    var isModuleCalibration = lastIsModuleCalibration

    /**开始对笔之前的回调*/
    var onCalibrationAction: () -> Unit = {}

    init {
        itemLayoutId = R.layout.item_module_calibration_layout

        itemClick = {
            showCenterPreview()
            it.context.moduleCalibrationDialog {
                onModuleCalibrationAction = {
                    lastIsModuleCalibration = it
                    isModuleCalibration = it
                    updateAdapterItem()
                }
            }
            onCalibrationAction()
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

    /**切换到中心点预览*/
    fun showCenterPreview() {
        previewModel.updatePreview {
            isCenterPreview = true
        }
    }

}