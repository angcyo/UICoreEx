package com.angcyo.engrave

import com.angcyo.bluetooth.fsc.laserpacker.queryDeviceState
import com.angcyo.canvas.utils.CanvasConstant
import com.angcyo.engrave.dslitem.preview.PreviewBracketItem
import com.angcyo.engrave.dslitem.preview.PreviewBrightnessItem
import com.angcyo.engrave.dslitem.preview.PreviewControlItem
import com.angcyo.engrave.dslitem.preview.PreviewTipItem
import com.angcyo.engrave.model.PreviewModel
import com.angcyo.item.DslBlackButtonItem
import com.angcyo.item.DslLineItem
import com.angcyo.library.ex._string
import com.angcyo.library.ex.dpi

/**
 * 雕刻布局, 预览布局相关操作
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/26
 */
abstract class BaseEngravePreviewLayoutHelper : BaseEngraveLayoutHelper() {

    override fun renderFlowItems() {
        if (engraveFlow == ENGRAVE_FLOW_PREVIEW_BEFORE_CONFIG ||
            engraveFlow == ENGRAVE_FLOW_PREVIEW
        ) {
            //close按钮
            showCloseView()
            renderPreviewItems()

            //UMEvent.PREVIEW.umengEventValue()
        } else {
            super.renderFlowItems()
        }
    }

    /**渲染预览界面界面*/
    fun renderPreviewItems() {
        renderDslAdapter {
            //
            PreviewTipItem()()
            PreviewBrightnessItem()()
            if (laserPeckerModel.productInfoData.value?.isCI() == true) {
                //C1没有升降支架
            } else {
                PreviewBracketItem()() {
                    itemValueUnit = CanvasConstant.valueUnit
                }
            }
            DslLineItem()() {
                itemHeight = 30 * dpi
            }
            PreviewControlItem()()
            DslBlackButtonItem()() {
                itemButtonText = _string(R.string.ui_next)
            }
            updatePreview()
            queryDeviceState()
        }
    }

    /**更新预览, 比如元素选择改变后/大小改变后*/
    fun updatePreview(async: Boolean = true, zPause: Boolean = false) {
        val delegate = engraveCanvasFragment?.canvasDelegate
        if (delegate == null) {
        } else {
            val previewInfo = PreviewModel.createPreviewInfo(delegate)
            previewModel.startOrRefreshPreview(previewInfo, async, zPause)
        }
    }

}