package com.angcyo.engrave

import android.graphics.Color
import android.view.ViewGroup
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.data.LaserPeckerProductInfo
import com.angcyo.canvas.CanvasView
import com.angcyo.canvas.core.CanvasEntryPoint
import com.angcyo.core.vmApp
import com.angcyo.drawable.DangerWarningDrawable
import com.angcyo.fragment.AbsLifecycleFragment
import com.angcyo.library.ex.alphaRatio
import com.angcyo.library.ex.elseNull
import com.angcyo.library.ex.removeFromParent
import com.angcyo.widget.loading.DangerWarningView

/**
 * 监听产品信息, 并做出一些界面响应
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/07/11
 */
class ProductLayoutHelper(val fragment: AbsLifecycleFragment) {

    //产品模式
    val laserPeckerModel = vmApp<LaserPeckerModel>()

    //雕刻提示
    var dangerWarningView: DangerWarningView? = null

    /**绑定布局*/
    @CanvasEntryPoint
    fun bindCanvasView(rootLayout: ViewGroup, canvasView: CanvasView?) {
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
        laserPeckerModel.deviceStateData.observe(fragment) {
            val before = laserPeckerModel.deviceStateData.beforeValue?.zConnect ?: 0
            if (before != it?.zConnect) {
                //z轴连接状态改变后, 检查是否要限制z轴限制
                _showZLimit(canvasView)
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
            }.elseNull {
                dangerWarningView?.removeFromParent()
            }
        }
    }

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

}