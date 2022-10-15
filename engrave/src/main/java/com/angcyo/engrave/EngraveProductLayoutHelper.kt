package com.angcyo.engrave

import android.graphics.Color
import android.graphics.Path
import android.view.ViewGroup
import com.angcyo.base.dslAHelper
import com.angcyo.bluetooth.fsc.FscBleApiModel
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.data.LaserPeckerProductInfo
import com.angcyo.bluetooth.fsc.laserpacker.parse.toDeviceStateString
import com.angcyo.bluetooth.fsc.laserpacker.parse.toLaserPeckerVersionName
import com.angcyo.canvas.CanvasView
import com.angcyo.canvas.Strategy
import com.angcyo.canvas.core.CanvasEntryPoint
import com.angcyo.core.component.dslPermissions
import com.angcyo.core.vmApp
import com.angcyo.drawable.DangerWarningDrawable
import com.angcyo.engrave.ble.DeviceConnectTipActivity
import com.angcyo.engrave.ble.DeviceSettingFragment
import com.angcyo.engrave.ble.bluetoothSearchListDialog
import com.angcyo.engrave.model.EngraveModel
import com.angcyo.engrave.model.PreviewModel
import com.angcyo.library.component.StateLayoutInfo
import com.angcyo.library.component.StateLayoutManager
import com.angcyo.library.ex.*
import com.angcyo.library.toast
import com.angcyo.viewmodel.observe
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.loading.DangerWarningView
import com.angcyo.widget.span.span

/**
 * 监听产品信息, 并做出一些界面响应
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/07/11
 */
class EngraveProductLayoutHelper(val engraveCanvasFragment: IEngraveCanvasFragment) {

    companion object {
        /**预览的提示颜色和蚂蚁线的颜色*/
        const val PREVIEW_COLOR = Color.BLUE

        /**雕刻颜色*/
        const val ENGRAVE_COLOR = Color.RED
    }

    //产品模式
    val laserPeckerModel = vmApp<LaserPeckerModel>()

    //雕刻模式
    val engraveModel = vmApp<EngraveModel>()

    //预览模式
    val previewModel = vmApp<PreviewModel>()

    //雕刻提示
    var dangerWarningView: DangerWarningView? = null

