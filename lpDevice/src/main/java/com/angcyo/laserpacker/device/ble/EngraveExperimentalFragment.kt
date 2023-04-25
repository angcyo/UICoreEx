package com.angcyo.laserpacker.device.ble

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.item.component.DebugAction
import com.angcyo.item.component.DebugFragment
import com.angcyo.laserpacker.device.R
import com.angcyo.library.component.hawk.LibHawkKeys
import com.angcyo.library.component.hawk.LibLpHawkKeys
import com.angcyo.library.ex._string

/**
 * 实验性功能
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/12/09
 */
class EngraveExperimentalFragment : DebugFragment() {

    init {
        fragmentTitle = _string(R.string.engrave_experimental)

        fragmentConfig.isLightStyle = true
        fragmentConfig.showTitleLineView = true
    }

    override fun initBaseView(savedInstanceState: Bundle?) {
        super.initBaseView(savedInstanceState)
    }

    override fun onInitFragment(savedInstanceState: Bundle?) {
        fragmentConfig.fragmentBackgroundDrawable = ColorDrawable(Color.WHITE)
        super.onInitFragment(savedInstanceState)
    }

    override fun renderActions() {
        renderDslAdapter {

            renderDebugAction(DebugAction().apply {
                label = "激活传输数据时的索引检查"
                des = "激活后,如果设备上已存在数据,则不重新传输."
                key = HawkEngraveKeys::enableTransferIndexCheck.name
                type = Boolean::class.java
                defValue = HawkEngraveKeys.enableTransferIndexCheck
            })

            renderDebugAction(DebugAction().apply {
                label = "激活第三方GCode数据全转换"
                des = "激活后,将会重新生成第三方GCode数据,而不是在原始数据基础上修改."
                key = HawkEngraveKeys::enableGCodeTransform.name
                type = Boolean::class.java
                defValue = HawkEngraveKeys.enableGCodeTransform
            })

            renderDebugAction(DebugAction().apply {
                label = "激活GCode G2/G3指令输出"
                des = "激活后,矢量图形转GCode算法时,将输出G2/G3指令,否则仅使用G0/G1指令."
                key = LibLpHawkKeys::enableVectorArc.name
                type = Boolean::class.java
                defValue = LibLpHawkKeys.enableVectorArc
            })

            renderDebugAction(DebugAction().apply {
                label = "激活从上往下雕刻"
                des = "激活后,元素的雕刻顺序按照坐标从上往下,否则按照规定的图层顺序雕刻."
                key = HawkEngraveKeys::enableItemTopOrder.name
                type = Boolean::class.java
                defValue = HawkEngraveKeys.enableItemTopOrder
            })

            renderDebugAction(DebugAction().apply {
                label = "激活雕刻时的信息渲染"
                des = "激活后,将在雕刻时渲染对应元素的雕刻序号,进度,以及对应边框."
                key = HawkEngraveKeys::enableRenderEngraveInfo.name
                type = Boolean::class.java
                defValue = HawkEngraveKeys.enableRenderEngraveInfo
            })

            renderDebugAction(DebugAction().apply {
                label = "激活单元素雕刻参数"
                des = "激活后,每个元素都可以单独设置雕刻参数."
                key = HawkEngraveKeys::enableItemEngraveParams.name
                type = Boolean::class.java
                defValue = HawkEngraveKeys.enableItemEngraveParams
            })

            renderDebugAction(DebugAction().apply {
                label = "激活单元素传输雕刻"
                des = "激活后,一个元素一个元素传输和雕刻."
                key = HawkEngraveKeys::enableSingleItemTransfer.name
                type = Boolean::class.java
                defValue = HawkEngraveKeys.enableSingleItemTransfer

                action = { _, value ->
                    if (value is Boolean) {
                        LibHawkKeys.enableCanvasRenderLimit = !value
                    }
                }
            })

        }
    }
}