package com.angcyo.canvas2.laser.pecker

import android.graphics.Path
import com.angcyo.base.contentView
import com.angcyo.base.dslAHelper
import com.angcyo.bluetooth.fsc.laserpacker.DeviceStateModel
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.command.EngravePreviewCmd
import com.angcyo.bluetooth.fsc.laserpacker.data.LaserPeckerProductInfo
import com.angcyo.bluetooth.fsc.laserpacker.data.OverflowInfo
import com.angcyo.bluetooth.fsc.laserpacker.data.isOverflowBounds
import com.angcyo.bluetooth.fsc.laserpacker.data.isOverflowLimit
import com.angcyo.bluetooth.fsc.laserpacker.parse.toDeviceStateString
import com.angcyo.bluetooth.fsc.laserpacker.parse.toLaserPeckerVersionName
import com.angcyo.bluetooth.fsc.laserpacker.writeEngraveLog
import com.angcyo.canvas.render.data.LimitInfo
import com.angcyo.canvas2.laser.pecker.engrave.EngraveInfoRenderer
import com.angcyo.canvas2.laser.pecker.manager.GuideManager
import com.angcyo.canvas2.laser.pecker.util.LPElementHelper
import com.angcyo.canvas2.laser.pecker.util.mmToRenderValueUnit
import com.angcyo.canvas2.laser.pecker.util.updateElementAfterEngrave
import com.angcyo.core.component.model.DataShareModel
import com.angcyo.core.vmApp
import com.angcyo.dialog.messageDialog
import com.angcyo.drawable.StateBarDrawable
import com.angcyo.engrave2.model.EngraveModel
import com.angcyo.engrave2.model.PreviewModel
import com.angcyo.http.tcp.TcpConnectInfo
import com.angcyo.laserpacker.device.DeviceHelper
import com.angcyo.laserpacker.device.ble.BluetoothSearchHelper
import com.angcyo.laserpacker.device.ble.DeviceConnectTipActivity
import com.angcyo.laserpacker.device.ble.DeviceSettingFragment
import com.angcyo.laserpacker.device.model.FscDeviceModel
import com.angcyo.library.L
import com.angcyo.library.annotation.CallPoint
import com.angcyo.library.canvas.core.Reason
import com.angcyo.library.component.StateLayoutInfo
import com.angcyo.library.component.StateLayoutManager
import com.angcyo.library.component.lastActivity
import com.angcyo.library.ex._color
import com.angcyo.library.ex._string
import com.angcyo.library.ex.computePathBounds
import com.angcyo.library.ex.elseNull
import com.angcyo.library.ex.have
import com.angcyo.library.ex.nowTime
import com.angcyo.library.ex.toPath
import com.angcyo.viewmodel.observe
import com.angcyo.widget.StateBarView
import com.angcyo.widget.span.span

/**
 * 产品相关的布局助手
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/25
 */
class ProductLayoutHelper(override val renderLayoutHelper: RenderLayoutHelper) : IControlHelper {

    companion object {

        const val TAG_MAIN = "main"
    }

