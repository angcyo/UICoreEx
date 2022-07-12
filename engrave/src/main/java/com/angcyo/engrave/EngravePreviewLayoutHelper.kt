package com.angcyo.engrave

import android.graphics.RectF
import android.view.MotionEvent
import com.angcyo.bluetooth.fsc.CommandQueueHelper
import com.angcyo.bluetooth.fsc.IReceiveBeanAction
import com.angcyo.bluetooth.fsc.enqueue
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.command.EngravePreviewCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.ExitCmd
import com.angcyo.bluetooth.fsc.laserpacker.parse.EngravePreviewParser
import com.angcyo.bluetooth.fsc.laserpacker.parse.QuerySettingParser
import com.angcyo.bluetooth.fsc.laserpacker.parse.QueryStateParser
import com.angcyo.bluetooth.fsc.parse
import com.angcyo.canvas.CanvasDelegate
import com.angcyo.core.vmApp
import com.angcyo.engrave.ble.toZModeString
import com.angcyo.fragment.AbsLifecycleFragment
import com.angcyo.library.ex.*
import com.angcyo.library.toast
import com.angcyo.widget.image.TouchCompatImageView
import com.angcyo.widget.progress.DslSeekBar
import com.angcyo.widget.span.span

/**
 * 雕刻预览布局相关操作
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/01
 */
class EngravePreviewLayoutHelper(val fragment: AbsLifecycleFragment) : BaseEngraveLayoutHelper() {

    /**支架的最大移动步长*/
    val BRACKET_MAX_STEP: Int = 65535//130, 65535

    /**预览的范围, 如果为null. 则从[canvasDelegate]中获取
     *
     * 如果需要实时更新预览,可以调用[com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel.sendUpdatePreviewRange]
     * */
    var previewBounds: RectF? = null

    /**下一步回调*/
    var onNextAction: ClickAction? = null

    init {
        iViewLayoutId = R.layout.canvas_engrave_preview_layout
        //模式改变监听, 改变按钮的文本
        laserPeckerModel.deviceStateData.observe(this) {

            //雕刻模式提示
            viewHolder?.visible(R.id.preview_text_view, laserPeckerModel.isZOpen())
            if (laserPeckerModel.isZOpen()) {
                viewHolder?.tv(R.id.preview_text_view)?.text = span {
                    append(_string(R.string.device_setting_tips_fourteen_11))
                    append(QuerySettingParser.Z_MODEL.toZModeString())
                }
            }

            if (it != null) {
                val mode = it.mode
                viewHolder?.enable(R.id.centre_button, true)
                if (mode == QueryStateParser.WORK_MODE_ENGRAVE_PREVIEW) {
                    if (it.workState == 0x07) {
                        //显示中心模式
                        viewHolder?.enable(R.id.centre_button, false)
                        viewHolder?.tv(R.id.preview_button)?.text =
                            _string(R.string.preview_continue)
                    } else if (laserPeckerModel.isZOpen()) {
                        if (it.workState == 0x05) {
                            //Z轴滚动预览中
                            viewHolder?.tv(R.id.preview_button)?.text =
                                _string(R.string.preview_scroll_pause)
                        } else {
                            viewHolder?.tv(R.id.preview_button)?.text =
                                _string(R.string.preview_scroll_continue)
                        }
                    } else {
                        viewHolder?.tv(R.id.preview_button)?.text =
                            _string(R.string.print_v2_package_preview_over)
                    }
                } else if (mode == QueryStateParser.WORK_MODE_IDLE) {
                    loopCheckDeviceState = false
                    viewHolder?.tv(R.id.preview_button)?.text = _string(R.string.preview_continue)
                }
            } else {
                loopCheckDeviceState = false
            }
        }
    }

