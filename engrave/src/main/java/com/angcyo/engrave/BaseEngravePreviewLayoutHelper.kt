package com.angcyo.engrave

import com.angcyo.bluetooth.fsc.laserpacker.asyncQueryDeviceState
import com.angcyo.canvas.utils.CanvasConstant
import com.angcyo.engrave.dslitem.EngraveDividerItem
import com.angcyo.engrave.dslitem.preview.PreviewBracketItem
import com.angcyo.engrave.dslitem.preview.PreviewBrightnessItem
import com.angcyo.engrave.dslitem.preview.PreviewControlItem
import com.angcyo.engrave.dslitem.preview.PreviewTipItem
import com.angcyo.engrave.model.PreviewModel
import com.angcyo.item.DslBlackButtonItem
import com.angcyo.library.ex._string
import com.hingin.umeng.UMEvent
import com.hingin.umeng.umengEventValue

/**
 * 雕刻布局, 预览布局相关操作
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/26
 */
abstract class BaseEngravePreviewLayoutHelper : BaseFlowLayoutHelper() {

    override fun renderFlowItems() {
        when (engraveFlow) {
            ENGRAVE_FLOW_PREVIEW_BEFORE_CONFIG -> renderPreviewItems()
            ENGRAVE_FLOW_PREVIEW -> renderPreviewItems()
            else -> super.renderFlowItems()
        }
    }

    //

    /**渲染预览界面界面*/
    fun renderPreviewItems() {
        updateIViewTitle(_string(R.string.previewing))
        engraveBackFlow = 0
        //close按钮
        showCloseView(true, _string(R.string.ui_quit))
        if (engraveFlow == ENGRAVE_FLOW_PREVIEW) {
            UMEvent.PREVIEW.umengEventValue()
        }
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
            EngraveDividerItem()()
            PreviewControlItem()()
            DslBlackButtonItem()() {
                itemButtonText = _string(R.string.ui_next)
                itemClick = {
                    //下一步, 数据配置界面
                    engraveFlow = ENGRAVE_FLOW_TRANSFER_BEFORE_CONFIG
                    renderFlowItems()
                }
            }
            updatePreview()
            asyncQueryDeviceState()
        }
    }

    //

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