    @CallPoint
    fun bindProductLayout() {
        //警示提示动画
        dangerWarningHelper.bindDangerWarning(renderLayoutHelper.renderFragment)

        val fragment = fragment

        //状态管理
        stateLayoutManager.group = _rootViewHolder?.group(R.id.canvas_device_state_wrap_layout)

        //监听通讯状态
        vmApp<DataShareModel>().shareStateOnceData.observe(fragment) { state ->
            if (state != null) {
                _rootViewHolder?.v<StateBarView>(R.id.state_bar_view)
                    ?.firstDrawable<StateBarDrawable>()?.state = state
            }
        }

        //监听产品信息
        laserPeckerModel.productInfoData.observe(fragment) { productInfo ->
            /*if (laserPeckerModel.initializeData.value == true) {
                _showProductLimit(canvasView, productInfo)
            }*/

            //设置位置限定
            if (productInfo == null) {
                LPElementHelper.POSITION_CUT_LEFT = 30f
                LPElementHelper.POSITION_CUT_TOP = LPElementHelper.POSITION_CUT_LEFT * 5
            } else {
                LPElementHelper.POSITION_CUT_LEFT = productInfo.widthPhys / 2f
                LPElementHelper.POSITION_CUT_TOP = productInfo.heightPhys / 2f
            }
        }

        //蓝牙设置改变后回调
        laserPeckerModel.updateSettingOnceData.observe(fragment) {
            if (it == true) {
                _showZRSLimit()
            }
        }
        //设备初始化后回调
        laserPeckerModel.initializeOnceData.observe(fragment) {
            if (it == true) {
                _showZRSLimit()
            }
        }

        //监听设备状态, Z/R/S连接状态 提示
        deviceStateModel.deviceStateData.observe(fragment) {
            //设备模式提示
            val stateString = it?.toDeviceStateString()
            val beforeMode = deviceStateModel.deviceStateData.beforeValue?.mode
            val mode = it?.mode

            if (stateString.isNullOrEmpty()) {
                stateLayoutManager.removeState(engraveStateInfo)
            } else if (it.isModeEngravePreview() && (laserPeckerModel.overflowInfoData.value?.overflowType?.isOverflowBounds() == true ||
                        laserPeckerModel.overflowInfoData.value?.overflowType?.isOverflowLimit() == true)
            ) {
                //预览模式下, 溢出了的情况
            } else {
                //模式改变
                engraveStateInfo.text = stateString
                engraveStateInfo.updateAnim = beforeMode != mode
                updateState(engraveStateInfo)
            }

            if (it?.isModeEngravePreview() != true) {
                //停止预览后, 清除状态
                laserPeckerModel.overflowInfoData.postValue(null)
            }
            if (it?.isModeShutdown() == true) {
                //关机后, 断开设备
                deviceStateModel.disconnectDevice(TcpConnectInfo(data = "设备状态:关机"))
            }

            it?.let {
                //有设备连接
                //stateLayoutManager.removeState(connectStateInfo)
                bindDeviceConnectTipLayout()
            }.elseNull {
                //无设备连接
                //updateState(connectStateInfo)
                bindDeviceConnectTipLayout()
            }
        }

        //监听设备正忙提示
        laserPeckerModel.deviceBusyOnceData.observe(fragment, allowBackward = false) { busy ->
            if (busy == true) {
                FscDeviceModel.disableAutoConnectToTime = nowTime() + 1 * 60 * 1000 //临时禁用自动连接1分钟
                fragment.fContext().messageDialog {
                    dialogMessage = _string(R.string.device_busy_tip)
                }
            }
        }

        //监听正在预览的矩形
        previewModel.previewInfoData.observe(fragment, allowBackward = false) { info ->
            if (!HawkEngraveKeys.enableLowMode) {
                val progressRenderer = renderDelegate?.renderManager?.progressRenderer
                if (info == null) {
                    progressRenderer?.updateVisible(false, Reason.code, null)
                } else {
                    progressRenderer?.apply {
                        updateVisible(true, Reason.code, null)
                        renderBorder = true
                        borderColor = DeviceHelper.PREVIEW_COLOR
                        borderBounds = info.originBounds
                    }
                }
            }
        }

        //监听范围预览
        laserPeckerModel.overflowInfoData.observe(fragment, allowBackward = false) {
            if (it != null && (it.overflowType.isOverflowBounds() || it.overflowType.isOverflowLimit())) {
                previewOverflowStateInfo.text = if (it.overflowType.isOverflowBounds()) {
                    if (it.overflowType.have(OverflowInfo.OVERFLOW_TYPE_HEIGHT)) {
                        _string(
                            R.string.out_of_height_bounds,
                            (laserPeckerModel.productInfoData.value?.deviceConfigBean?.sRepHeightPhys
                                ?: 0).toFloat().mmToRenderValueUnit()
                        )
                    } else {
                        _string(R.string.out_of_bounds)
                    }
                } else {
                    if (it.overflowType.have(OverflowInfo.OVERFLOW_TYPE_HEIGHT_LIMIT)) {
                        _string(
                            R.string.out_of_height_limit,
                            (laserPeckerModel.productInfoData.value?.deviceConfigBean?.bestHeightPhys
                                ?: 0).toFloat().mmToRenderValueUnit()
                        )
                    } else {
                        _string(R.string.out_of_limit)
                    }
                }
                updateState(previewOverflowStateInfo)
            } else {
                stateLayoutManager.removeState(previewOverflowStateInfo)
            }
        }

        //监听雕刻任务
        renderDelegate?.let {
            val taskRenderer = EngraveInfoRenderer.install(it)
            engraveModel.engraveStateData.observe(fragment, allowBackward = false) {
                it?.let {
                    if (it.state != EngraveModel.ENGRAVE_STATE_FINISH) {
                        //雕刻任务未完成
                        taskRenderer.engraveTaskId = it.taskId
                    } else {
                        taskRenderer.engraveTaskId = null
                    }
                    //雕刻任务的进度监听渲染
                    if (HawkEngraveKeys.enableRenderEngraveInfo) {
                        renderDelegate?.refresh()
                    }

                    //雕刻图层的进度监听
                    if (HawkEngraveKeys.enableLayerEngraveInfo) {
                        renderLayoutHelper.updateEngraveLayerListLayout()
                    }

                    //雕刻任务完成后, 更新变量文本
                    if (it.state == EngraveModel.ENGRAVE_STATE_FINISH) {
                        "雕刻完成, 更新变量文本".writeEngraveLog(L.INFO)
                        renderDelegate?.updateElementAfterEngrave()

                        GuideManager.checkOrShowGuide(
                            lastActivity?.window?.contentView(),
                            null,
                            6,
                            300
                        )
                    }
                }
            }
        }

        //发送一次初始化成功的事件
        laserPeckerModel.initializeData.value?.let {
            if (it) {
                laserPeckerModel.initializeOnceData.postValue(true)
            }
        }
    }

    /**更新动画*/
    private fun updateState(stateInfo: StateLayoutInfo) {
        val anim = !HawkEngraveKeys.enableLowMode
        stateInfo.clipAnim = anim
        stateInfo.rotateAnim = anim

        stateLayoutManager.updateState(stateInfo)
    }