    override fun onIViewShow() {
        super.onIViewShow()
        //init
        viewHolder?.v<DslSeekBar>(R.id.brightness_seek_bar)?.apply {
            setProgress((EngraveHelper.lastPwrProgress * 100).toInt())
            config {
                onSeekChanged = { value, fraction, fromUser ->
                    EngraveHelper.lastPwrProgress = fraction
                    if (laserPeckerModel.isEngravePreviewMode()) {
                        startPreviewCmd(canvasDelegate, false, true)
                    } else if (laserPeckerModel.isEngravePreviewShowCenterMode()) {
                        showPreviewCenterCmd(false)
                    } else if (laserPeckerModel.isIdleMode()) {
                        //空闲模式, 继续预览
                        startPreviewCmd(canvasDelegate, true, true)
                    } else {
                        ExitCmd().enqueue()
                        queryDeviceStateCmd()
                    }
                }
            }
        }
        viewHolder?.v<TouchCompatImageView>(R.id.bracket_up_view)?.touchAction = { view, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                view.disableParentInterceptTouchEvent()
                view.longFeedback()
                //支架上升
                bracketUpCmd()
            } else if (event.actionMasked == MotionEvent.ACTION_UP ||
                event.actionMasked == MotionEvent.ACTION_CANCEL
            ) {
                view.disableParentInterceptTouchEvent(false)
                bracketStopCmd()
            }
        }
        viewHolder?.v<TouchCompatImageView>(R.id.bracket_down_view)?.touchAction = { view, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                view.disableParentInterceptTouchEvent()
                view.longFeedback()
                //支架下降
                bracketDownCmd()
            } else if (event.actionMasked == MotionEvent.ACTION_UP ||
                event.actionMasked == MotionEvent.ACTION_CANCEL
            ) {
                view.disableParentInterceptTouchEvent(false)
                bracketStopCmd()
            }
        }
        viewHolder?.click(R.id.bracket_stop_view) {
            bracketStopCmd()
        }
        viewHolder?.throttleClick(R.id.close_layout_view) {
            hide()
        }
        viewHolder?.click(R.id.centre_button) {
            if (laserPeckerModel.isEngravePreviewShowCenterMode()) {
                showPreviewCenterCmd(true)
            } else {
                ExitCmd().enqueue()
                showPreviewCenterCmd(true)
            }
        }
        viewHolder?.click(R.id.preview_button) {
            if (laserPeckerModel.isEngravePreviewShowCenterMode()) {
                //中心点预览模式下, 继续预览
                ExitCmd().enqueue()
                startPreviewCmd(canvasDelegate, true, false)
            } else if (laserPeckerModel.deviceStateData.value?.isModeIdle() == true) {
                //空闲模式中
                startPreviewCmd(canvasDelegate, true, false)
            } else if (laserPeckerModel.isZOpen() /*&& laserPeckerModel.isEngravePreviewPause() 这个状态会有延迟*/) {
                if (laserPeckerModel.isEngravePreviewZ()) {
                    //第三轴滚动中, 则需要暂停滚动
                    loopCheckDeviceState = false
                    startPreviewCmd(canvasDelegate, true, false, true)
                } else {
                    //Z轴暂停滚动中, 则滚动预览
                    zContinuePreviewCmd()
                    loopCheckDeviceState = true
                    checkDeviceState()
                }
            } else {
                //结束预览
                hide()
            }
        }

        //next
        viewHolder?.visible(
            R.id.next_button,
            onNextAction != null && canvasDelegate?.getSelectedRenderer() != null
        )
        viewHolder?.click(R.id.next_button) {
            onNextAction?.invoke(it)
        }

        //cmd
        startPreviewCmd(canvasDelegate, true, false)
    }

    override fun onIViewRemove() {
        super.onIViewRemove()
        ExitCmd().enqueue()
        queryDeviceStateCmd()
    }

    //region ------command------

    /**查询设备状态*/
    fun queryDeviceStateCmd() {
        vmApp<LaserPeckerModel>().queryDeviceState()
    }

    /**开始预览
     * [updateState] 是否要更新状态
     * [zPause] 是否是第三轴暂停预览
     * */
    fun startPreviewCmd(
        canvasDelegate: CanvasDelegate?,
        updateState: Boolean,
        async: Boolean,
        zPause: Boolean = false
    ) {
        val bounds = previewBounds ?: canvasDelegate?.getSelectedRenderer()?.getRotateBounds()
        bounds?.let {
            val cmd = if (zPause) {
                EngravePreviewCmd.previewZRange(
                    bounds.left.toInt(),
                    bounds.top.toInt(),
                    bounds.width().toInt(),
                    bounds.height().toInt(),
                    EngraveHelper.lastPwrProgress
                )
            } else {
                EngravePreviewCmd.previewRange(
                    bounds.left.toInt(),
                    bounds.top.toInt(),
                    bounds.width().toInt(),
                    bounds.height().toInt(),
                    EngraveHelper.lastPwrProgress
                )
            }
            val flag =
                if (async) CommandQueueHelper.FLAG_ASYNC else CommandQueueHelper.FLAG_NORMAL
            cmd.enqueue(flag)
            queryDeviceStateCmd()
        }.elseNull {
            toast("No preview elements!")
            if (updateState) {
                queryDeviceStateCmd()
            }
        }
    }

    /**停止预览*/
    fun stopPreviewCmd() {
        val cmd = EngravePreviewCmd.previewStop()
        cmd.enqueue()
        queryDeviceStateCmd()
    }

    /**z轴滚动预览*/
    fun zContinuePreviewCmd() {
        val cmd = EngravePreviewCmd.previewZContinue()
        cmd.enqueue()
        queryDeviceStateCmd()
    }

    /**支架上升*/
    fun bracketUpCmd(action: IReceiveBeanAction? = null) {
        val cmd = EngravePreviewCmd.previewBracketUp(BRACKET_MAX_STEP)
        cmd.enqueue { bean, error ->
            if (bean?.parse<EngravePreviewParser>()?.isBracketConnect() != true) {
                toast("支架未连接")
            }
            action?.invoke(bean, error)
        }
    }

    /**支架下降*/
    fun bracketDownCmd(action: IReceiveBeanAction? = null) {
        val cmd = EngravePreviewCmd.previewBracketDown(BRACKET_MAX_STEP)
        cmd.enqueue { bean, error ->
            if (bean?.parse<EngravePreviewParser>()?.isBracketConnect() != true) {
                toast("支架未连接")
            }
            action?.invoke(bean, error)
        }
    }

    /**停止支架*/
    fun bracketStopCmd(action: IReceiveBeanAction? = null) {
        val cmd = EngravePreviewCmd.previewBracketStop()
        cmd.enqueue { bean, error ->
            if (bean?.parse<EngravePreviewParser>()?.isBracketConnect() != true) {
                toast("支架未连接")
            }
            action?.invoke(bean, error)
        }
    }

    /**显示中心*/
    fun showPreviewCenterCmd(updateState: Boolean) {
        val cmd = EngravePreviewCmd.previewShowCenter(EngraveHelper.lastPwrProgress)
        cmd.enqueue(CommandQueueHelper.FLAG_ASYNC)
        if (updateState) {
            queryDeviceStateCmd()
        }
    }

    //endregion
}