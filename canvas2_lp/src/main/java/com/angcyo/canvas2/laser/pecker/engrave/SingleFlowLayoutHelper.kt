package com.angcyo.canvas2.laser.pecker.engrave

import com.angcyo.bluetooth.fsc.enqueue
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.command.EngraveCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.ExitCmd
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.EngraveSegmentScrollItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.engrave.EngraveConfirmItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.engrave.EngraveMaterialWheelItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.engrave.EngraveOptionWheelItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.engrave.EngraveProgressItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.engrave.EngravePropertyItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.engrave.EngravingControlItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.engrave.EngravingInfoItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.preview.PreviewExDeviceTipItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.preview.PreviewTipItem
import com.angcyo.core.tgStrokeLoadingCaller
import com.angcyo.engrave2.EngraveFlowDataHelper
import com.angcyo.item.DslBlackButtonItem
import com.angcyo.item.style.itemCurrentIndex
import com.angcyo.item.style.itemLabelText
import com.angcyo.laserpacker.device.EngraveHelper
import com.angcyo.laserpacker.device.MaterialHelper
import com.angcyo.laserpacker.device.engraveLoadingAsyncTimeout
import com.angcyo.library.ex._string
import com.angcyo.library.ex.dpi
import com.angcyo.library.ex.syncSingle
import com.angcyo.library.toastQQ
import com.angcyo.library.utils.uuid
import com.angcyo.objectbox.laser.pecker.entity.EngraveConfigEntity
import com.angcyo.objectbox.laser.pecker.entity.MaterialEntity
import com.angcyo.objectbox.laser.pecker.lpSaveEntity
import com.angcyo.widget.span.span

/**
 * 简单的雕刻流程控制, 简单的预览, 简单的雕刻
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2023/08/05
 */
class SingleFlowLayoutHelper : BasePreviewLayoutHelper() {

    init {
        flowTaskId = uuid()
        engraveFlow
        selectLayerId = HawkEngraveKeys.lastLayerId
    }

    override fun renderFlowItems() {
        if (isAttach()) {
            when (engraveFlow) {
                ENGRAVE_FLOW_BEFORE_CONFIG -> renderEngraveConfig()
                ENGRAVE_FLOW_ENGRAVING -> renderEngraving()
                else -> super.renderFlowItems()
            }
        }
    }

