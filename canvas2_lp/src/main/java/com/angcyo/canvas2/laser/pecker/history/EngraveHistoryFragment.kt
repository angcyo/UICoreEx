package com.angcyo.canvas2.laser.pecker.history

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import com.angcyo.bluetooth.fsc.enqueue
import com.angcyo.bluetooth.fsc.laserpacker.command.FileModeCmd
import com.angcyo.bluetooth.fsc.laserpacker.parse.FileTransferParser
import com.angcyo.bluetooth.fsc.parse
import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas2.laser.pecker.DangerWarningHelper
import com.angcyo.canvas2.laser.pecker.IEngraveRenderFragment
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.engrave.EngraveFlowLayoutHelper
import com.angcyo.core.fragment.BasePagerFragment
import com.angcyo.dialog.normalDialog
import com.angcyo.fragment.AbsLifecycleFragment
import com.angcyo.laserpacker.device.engraveLoadingAsyncTimeout
import com.angcyo.library.ex._string
import com.angcyo.library.ex.gone
import com.angcyo.library.ex.syncSingle
import com.angcyo.library.ex.visible
import com.angcyo.library.toast
import com.hingin.umeng.UMEvent
import com.hingin.umeng.umengEventValue

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

        UMEvent.HISTORY.umengEventValue()
    }

    //警示提示动画
    private val dangerWarningHelper = DangerWarningHelper()
    private var rightIcoView: View? = null

    override fun initBaseView(savedInstanceState: Bundle?) {
        super.initBaseView(savedInstanceState)
        //
        dangerWarningHelper.bindDangerWarning(this)
        //
        appendRightItem(ico = R.drawable.canvas_delete_ico, action = {
            rightIcoView = this
            gone()
        }) {
            fContext().normalDialog {
                dialogTitle = _string(R.string.engrave_warn)
                dialogMessage = _string(R.string.canvas_delete_project_tip)
                positiveButton { dialog, dialogViewHolder ->
                    dialog.dismiss()
                    deleteAllHistory()
                }
            }
        }
    }

    private var _lastVisible = false

    /**是否显示右边删除按钮*/
    fun showRightDeleteIcoView(visible: Boolean = true) {
        _lastVisible = visible
        rightIcoView?.visible(visible)
    }

    override fun onTabLayoutIndexChange(
        fromIndex: Int,
        toIndex: Int,
        reselect: Boolean,
        fromUser: Boolean
    ) {
        if (toIndex == 0) {
            rightIcoView?.visible(false)
        } else {
            rightIcoView?.visible(_lastVisible)
        }
    }

    /**删除设备所有记录*/
    private fun deleteAllHistory() {
        engraveLoadingAsyncTimeout({
            syncSingle { countDownLatch ->
                FileModeCmd.deleteAllHistory().enqueue { bean, error ->
                    countDownLatch.countDown()

                    if (bean?.parse<FileTransferParser>()?.isFileDeleteSuccess() == true) {
                        toast(_string(R.string.delete_history_succeed))
                        showRightDeleteIcoView(false)
                        getPageFragment(
                            1,
                            EngraveDeviceHistoryFragment::class.java
                        ).startRefresh() //刷新设备历史
                    }
                    error?.let { toast(it.message) }
                }
            }
        })
    }

    //---

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