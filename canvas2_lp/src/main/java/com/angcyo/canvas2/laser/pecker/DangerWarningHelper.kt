package com.angcyo.canvas2.laser.pecker

import android.view.ViewGroup
import com.angcyo.bluetooth.fsc.laserpacker.DeviceStateModel
import com.angcyo.core.vmApp
import com.angcyo.drawable.DangerWarningDrawable
import com.angcyo.laserpacker.device.DeviceHelper
import com.angcyo.library.annotation.CallPoint
import com.angcyo.library.ex.alphaRatio
import com.angcyo.library.ex.elseNull
import com.angcyo.library.ex.removeFromParent
import com.angcyo.widget.loading.DangerWarningView

/**
 * 设备警示动画助手
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/04/15
 */
class DangerWarningHelper {

    //雕刻提示
    var dangerWarningView: DangerWarningView? = null

    val deviceStateModel = vmApp<DeviceStateModel>()

    @CallPoint
    fun bindDangerWarning(renderFragment: IEngraveRenderFragment) {
        val fragment = renderFragment.fragment
        //监听设备状态, Z/R/S连接状态
        deviceStateModel.deviceStateData.observe(fragment) {
            //设备模式提示
            //警示提示动画
            it?.let {
                val rootLayout = renderFragment.dangerLayoutContainer
                //提示
                if (rootLayout == null) {
                    //no op
                } else if (it.isModeEngravePreview()) {
                    //预览模式
                    showDangerWaring(rootLayout, DeviceHelper.PREVIEW_COLOR.alphaRatio(0.4f))
                } else if (it.isModeEngrave()) {
                    //雕刻模式
                    showDangerWaring(rootLayout, DeviceHelper.ENGRAVE_COLOR.alphaRatio(0.4f))
                } else {
                    //空闲
                    dangerWarningView?.removeFromParent()
                }

            }.elseNull {
                dangerWarningView?.removeFromParent()
            }
        }
    }

    /**显示危险提示view*/
    private fun showDangerWaring(rootLayout: ViewGroup, color: Int) {
        if (dangerWarningView == null) {
            dangerWarningView = DangerWarningView(rootLayout.context)
        }
        dangerWarningView?.firstDrawable<DangerWarningDrawable>()?.warnColor = color
        if (dangerWarningView?.parent == null) {
            rootLayout.addView(dangerWarningView, -1, -1)
        }
    }
}