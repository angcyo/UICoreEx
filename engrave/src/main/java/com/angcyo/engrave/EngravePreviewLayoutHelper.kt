package com.angcyo.engrave

import android.view.MotionEvent
import com.angcyo.bluetooth.fsc.CommandQueueHelper
import com.angcyo.bluetooth.fsc.IReceiveBeanAction
import com.angcyo.bluetooth.fsc.enqueue
import com.angcyo.bluetooth.fsc.laserpacker.command.EngravePreviewCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.ExitCmd
import com.angcyo.bluetooth.fsc.laserpacker.parse.EngravePreviewParser
import com.angcyo.bluetooth.fsc.laserpacker.parse.QuerySettingParser
import com.angcyo.bluetooth.fsc.laserpacker.parse.QueryStateParser
import com.angcyo.bluetooth.fsc.parse
import com.angcyo.canvas.CanvasDelegate
import com.angcyo.engrave.ble.toZModeString
import com.angcyo.engrave.data.PreviewBoundsInfo
import com.angcyo.engrave.dslitem.engrave.EngraveOptionDiameterItem
import com.angcyo.fragment.AbsLifecycleFragment
import com.angcyo.library.annotation.CallPoint
import com.angcyo.library.ex.ClickAction
import com.angcyo.library.ex._string
import com.angcyo.library.ex.disableParentInterceptTouchEvent
import com.angcyo.library.ex.longFeedback
import com.angcyo.library.toast
import com.angcyo.widget.base.bindInRootView
import com.angcyo.widget.image.TouchCompatImageView
import com.angcyo.widget.progress.DslSeekBar
import com.angcyo.widget.span.span
import com.hingin.umeng.UMEvent
import com.hingin.umeng.umengEventValue

/**
 * 雕刻预览布局相关操作
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/01
 */
class EngravePreviewLayoutHelper(val fragment: AbsLifecycleFragment) : BaseEngraveLayoutHelper() {

    /**支架的最大移动步长*/
    val BRACKET_MAX_STEP: Int = 65535//130, 65535

    /**强制指定预览信息, 用于历史文档数据
     *
     * 预览的范围, 如果为null. 则从[canvasDelegate]中获取
     *
     * 如果需要实时更新预览,可以调用[com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel.sendUpdatePreviewRange]
     * */
    var previewBoundsInfo: PreviewBoundsInfo? = null

    /**下一步回调*/
    var onNextAction: ClickAction? = null

    /**物理尺寸逻辑*/
    var engraveOptionDiameterItem: EngraveOptionDiameterItem = EngraveOptionDiameterItem().apply {
        itemEngraveOptionInfo = engraveModel.engraveOptionInfoData.value!!
    }

    init {
        iViewLayoutId = R.layout.canvas_engrave_preview_layout
    }

    @CallPoint
    fun bindDeviceState() {
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

            if (it?.isModeEngravePreview() == false) {
                //非预览模式
                engraveModel.updateEngravePreviewUuid(null)
                engraveModel.engravePreviewInfoData.postValue(null)
            }

            if (it != null) {
                val mode = it.mode
                viewHolder?.enable(R.id.centre_button, true)
                if (mode == QueryStateParser.WORK_MODE_ENGRAVE_PREVIEW) {
                    //雕刻预览中
                    if (engraveModel.isRestore() && canvasDelegate?.getSelectedRenderer() == null) {
                        viewHolder?.tv(R.id.preview_button)?.text =
                            _string(R.string.print_v2_package_preview_over)
                    } else if (it.workState == 0x07) {
                        //显示中心模式
                        viewHolder?.enable(R.id.centre_button, false)
                        viewHolder?.tv(R.id.preview_button)?.text =
                            _string(R.string.preview_continue)
                    } else if (laserPeckerModel.haveExDevice()) {
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

    override fun onIViewCreate() {
        super.onIViewCreate()
        bindDeviceState()
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
        /*viewHolder?.v<TouchCompatImageView>(R.id.bracket_up_view)?.touchAction = { view, event ->
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
        //click
        viewHolder?.click(R.id.bracket_stop_view) {
            bracketStopCmd()
        }*/
        //物理尺寸
        val rOpen = laserPeckerModel.isROpen()
        viewHolder?.visible(R.id.diameter_wrap_layout, rOpen)
        if (rOpen) {
            engraveOptionDiameterItem.bindInRootView(viewHolder?.view(R.id.diameter_wrap_layout))
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
            if (engraveModel.isRestore() && canvasDelegate?.getSelectedRenderer() == null) {
                //结束预览
                hide()
            } else if (laserPeckerModel.isEngravePreviewShowCenterMode()) {
                //中心点预览模式下, 继续预览
                ExitCmd().enqueue()
                startPreviewCmd(canvasDelegate, true, false)
            } else if (laserPeckerModel.deviceStateData.value?.isModeIdle() == true) {
                //空闲模式中
                startPreviewCmd(canvasDelegate, true, false)
            } else if (laserPeckerModel.haveExDevice() /*&& laserPeckerModel.isEngravePreviewPause() 这个状态会有延迟*/) {
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
        val isRestoreState = canvasDelegate?.getSelectedRenderer() == null

        //next
        viewHolder?.visible(R.id.brightness_layout, !isRestoreState)
        viewHolder?.visible(R.id.centre_button, !isRestoreState)
        viewHolder?.visible(R.id.next_button, onNextAction != null && !isRestoreState)

        viewHolder?.click(R.id.next_button) {
            onNextAction?.invoke(it)
        }

        //cmd
        startPreviewCmd(canvasDelegate, true, false)

        //
        UMEvent.PREVIEW.umengEventValue()
    }

    override fun onIViewRemove() {
        super.onIViewRemove()
        loopCheckDeviceState = false
        if (laserPeckerModel.deviceStateData.value?.isModeEngravePreview() == true) {
            //关闭界面时, 如果在预览状态, 则退出预览
            ExitCmd().enqueue()
            queryDeviceStateCmd()
        }
    }

    //region ------command------

    /**查询设备状态*/
    fun queryDeviceStateCmd() {
        laserPeckerModel.queryDeviceState()
    }

    /**开始预览*/
    fun startPreviewCmd(
        canvasDelegate: CanvasDelegate?,
        updateState: Boolean,
        async: Boolean,
        zPause: Boolean = false
    ) {
        val selectedRenderer = canvasDelegate?.getSelectedRenderer()

        if (previewBoundsInfo == null) {
            //没有强制指定的预览信息

            if (selectedRenderer != null) {
                EngraveHelper.sendPreviewRange(selectedRenderer, updateState, async, zPause)
            } else {
                if (engraveModel.isRestore().not()) {
                    toast("No preview elements!")
                }
                if (updateState) {
                    queryDeviceStateCmd()
                }
            }
        } else {
            previewBoundsInfo?.apply {
                laserPeckerModel.sendUpdatePreviewRange(
                    originRectF,
                    originRectF,
                    originRotate,
                    EngraveHelper.lastPwrProgress,
                    updateState,
                    async,
                    zPause,
                    EngraveHelper.getDiameter()
                )
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