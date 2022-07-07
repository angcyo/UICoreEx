package com.angcyo.engrave

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.canvas.CanvasDelegate
import com.angcyo.core.vmApp
import com.angcyo.iview.BaseRecyclerIView
import com.angcyo.library.component._delay

/**
 * 雕刻相关布局助手
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/07
 */
abstract class BaseEngraveLayoutHelper : BaseRecyclerIView() {

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
}