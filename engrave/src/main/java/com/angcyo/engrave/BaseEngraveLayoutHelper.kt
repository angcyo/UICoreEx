package com.angcyo.engrave

import android.content.Context
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.canvas.CanvasDelegate
import com.angcyo.core.vmApp
import com.angcyo.dialog.messageDialog
import com.angcyo.iview.BaseRecyclerIView
import com.angcyo.library.component._delay
import com.angcyo.library.ex._drawable
import com.angcyo.library.ex._string

/**
 * 雕刻相关布局助手
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/07
 */
abstract class BaseEngraveLayoutHelper : BaseRecyclerIView() {

    /**用来获取预览的元素Bounds*/
    var canvasDelegate: CanvasDelegate? = null

    //产品模式
    val laserPeckerModel = vmApp<LaserPeckerModel>()

    /**是否循环检测设备状态*/
    var loopCheckDeviceState: Boolean = false

    /**持续检查工作作态*/
    fun checkDeviceState() {
        _delay(1_000) {
            //延迟1秒后, 继续查询状态
            laserPeckerModel.queryDeviceState() { bean, error ->
                if (error != null || loopCheckDeviceState) {
                    //出现了错误, 继续查询
                    checkDeviceState()
                }
            }
        }
    }

    /**显示预览安全提示框*/
    fun showPreviewSafetyTips(context: Context, action: () -> Unit) {
        context.messageDialog {
            dialogMessageLeftIco = _drawable(R.mipmap.safe_tips)
            dialogTitle = _string(R.string.size_safety_tips)
            dialogMessage = _string(R.string.size_safety_content)
            negativeButtonText = _string(R.string.dialog_negative)

            positiveButton { dialog, dialogViewHolder ->
                dialog.dismiss()
                action()
            }
        }
    }
}