    /**渲染雕刻配置界面
     * [com.angcyo.canvas2.laser.pecker.engrave.EngraveFlowLayoutHelper.renderEngraveConfig]*/
    fun renderEngraveConfig() {
        updateIViewTitle(_string(R.string.print_setting))
        engraveBackFlow = 0
        showCloseView(true, _string(R.string.ui_quit))

        val taskId = flowTaskId

        //当前选中图层的雕刻配置
        val engraveConfigEntity =
            engraveConfigProvider.getEngraveConfig(this, selectLayerId)
        val materialEntity = engraveConfigProvider.getEngraveMaterial(this, selectLayerId)

        renderDslAdapter {
            PreviewTipItem()() {
                itemTip = _string(R.string.engrave_tip)
            }
            if (!laserPeckerModel.isCSeries()) {
                //非C1显示, 设备水平角度
                renderDeviceInfoIfNeed()
            }
            if (deviceStateModel.needShowExDeviceTipItem()) {
                PreviewExDeviceTipItem()() {
                    itemEngraveConfigEntity = engraveConfigEntity
                }
            }

            //雕刻相关的参数

            //雕刻图层切换
            if (!deviceStateModel.isPenMode()) {//握笔模块, 不需要材质
                //材质选择
                EngraveMaterialWheelItem()() {
                    itemTag = MaterialEntity::name.name
                    itemLabelText = _string(R.string.custom_material)
                    itemWheelList = MaterialHelper.getLayerMaterialList(selectLayerId)
                    itemSelectedIndex = MaterialHelper.indexOfMaterial(
                        itemWheelList as List<MaterialEntity>,
                        materialEntity
                    )
                    itemEngraveConfigEntity = engraveConfigEntity

                    //刷新界面
                    observeItemChange {
                        renderFlowItems()
                    }
                }
            }

            // 激光光源选择
            val typeList = LaserPeckerHelper.findProductSupportLaserTypeList()
            if (laserPeckerModel.productInfoData.value?.isCI() != true && typeList.isNotEmpty()) {
                EngraveSegmentScrollItem()() {
                    itemText = _string(R.string.laser_type)
                    itemSegmentList = typeList
                    itemCurrentIndex =
                        typeList.indexOfFirst { it.type == engraveConfigEntity.type }
                    observeItemChange {
                        val type = typeList[itemCurrentIndex].type
                        HawkEngraveKeys.lastType = type.toInt()
                        engraveConfigEntity.type = type
                        engraveConfigEntity.lpSaveEntity()
                        renderFlowItems()
                    }
                }
            }

            if (laserPeckerModel.isCSeries()) {
                //C1 加速级别选择
                if (engraveConfigEntity.precision < 0) {
                    engraveConfigEntity.precision = 1
                    engraveConfigEntity.lpSaveEntity()
                }
                EngraveOptionWheelItem()() {
                    itemTag = EngraveConfigEntity::precision.name
                    itemLabelText = _string(R.string.engrave_precision)
                    itemWheelList = EngraveHelper.percentList(5)
                    itemSelectedIndex = EngraveHelper.findOptionIndex(
                        itemWheelList,
                        engraveConfigEntity.precision
                    )
                    itemEngraveConfigEntity = engraveConfigEntity
                }
            }

            //雕刻参数
            if (deviceStateModel.isPenMode()) {
                //握笔模块, 雕刻速度, 非雕刻深度
                engraveConfigEntity.power = 100 //功率必须100%
                engraveConfigEntity.lpSaveEntity()
                EngraveOptionWheelItem()() {
                    itemTag = MaterialEntity.SPEED
                    itemLabelText = _string(R.string.engrave_speed)
                    itemWheelList = EngraveHelper.percentList()
                    itemEngraveConfigEntity = engraveConfigEntity
                    itemSelectedIndex = EngraveHelper.findOptionIndex(
                        itemWheelList,
                        EngraveCmd.depthToSpeed(engraveConfigEntity.depth)
                    )
                }
            } else {
                //功率/深度/次数
                EngravePropertyItem()() {
                    itemEngraveConfigEntity = engraveConfigEntity
                }
            }

            EngraveConfirmItem()() {
                itemClick = {
                    //开始雕刻
                    checkEngraveNotify {
                        checkExDevice {
                            showFocalDistance(it.context) {
                                showSafetyTips(it.context) {
                                    asyncTimeoutExitCmd { bean, error ->
                                        if (error == null) {
                                            //开始雕刻
                                            onStartEngrave(taskId)
                                            /*engraveConfigProvider.onStartEngrave(this@EngraveFlowLayoutHelper)
                                            val taskEntity = engraveModel.startEngrave(taskId)
                                            if (taskEntity.dataIndexList.isNullOrEmpty()) {
                                                toastQQ(_string(R.string.no_data_engrave))
                                            } else {
                                                engraveFlow = ENGRAVE_FLOW_ENGRAVING
                                                renderFlowItems()
                                            }*/
                                        } else {
                                            toastQQ(error.message)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**[com.angcyo.canvas2.laser.pecker.engrave.EngraveFlowLayoutHelper.renderEngraving]*/
    fun renderEngraving() {
        val taskId = flowTaskId
        val transferConfigEntity = EngraveFlowDataHelper.getTransferConfig(taskId)
        updateIViewTitle(span {
            if (!transferConfigEntity?.name.isNullOrBlank()) {
                append(transferConfigEntity?.name)
                appendLine()
                append(_string(R.string.engraving)) {
                    fontSize = _titleFontSize * dpi
                }
            } else {
                append(_string(R.string.engraving))
            }
        })

        showCloseView(false)

        renderDslAdapter {
            PreviewTipItem()() {
                itemTip = _string(R.string.engrave_move_state_tips)
            }
            if (!laserPeckerModel.isCSeries()) {
                //非C1显示, 设备水平角度
                renderDeviceInfoIfNeed()
            }
            //强制显示模块信息
            PreviewExDeviceTipItem()() {
                itemEngraveConfigEntity = EngraveFlowDataHelper.getCurrentEngraveConfig(flowTaskId)
            }
            EngraveProgressItem()() {
                itemTaskId = flowTaskId
            }
            EngravingInfoItem()() {
                itemTaskId = flowTaskId
            }
            if (HawkEngraveKeys.enableSingleItemTransfer) {
                //激活单文件雕刻的情况下, 允许跳过当前雕刻的索引
                DslBlackButtonItem()() {
                    itemButtonText = _string(R.string.engrave_skip_current)
                    itemClick = {
                        //强制退出
                        engraveCanvasFragment?.fragment?.tgStrokeLoadingCaller { isCancel, loadEnd ->
                            ExitCmd(timeout = HawkEngraveKeys.receiveTimeoutMax).enqueue { bean, error ->
                                loadEnd(bean, error)
                                if (error != null) {
                                    toastQQ(error.message)
                                } else {
                                    engraveModel.finishCurrentIndexEngrave()
                                }
                            }
                        }
                    }
                }
            }
            //---雕刻控制-暂停-结束
            EngravingControlItem()() {
                itemTaskId = flowTaskId
                itemPauseAction = { isPause ->
                    if (isPause) {
                        if (!checkUnsafe()) {
                            engraveModel.continueEngrave()
                        }
                    } else {
                        engraveModel.pauseEngrave()
                    }
                }
                itemStopAction = {
                    //停止雕刻, 直接完成
                    engraveCanvasFragment?.fragment?.engraveLoadingAsyncTimeout({
                        syncSingle { countDownLatch ->
                            engraveModel.stopEngrave("来自点击按钮", countDownLatch)
                        }
                    })
                }
            }
        }
    }

}