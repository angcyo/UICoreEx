package com.angcyo.canvas2.laser.pecker.engrave

import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.library.ex._string

/**
 * 简单的雕刻流程控制, 简单的预览, 简单的雕刻
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2023/08/05
 */
class SingleFlowLayoutHelper : BaseEngraveLayoutHelper() {

    init {
        engraveFlow
        selectLayerId = HawkEngraveKeys.lastLayerId
        singleFlowInfo //必须赋值
    }

    override fun renderEngraveConfig() {
        super.renderEngraveConfig()
        engraveBackFlow = 0
        showCloseView(true, _string(R.string.ui_quit))
    }

    override fun renderEngraving() {
        super.renderEngraving()
        engraveBackFlow = 0
        showCloseView(true, _string(R.string.ui_quit))
    }

    override fun renderEngraveFinish() {
        super.renderEngraveFinish()
        engraveBackFlow = 0
        showCloseView(true, _string(R.string.ui_quit))
    }
}