    /**绑定布局*/
    @CanvasEntryPoint
    fun bindCanvasView(viewHolder: DslViewHolder, rootLayout: ViewGroup, canvasView: CanvasView?) {
        val fragment = engraveCanvasFragment.fragment

        //状态管理
        stateLayoutManager.group = viewHolder.group(R.id.canvas_device_state_wrap_layout)

        //监听产品信息
        laserPeckerModel.productInfoData.observe(fragment) { productInfo ->
            _showProductLimit(canvasView, productInfo)
        }

        //蓝牙设置改变后回调
        laserPeckerModel.updateSettingOnceData.observe(fragment) {
            if (it == true) {
                _showZRSLimit(canvasView)
            }
        }
        //设备初始化后回调
        laserPeckerModel.initializeOnceData.observe(fragment) {
            if (it == true) {
                _showZRSLimit(canvasView)
            }
        }

/*
        //监听Z轴/R轴/S轴设置状态
        laserPeckerModel.deviceSettingData.observe(engraveCanvasFragment.fragment) {
            val beforeZ = laserPeckerModel.deviceSettingData.beforeValue?.zFlag ?: 0
            val beforeR = laserPeckerModel.deviceSettingData.beforeValue?.rFlag ?: 0
            val beforeS = laserPeckerModel.deviceSettingData.beforeValue?.sFlag ?: 0
            if (beforeZ != it?.zFlag || beforeR != it.rFlag || beforeS != it.sFlag) {
                //z轴开关改变后, 检查是否要限制z轴限制
                _showZRSLimit(canvasView)
            }
        }
*/

        //监听设备状态, Z/R/S连接状态
        laserPeckerModel.deviceStateData.observe(fragment) {
            /* val beforeZ = laserPeckerModel.deviceStateData.beforeValue?.zConnect ?: 0
             val beforeR = laserPeckerModel.deviceStateData.beforeValue?.rConnect ?: 0
             val beforeS = laserPeckerModel.deviceStateData.beforeValue?.sConnect ?: 0
             if (beforeZ != it?.zConnect || beforeR != it.rConnect || beforeS != it.sConnect) {
                 //z轴连接状态改变后, 检查是否要限制z轴限制
                 _showZRSLimit(canvasView)
             }*/

            //设备模式提示
            val stateString = it?.toDeviceStateString()
            val beforeMode = laserPeckerModel.deviceStateData.beforeValue?.mode
            val mode = it?.mode

            if (stateString.isNullOrEmpty()) {
                stateLayoutManager.removeState(engraveStateInfo)
            } else {
                //模式改变
                engraveStateInfo.text = stateString
                engraveStateInfo.updateAnim = beforeMode != mode
                stateLayoutManager.updateState(engraveStateInfo)
            }

            if (it?.isModeEngravePreview() != true) {
                //停止预览后, 清除状态
                laserPeckerModel.overflowInfoData.postValue(null)
            }

            //警示提示动画
            it?.let {
                //提示
                if (it.isModeEngravePreview()) {
                    //预览模式
                    showDangerWaring(rootLayout, PREVIEW_COLOR.alphaRatio(0.4f))
                } else if (it.isModeEngrave()) {
                    //雕刻模式
                    showDangerWaring(rootLayout, ENGRAVE_COLOR.alphaRatio(0.4f))
                } else {
                    //空闲
                    dangerWarningView?.removeFromParent()
                }

                //有设备连接
                //stateLayoutManager.removeState(connectStateInfo)
                initDeviceConnectTipLayout(viewHolder)
            }.elseNull {
                dangerWarningView?.removeFromParent()

                //无设备连接
                //stateLayoutManager.updateState(connectStateInfo)
                initDeviceConnectTipLayout(viewHolder)
            }
        }

        /*//监听item雕刻进度
        engraveModel.engraveItemData.observe(fragment, allowBackward = false) { info ->
            info?.let {
                canvasView?.canvasDelegate?.progressRenderer?.let {
                    it.progressRenderer = canvasView.canvasDelegate.getRendererItem(info.uuid)
                    it.progress = info.progress
                }
            }
        }*/

        //监听正在预览的矩形
        previewModel.previewInfoData.observe(fragment, allowBackward = false) { info ->
            val canvasDelegate = canvasView?.canvasDelegate
            if (info == null) {
                canvasDelegate?.progressRenderer?.setVisible(false, Strategy.preview)
            } else {
                canvasDelegate?.progressRenderer?.apply {
                    setVisible(true, Strategy.preview)
                    borderRectRotate = info.rotate
                    borderColor = PREVIEW_COLOR
                    borderRect = if (info.rotate == null) {
                        //非4点预览
                        info.rotateBounds
                    } else {
                        //4点预览
                        info.originBounds
                    }
                }
            }
        }

        //监听范围预览
        laserPeckerModel.overflowInfoData.observe(fragment, allowBackward = false) {
            if (it != null && (it.isOverflowBounds || it.isOverflowLimit)) {
                previewOverflowStateInfo.text = if (it.isOverflowBounds) {
                    _string(R.string.out_of_bounds)
                } else {
                    _string(R.string.out_of_limit)
                }
                stateLayoutManager.updateState(previewOverflowStateInfo)
            } else {
                stateLayoutManager.removeState(previewOverflowStateInfo)
            }
        }
    }

    //region ---内部操作---

    /**显示产品限制框*/
    fun _showProductLimit(canvasView: CanvasView?, productInfo: LaserPeckerProductInfo?) {
        if (canvasView == null) {
            return
        }
        if (productInfo == null) {
            canvasView.canvasDelegate.limitRenderer.clear()
        } else {
            if (productInfo.isOriginCenter) {
                canvasView.canvasDelegate.moveOriginToCenter()
            } else {
                canvasView.canvasDelegate.moveOriginToLT()
            }
            _updateProductLimit(canvasView, productInfo)
        }
    }

