package com.angcyo.engrave

import com.angcyo.bluetooth.fsc.enqueue
import com.angcyo.bluetooth.fsc.laserpacker.asyncQueryDeviceState
import com.angcyo.bluetooth.fsc.laserpacker.command.ExitCmd
import com.angcyo.bluetooth.fsc.laserpacker.syncQueryDeviceState
import com.angcyo.canvas.data.CanvasProjectItemBean
import com.angcyo.engrave.dslitem.EngraveDividerItem
import com.angcyo.engrave.dslitem.preview.*
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
abstract class BasePreviewLayoutHelper : BaseFlowLayoutHelper() {

    override fun renderFlowItems() {
        when (engraveFlow) {
            ENGRAVE_FLOW_PREVIEW_BEFORE_CONFIG -> renderPreviewItems()
            ENGRAVE_FLOW_PREVIEW -> renderPreviewItems()
            else -> super.renderFlowItems()
        }
    }

    override fun onEngraveFlowChanged(from: Int, to: Int) {
        super.onEngraveFlowChanged(from, to)
        if (to == ENGRAVE_FLOW_TRANSFER_BEFORE_CONFIG) {
            //预览前的第三轴配置信息
        } else if (to == ENGRAVE_FLOW_PREVIEW) {
            //预览界面, 创建预览信息, 并开始预览
            previewModel.startPreview(PreviewModel.createPreviewInfo(engraveCanvasFragment?.canvasDelegate))
            previewExDeviceNoConnectTip()
        }
    }

    //region ---预览界面/支架控制---

    /**渲染预览界面界面*/
    fun renderPreviewItems() {
        updateIViewTitle(_string(R.string.previewing))
        engraveBackFlow = 0
        //close按钮
        showCloseView(true, _string(R.string.ui_quit))
        if (engraveFlow == ENGRAVE_FLOW_PREVIEW) {
            UMEvent.PREVIEW.umengEventValue()
        }

        val previewConfigEntity = EngraveFlowDataHelper.generatePreviewConfig(flowTaskId)

        renderDslAdapter {
            //
            PreviewTipItem()()
            if (!laserPeckerModel.isC1()) {
                //非C1显示, 设备水平角度
                DeviceAngleItem()()
            }
            if (laserPeckerModel.needShowExDeviceTipItem()) {
                PreviewExDeviceTipItem()()
            }
            if (laserPeckerModel.isPenMode()) {
                //握笔模式, 不支持亮度调节, 握笔校准
                ModuleCalibrationItem()()
            } else {
                PreviewBrightnessItem()() {
                    itemPreviewConfigEntity = previewConfigEntity
                }
            }
            if (laserPeckerModel.isC1()) {
                //C1没有升降支架
            } else {
                PreviewBracketItem()()
            }
            if (laserPeckerModel.isROpen()) {
                //物理直径
                PreviewDiameterItem()() {
                    itemPreviewConfigEntity = previewConfigEntity
                }
            }
            EngraveDividerItem()()
            PreviewControlItem()() {
                itemPathPreviewClick = {
                    startPathPreview(it as? CanvasProjectItemBean)
                }
            }
            DslBlackButtonItem()() {
                itemButtonText = _string(R.string.ui_next)
                itemClick = {
                    //下一步, 数据配置界面

                    //让设备进入空闲模式
                    ExitCmd().enqueue()
                    syncQueryDeviceState()

                    engraveFlow = ENGRAVE_FLOW_TRANSFER_BEFORE_CONFIG
                    renderFlowItems()
                }
            }
            asyncQueryDeviceState()
        }
    }

    /**开始路径预览流程*/
    fun startPathPreview(projectDataBean: CanvasProjectItemBean?) {
        projectDataBean ?: return
        viewHolder?.context?.pathPreviewDialog(projectDataBean) {

        }
    }

    //endregion ---预览界面/支架控制---
}