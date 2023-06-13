package com.angcyo.canvas2.laser.pecker.engrave

import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker.isOverflowProductBounds
import com.angcyo.bluetooth.fsc.laserpacker.syncQueryDeviceState
import com.angcyo.canvas.render.core.Reason
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dialog.pathPreviewDialog
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.EngraveDividerItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.preview.CalibrationOffsetItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.preview.ModuleCalibrationItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.preview.PreviewBracketItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.preview.PreviewBrightnessItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.preview.PreviewControlItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.preview.PreviewDiameterItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.preview.PreviewExDeviceTipItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.preview.PreviewTipItem
import com.angcyo.dsladapter.findItem
import com.angcyo.engrave2.EngraveFlowDataHelper
import com.angcyo.item.DslBlackButtonItem
import com.angcyo.library.ex._string
import com.angcyo.library.ex.isDebugType
import com.angcyo.library.toastQQ
import com.hingin.umeng.UMEvent
import com.hingin.umeng.umengEventValue

/**
 * 雕刻布局, 预览布局相关操作
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/26
 */
abstract class BasePreviewLayoutHelper : BaseFlowLayoutHelper() {

    override fun renderFlowItems() {
        if (isAttach()) {
            when (engraveFlow) {
                ENGRAVE_FLOW_PREVIEW_BEFORE_CONFIG -> renderPreviewBeforeItems()
                ENGRAVE_FLOW_PREVIEW -> renderPreviewItems()
                else -> super.renderFlowItems()
            }
        }
    }

    override fun onEngraveFlowChanged(from: Int, to: Int) {
        super.onEngraveFlowChanged(from, to)
        if (to == ENGRAVE_FLOW_TRANSFER_BEFORE_CONFIG) {
            //预览前的第三轴配置信息
        } else if (to == ENGRAVE_FLOW_PREVIEW) {
            //预览界面, 创建预览信息, 并开始预览
            engraveCanvasFragment?.renderDelegate?.let {
                previewModel.startPreview(LPPreviewHelper.createPreviewInfo(it))
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

        checkRightView()
    }

    override fun onIViewShow() {
        super.onIViewShow()
        isMinimumPreview = false
        checkRightView()
    }

    /**检查右边图标的可见性*/
    open fun checkRightView() {
        viewHolder?.visible(R.id.right_image_view, engraveFlow == ENGRAVE_FLOW_PREVIEW)
        viewHolder?.click(R.id.right_image_view) {
            isMinimumPreview = true
            removeInner()
        }
    }

    //region ---预览界面/支架控制---

    /**渲染预览前配置界面*/
    fun renderPreviewBeforeItems() {
        renderDslAdapter {
            if (deviceStateModel.needShowExDeviceTipItem()) {
                PreviewExDeviceTipItem()()
            }
            /*if (laserPeckerModel.isC1()) {
                if (laserPeckerModel.isPenMode()) {
                    updateIViewTitle(_string(R.string.device_module_calibration_label))
                    //握笔模式, 不支持亮度调节, 握笔校准
                    ModuleCalibrationItem()()
                }
            }*/
            if (laserPeckerModel.isROpen() || isDebugType()) {
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
        val delegate = engraveCanvasFragment?.renderDelegate

        val previewConfigEntity = EngraveFlowDataHelper.generatePreviewConfig(flowTaskId)

        renderDslAdapter {
            //
            if (this@BasePreviewLayoutHelper is HistoryEngraveFlowLayoutHelper) {
                //历史界面, 不显示拖拽元素提示
            } else {
                PreviewTipItem()()
            }
            if (!laserPeckerModel.isCSeries()) {
                //非C1显示, 设备水平角度
                renderDeviceInfoIfNeed()
            }
            //预览情况下, 一直显示设备扩展信息, 因为有焦距提示
            PreviewExDeviceTipItem()()
            if (deviceStateModel.isPenMode()) {
                //握笔模式, 不支持亮度调节, 握笔校准
                ModuleCalibrationItem()() {
                    onCalibrationAction = {
                        findItem(CalibrationOffsetItem::class.java, false)?.itemHidden =
                            !ModuleCalibrationItem.lastIsModuleCalibration
                        deviceStateModel.pauseLoopCheckState(it == 1, "握笔校准")
                        syncQueryDeviceState { bean, error ->
                            if (error == null) {
                                //刷新界面
                                updateAllItem()
                            }
                        }
                    }
                }

                //握笔偏移设置
                if (HawkEngraveKeys.enableCalibrationOffset) {
                    CalibrationOffsetItem()() {
                        itemHidden = !ModuleCalibrationItem.lastIsModuleCalibration

                        observeItemChange {
                            clearFlowId("数据偏移改变")
                            delegate?.dispatchAllRendererDataChange(Reason.user)
                        }
                    }
                }

            } else if (!laserPeckerModel.isL3()) { //L3只有白光, 不支持亮度调节
                PreviewBrightnessItem()() {
                    itemPreviewConfigEntity = previewConfigEntity
                }
            }
            if (laserPeckerModel.isCSeries()) {
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
                    startPathPreview("$it")
                }
            }
            DslBlackButtonItem()() {
                itemButtonText = _string(R.string.ui_next)
                itemClick = {
                    //下一步, 数据配置界面

                    if (!checkCanNext()) {
                        //不允许雕刻
                    } else {
                        //让设备进入空闲模式
                        deviceStateModel.pauseLoopCheckState(true, "预览界面下一步")
                        asyncTimeoutExitCmd { bean, error ->
                            if (error == null) {
                                syncQueryDeviceState()
                                engraveFlow = ENGRAVE_FLOW_TRANSFER_BEFORE_CONFIG
                                engraveBackFlow = 0
                                renderFlowItems()
                            } else {
                                toastQQ(error.message)
                            }
                        }
                    }
                }
            }
        }

        //轮询查询状态
        deviceStateModel.startLoopCheckState(reason = "预览界面")
    }

    /**开始路径预览流程*/
    fun startPathPreview(renderUuid: String?) {
        renderUuid ?: return
        val delegate = engraveCanvasFragment?.renderDelegate ?: return
        val itemRenderer = delegate.renderManager.findElementRenderer(renderUuid)
        if (itemRenderer?.renderProperty?.getRenderBounds().isOverflowProductBounds()) {
            toastQQ(_string(R.string.engrave_bounds_warn))
            return
        }
        deviceStateModel.pauseLoopCheckState(true, "开始路径预览")
        viewHolder?.context?.pathPreviewDialog(renderUuid) {
            renderDelegate = delegate
            onDismissListener = {
                deviceStateModel.pauseLoopCheckState(false, "结束路径预览")
            }
        }
    }

    //endregion ---预览界面/支架控制---
}