    fun _updateProductLimit(canvasView: CanvasView, productInfo: LaserPeckerProductInfo) {
        //物理尺寸用红色提示
        canvasView.canvasDelegate.showAndResetLimitBounds(productInfo.bounds.toPath()) {
            isPrimary = true
            limitStrokeColor = ENGRAVE_COLOR
        }
        //最佳预览尺寸用蓝色提示
        canvasView.canvasDelegate.addAndShowLimitBounds(productInfo.limitPath) {
            limitStrokeColor = PREVIEW_COLOR
        }
    }

    /**显示Z/R/S轴限制框*/
    fun _showZRSLimit(canvasView: CanvasView?) {
        if (canvasView == null) {
            return
        }
        val productInfo = laserPeckerModel.productInfoData.value
        val zLimitPath = productInfo?.zLimitPath
        val rLimitPath = productInfo?.rLimitPath
        val sLimitPath = productInfo?.sLimitPath

        var limitPath: Path? = null
        if (productInfo != null) {
            if (laserPeckerModel.isZOpen() && zLimitPath != null) {
                //Z轴连接
                limitPath = zLimitPath
            } else if (laserPeckerModel.isROpen() && rLimitPath != null) {
                //R轴连接
                limitPath = rLimitPath
            } else if (laserPeckerModel.isSOpen() && sLimitPath != null) {
                //S轴连接
                limitPath = sLimitPath
            }
        }

        if (productInfo != null && limitPath != null) {
            //追加显示Z轴显示框

            canvasView.canvasDelegate.showAndResetLimitBounds(productInfo.bounds.toPath()) {
                isPrimary = true
                enableRender = false
            }
            canvasView.canvasDelegate.addAndShowLimitBounds(limitPath) {
                limitStrokeColor = ENGRAVE_COLOR
            }
        } else {
            _showProductLimit(canvasView, productInfo)
        }
    }

    /**显示危险提示view*/
    fun showDangerWaring(rootLayout: ViewGroup, color: Int) {
        if (dangerWarningView == null) {
            dangerWarningView = DangerWarningView(rootLayout.context)
        }
        dangerWarningView?.firstDrawable<DangerWarningDrawable>()?.warnColor = color
        if (dangerWarningView?.parent == null) {
            rootLayout.addView(dangerWarningView, -1, -1)
        }
    }

    /**显示连接的设备信息提示*/
    fun initDeviceConnectTipLayout(viewHolder: DslViewHolder) {
        if (laserPeckerModel.deviceStateData.value == null) {
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
            if (engraveCanvasFragment.engraveFlowLayoutHelper.isAttach()) {
                return@throttleClick
            } else {
                engraveCanvasFragment.fragment.dslPermissions(FscBleApiModel.bluetoothPermissionList()) { allGranted, foreverDenied ->
                    if (allGranted) {
                        engraveCanvasFragment.fragment.fContext().bluetoothSearchListDialog {
                            connectedDismiss = true
                        }
                    } else {
                        toast("cancel")
                    }
                }
            }
        }
        //显示设备设置界面
        viewHolder.throttleClick(R.id.device_setting_view) {
            if (engraveCanvasFragment.engraveFlowLayoutHelper.isAttach()) {
                return@throttleClick
            } else if (vmApp<FscBleApiModel>().haveDeviceConnected()) {
                engraveCanvasFragment.fragment.dslAHelper {
                    start(DeviceSettingFragment::class.java)
                }
            } else {
                viewHolder.clickCallView(R.id.device_tip_wrap_layout)
            }
        }
    }

    //状态管理
    val stateLayoutManager = StateLayoutManager()

    //雕刻状态信息
    val engraveStateInfo = StateLayoutInfo()

    //预览超范围状态信息
    val previewOverflowStateInfo = StateLayoutInfo()

    //endregion ---内部操作---

}