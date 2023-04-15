package com.angcyo.canvas2.laser.pecker.history

import android.os.Bundle
import android.view.ViewGroup
import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas2.laser.pecker.DangerWarningHelper
import com.angcyo.canvas2.laser.pecker.IEngraveRenderFragment
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.engrave.EngraveFlowLayoutHelper
import com.angcyo.core.fragment.BasePagerFragment
import com.angcyo.fragment.AbsLifecycleFragment
import com.angcyo.library.ex._string

/**
 * 历史文档界面
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/07/05
 */
class EngraveHistoryFragment : BasePagerFragment(), IEngraveRenderFragment {

    init {
        fragmentTitle = _string(R.string.ui_slip_menu_history)
        fragmentConfig.isLightStyle = true

        addPage(_string(R.string.app_history_title), EngraveAppHistoryFragment::class.java)
        addPage(_string(R.string.device_history_title), EngraveDeviceHistoryFragment::class.java)
    }

    //警示提示动画
    val dangerWarningHelper = DangerWarningHelper()

    override fun initBaseView(savedInstanceState: Bundle?) {
        super.initBaseView(savedInstanceState)
        //
        dangerWarningHelper.bindDangerWarning(this)
    }

    override val fragment: AbsLifecycleFragment
        get() = this
    override val renderDelegate: CanvasRenderDelegate?
        get() = null
    override val engraveFlowLayoutHelper: EngraveFlowLayoutHelper
        get() = EngraveFlowLayoutHelper()
    override val flowLayoutContainer: ViewGroup?
        get() = null
    override val dangerLayoutContainer: ViewGroup?
        get() = _vh.group(R.id.lib_content_wrap_layout)
}