    //region ---内部操作---

    //产品模式
    val laserPeckerModel = vmApp<LaserPeckerModel>()
    val deviceStateModel = vmApp<DeviceStateModel>()

    //雕刻模式
    val engraveModel = vmApp<EngraveModel>()

    //预览模式
    val previewModel = vmApp<PreviewModel>()

    //---

    //状态管理
    val stateLayoutManager = StateLayoutManager()

    //警示提示动画
    val dangerWarningHelper = DangerWarningHelper()

    //雕刻状态信息
    val engraveStateInfo = StateLayoutInfo()

    //预览超范围状态信息
    val previewOverflowStateInfo = StateLayoutInfo()

    /**显示产品限制框*/
    private fun _showProductLimit(productInfo: LaserPeckerProductInfo?) {
        if (productInfo == null) {
            renderDelegate?.clearLimitRender()
        } else {
            _updateProductLimit(productInfo)
        }
    }

    private fun _updateProductLimit(productInfo: LaserPeckerProductInfo) {
        val limitList = mutableListOf<LimitInfo>()
        //最佳预览尺寸用蓝色提示
        val limitPath: Path = productInfo.limitPath
        EngravePreviewCmd.getLimitPath(productInfo, true, true)?.let {
            limitList.add(LimitInfo(it, it.computePathBounds(), DeviceHelper.PREVIEW_COLOR))
        }
        limitList.add(
            LimitInfo(limitPath, limitPath.computePathBounds(), DeviceHelper.PREVIEW_COLOR)
        )
        //物理尺寸用红色提示
        limitList.add(
            LimitInfo(
                productInfo.bounds.toPath(),
                productInfo.bounds,
                DeviceHelper.ENGRAVE_COLOR,
                tag = TAG_MAIN
            )
        )

        renderDelegate?.resetLimitRender(limitList)
        renderDelegate?.showRectBounds(productInfo.bounds, offsetRectTop = true)
    }

    /**显示Z/R/S轴限制框*/
    private fun _showZRSLimit() {
        val productInfo = laserPeckerModel.productInfoData.value
        val tipPath: Path? = EngravePreviewCmd.getLimitPath(productInfo, false, true)
        val limitPath: Path? = EngravePreviewCmd.getLimitPath(productInfo, false)

        if (productInfo != null && limitPath != null) {
            //追加显示Z轴显示框
            val limitList = mutableListOf<LimitInfo>()
            tipPath?.let {
                limitList.add(LimitInfo(it, it.computePathBounds(), DeviceHelper.PREVIEW_COLOR))
            }
            limitList.add(
                LimitInfo(limitPath, limitPath.computePathBounds(), DeviceHelper.ENGRAVE_COLOR)
            )
            limitList.add(
                LimitInfo(
                    productInfo.bounds.toPath(),
                    productInfo.bounds,
                    enableRender = false,
                    tag = TAG_MAIN
                )
            )

            renderDelegate?.resetLimitRender(limitList)
            renderDelegate?.showRectBounds(productInfo.bounds, offsetRectTop = true)
        } else {
            _showProductLimit(productInfo)
        }
    }

    /**显示连接的设备信息提示*/
    private fun bindDeviceConnectTipLayout() {
        val viewHolder = _rootViewHolder ?: return
        if (deviceStateModel.deviceStateData.value == null) {
            //无设备连接
            viewHolder.img(R.id.device_image_view)
                ?.setImageResource(R.drawable.canvas_device_warn_svg)
            viewHolder.tv(R.id.device_name_view)?.text = _string(R.string.blue_no_device_connected)
        } else {
            val name =
                DeviceConnectTipActivity.formatDeviceName(laserPeckerModel.productInfoData.value?.deviceName)
            viewHolder.img(R.id.device_image_view)
                ?.setImageResource(DeviceConnectTipActivity.getDeviceImageRes(name))
            viewHolder.tv(R.id.device_name_view)?.text = span {
                append(name)
                laserPeckerModel.productInfoData.value?.let {
                    if (it.version > 0) {
                        append(" ${it.version.toLaserPeckerVersionName()}") {
                            foregroundColor = _color(R.color.text_sub_color)
                            relativeSizeScale = 0.7f
                        }
                    }
                }
            }
        }
        //显示蓝牙界面
        viewHolder.throttleClick(R.id.device_tip_wrap_layout) {
            if (laserPeckerModel.initializeData.value == true && flowLayoutHelper.isAttach()) {
                //界面已经显示, 并且有设备连接, 则不允许切换蓝牙设备
                return@throttleClick
            } else {
                BluetoothSearchHelper.checkAndSearchDevice(fragment)
            }
        }

        //显示设备设置界面
        viewHolder.throttleClick(R.id.device_setting_view) {
            if (flowLayoutHelper.isAttach()) {
                return@throttleClick
            } else if (vmApp<DeviceStateModel>().isDeviceConnect()) {
                fragment.dslAHelper {
                    start(DeviceSettingFragment::class.java)
                }
            } else {
                viewHolder.clickCallView(R.id.device_tip_wrap_layout)
            }
        }
    }

    //endregion ---内部操作---

}