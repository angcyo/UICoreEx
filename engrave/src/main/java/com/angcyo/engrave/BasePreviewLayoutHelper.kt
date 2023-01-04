package com.angcyo.engrave

import com.angcyo.bluetooth.fsc.enqueue
import com.angcyo.bluetooth.fsc.laserpacker.command.ExitCmd
import com.angcyo.bluetooth.fsc.laserpacker.syncQueryDeviceState
import com.angcyo.canvas.data.CanvasProjectItemBean
import com.angcyo.engrave.data.HawkEngraveKeys
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
            ENGRAVE_FLOW_PREVIEW_BEFORE_CONFIG -> renderPreviewBeforeItems()
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
            engraveCanvasFragment?.canvasDelegate?.let {
                previewModel.startPreview(PreviewModel.createPreviewInfo(it))
            }

            //延迟后查询设备连接状态
            viewHolder?.postDelay(HawkEngraveKeys.minQueryDelayTime) {
                syncQueryDeviceState { bean, error ->
                    if (error == null) {
                        previewExDeviceNoConnectTip()
                    }
                }
            }
        }
    }

    //region ---预览界面/支架控制---

    /**渲染预览前配置界面*/
    fun renderPreviewBeforeItems() {
        renderDslAdapter {
            if (laserPeckerModel.needShowExDeviceTipItem()) {
                PreviewExDeviceTipItem()()
            }
            /*if (laserPeckerModel.isC1()) {
                if (laserPeckerModel.isPenMode()) {
                    updateIViewTitle(_string(R.string.device_module_calibration_label))
                    //握笔模式, 不支持亮度调节, 握笔校准
                    ModuleCalibrationItem()()
                }
            }*/
            if (laserPeckerModel.isROpen()) {
                //旋转轴, 提前设置物理直径
                val previewConfigEntity = EngraveFlowDataHelper.generatePreviewConfig(flowTaskId)
                PreviewDiameterItem()() {
                    itemPreviewConfigEntity = previewConfigEntity
                }
            }
            DslBlackButtonItem()() {
                itemButtonText = _string(R.string.ui_next)
                itemClick = {
                    //下一步, 雕刻预览界面
                    engraveFlow = ENGRAVE_FLOW_PREVIEW
                    renderFlowItems()
                }
            }
        }
    }

    /**渲染预览界面*/
    fun renderPreviewItems() {
        updateIViewTitle(_string(R.string.preview))
        engraveBackFlow = 0
        //close按钮
        showCloseView(true, _string(R.string.ui_quit))
        if (engraveFlow == ENGRAVE_FLOW_PREVIEW) {
            UMEvent.PREVIEW.umengEventValue()
        }

        val previewConfigEntity = EngraveFlowDataHelper.generatePreviewConfig(flowTaskId)

        renderDslAdapter {
            //
            if (this@BasePreviewLayoutHelper is HistoryEngraveFlowLayoutHelper) {
                //历史界面, 不显示拖拽元素提示
            } else {
                PreviewTipItem()()
            }
            if (!laserPeckerModel.isC1()) {
                //非C1显示, 设备水平角度
                renderDeviceInfoIfNeed()
            }
            //预览情况下, 一直显示设备扩展信息, 因为有焦距提示
            PreviewExDeviceTipItem()()
            if (laserPeckerModel.isPenMode()) {
                //握笔模式, 不支持亮度调节, 握笔校准
                ModuleCalibrationItem()() {
                    onCalibrationAction = {
                        pauseLoopCheckDeviceState = it == 1
                        syncQueryDeviceState { bean, error ->
                            if (error == null) {
                                //刷新界面
                                updateAllItem()
                            }
                        }
                    }
                }
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
                //物理直径 //2022-12-18 移动到预览之前配置
                /*PreviewDiameterItem()() {
                    itemPreviewConfigEntity = previewConfigEntity
                }*/
            }
            EngraveDividerItem()()
            //预览控制, 范围/中心点预览
            PreviewControlItem()() {
                itemPathPreviewClick = {
                    startPathPreview(it as? CanvasProjectItemBean)
                }
            }
            DslBlackButtonItem()() {
                itemButtonText = _string(R.string.ui_next)
                itemClick = {
                    //下一步, 数据配置界面

                    if (checkOverflowBounds() || !checkTransferData()) {
                        //不允许雕刻
                    } else {
                        //让设备进入空闲模式
                        ExitCmd().enqueue()
                        syncQueryDeviceState()

                        engraveFlow = ENGRAVE_FLOW_TRANSFER_BEFORE_CONFIG
                        engraveBackFlow = 0
                        renderFlowItems()
                    }
                }
            }
        }

        //轮询查询状态
        checkLoopQueryDeviceState(true)
    }

    /**开始路径预览流程*/
    fun startPathPreview(projectDataBean: CanvasProjectItemBean?) {
        projectDataBean ?: return
        pauseLoopCheckDeviceState = true
        viewHolder?.context?.pathPreviewDialog(projectDataBean) {
            onDismissListener = {
                pauseLoopCheckDeviceState = false
            }
        }
    }

    //endregion ---预览界面/支架控制---
}