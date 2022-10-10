package com.angcyo.engrave

import android.graphics.Color
import android.view.ViewGroup
import com.angcyo.bluetooth.fsc.FscBleApiModel
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.data.LaserPeckerProductInfo
import com.angcyo.bluetooth.fsc.laserpacker.parse.toDeviceStateString
import com.angcyo.canvas.CanvasView
import com.angcyo.canvas.core.CanvasEntryPoint
import com.angcyo.core.component.dslPermissions
import com.angcyo.core.vmApp
import com.angcyo.drawable.DangerWarningDrawable
import com.angcyo.engrave.ble.DeviceConnectTipActivity
import com.angcyo.engrave.ble.bluetoothSearchListDialog
import com.angcyo.engrave.model.EngraveModel
import com.angcyo.engrave.model.PreviewModel
import com.angcyo.fragment.AbsLifecycleFragment
import com.angcyo.library.component.StateLayoutInfo
import com.angcyo.library.component.StateLayoutManager
import com.angcyo.library.ex._string
import com.angcyo.library.ex.alphaRatio
import com.angcyo.library.ex.elseNull
import com.angcyo.library.ex.removeFromParent
import com.angcyo.library.toast
import com.angcyo.viewmodel.observe
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.loading.DangerWarningView

/**
 * 监听产品信息, 并做出一些界面响应
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/07/11
 */
class EngraveProductLayoutHelper(val fragment: AbsLifecycleFragment) {

    //产品模式
    val laserPeckerModel = vmApp<LaserPeckerModel>()

    //雕刻模式
    val engraveModel = vmApp<EngraveModel>()
    val previewModel = vmApp<PreviewModel>()

    //雕刻提示
    var dangerWarningView: DangerWarningView? = null

    /**绑定布局*/
    @CanvasEntryPoint
    fun bindCanvasView(viewHolder: DslViewHolder, rootLayout: ViewGroup, canvasView: CanvasView?) {

        //状态管理
        stateLayoutManager.group = viewHolder.group(R.id.canvas_device_state_wrap_layout)

        //监听产品信息
        laserPeckerModel.productInfoData.observe(fragment) { productInfo ->
            _showProductLimit(canvasView, productInfo)
        }

        //监听Z轴
        laserPeckerModel.deviceSettingData.observe(fragment) {
            val before = laserPeckerModel.deviceSettingData.beforeValue?.zFlag ?: 0
            if (before != it?.zFlag) {
                //z轴开关改变后, 检查是否要限制z轴限制
                _showZLimit(canvasView)
            }
        }

        //监听设备状态
        laserPeckerModel.deviceStateData.observe(fragment) {

            val before = laserPeckerModel.deviceStateData.beforeValue?.zConnect ?: 0
            if (before != it?.zConnect) {
                //z轴连接状态改变后, 检查是否要限制z轴限制
                _showZLimit(canvasView)
            }

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
                    showDangerWaring(rootLayout, Color.BLUE.alphaRatio(0.4f))
                } else if (it.isModeEngrave()) {
                    //雕刻模式
                    showDangerWaring(rootLayout, Color.RED.alphaRatio(0.4f))
                } else {
                    //空闲
                    dangerWarningView?.removeFromParent()
                }

                //有设备连接
                //stateLayoutManager.removeState(connectStateInfo)
                showDeviceTip(viewHolder)
            }.elseNull {
                dangerWarningView?.removeFromParent()

                //无设备连接
                //stateLayoutManager.updateState(connectStateInfo)
                showDeviceTip(viewHolder)
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
        }

        //监听正在预览的item
        engraveModel.engraveItemInfoData.observe(fragment, allowBackward = false) { info ->
            info?.let {
                canvasView?.canvasDelegate?.progressRenderer?.let {
                    it.borderRenderer = canvasView.canvasDelegate.getRendererItem(info.uuid)
                    //是否激活绘制旋转后的边框
                    it.drawRotateBorder = previewModel.previewInfoData.value?.rotate != null
                }
            }
        }*/

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
            canvasView.canvasDelegate.showAndLimitBounds(productInfo.limitPath)
        }
    }

    /**显示Z轴限制框*/
    fun _showZLimit(canvasView: CanvasView?) {
        if (canvasView == null) {
            return
        }
        val productInfo = laserPeckerModel.productInfoData.value
        val zLimitPath = productInfo?.zLimitPath
        if (laserPeckerModel.isZOpen() && productInfo != null && zLimitPath != null) {
            //Z轴连接
            //追加显示Z轴显示框
            canvasView.canvasDelegate.limitRenderer.apply {
                updateLimit {
                    addPath(zLimitPath)
                }
                limitBounds = productInfo.bounds
            }
            canvasView.canvasDelegate.showRectBounds(productInfo.bounds)
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

    /**显示连接的提示*/
    fun showDeviceTip(viewHolder: DslViewHolder) {
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
            viewHolder.tv(R.id.device_name_view)?.text = name
        }
        //显示蓝牙界面
        viewHolder.click(R.id.device_tip_wrap_layout) {
            fragment.dslPermissions(FscBleApiModel.bluetoothPermissionList()) { allGranted, foreverDenied ->
                if (allGranted) {
                    fragment.fContext().bluetoothSearchListDialog {
                        connectedDismiss = true
                    }
                } else {
                    toast("cancel")
                }
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