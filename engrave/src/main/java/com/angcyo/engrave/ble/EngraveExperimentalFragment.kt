package com.angcyo.engrave.ble

import android.os.Bundle
import com.angcyo.engrave.R
import com.angcyo.engrave.data.HawkEngraveKeys
import com.angcyo.item.component.DebugAction
import com.angcyo.item.component.DebugFragment
import com.angcyo.library.component.LibHawkKeys
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

    override fun renderActions() {
        renderDslAdapter {

            renderDebugAction(DebugAction().apply {
                label = "激活GCode G2/G3指令输出"
                des = "矢量图形转GCode算法时,是否激活G2/G3指令"
                key = LibHawkKeys::enableVectorArc.name
                type = Boolean::class.java
                defValue = LibHawkKeys.enableVectorArc
            })

            renderDebugAction(DebugAction().apply {
                label = "激活传输数据时的索引检查"
                des = "关闭后,所有数据直接传输,不检查机器是否已存在数据."
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
        }
    }
}