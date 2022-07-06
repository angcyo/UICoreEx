package com.angcyo.engrave

import android.graphics.RectF
import android.view.MotionEvent
import androidx.fragment.app.Fragment
import com.angcyo.bluetooth.fsc.CommandQueueHelper
import com.angcyo.bluetooth.fsc.IReceiveBeanAction
import com.angcyo.bluetooth.fsc.enqueue
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.command.EngravePreviewCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.ExitCmd
import com.angcyo.bluetooth.fsc.laserpacker.parse.QueryStateParser
import com.angcyo.canvas.CanvasDelegate
import com.angcyo.core.vmApp
import com.angcyo.library.ex._string
import com.angcyo.library.ex.disableParentInterceptTouchEvent
import com.angcyo.library.ex.elseNull
import com.angcyo.library.ex.longFeedback
import com.angcyo.widget.image.TouchCompatImageView
import com.angcyo.widget.progress.DslSeekBar

/**
 * 雕刻预览布局相关操作
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/01
 */
class EngravePreviewLayoutHelper(val fragment: Fragment) : BaseEngraveLayoutHelper() {

    /**支架的最大移动步长*/
    val BRACKET_MAX_STEP: Int = 65535//130, 65535

    /**预览的范围, 如果为null. 则从[canvasDelegate]中获取
     *
     * 如果需要实时更新预览,可以调用[com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel.sendUpdatePreviewRange]
     * */
    var previewBounds: RectF? = null

    val laserPeckerModel = vmApp<LaserPeckerModel>()

    init {
        iViewLayoutId = R.layout.canvas_engrave_preview_layout
        //模式改变监听
        laserPeckerModel.deviceStateData.observe(fragment) {
            if (it != null) {
                val mode = it.mode
                viewHolder?.enable(R.id.centre_button, true)
                if (mode == QueryStateParser.WORK_MODE_ENGRAVE_PREVIEW) {
                    if (it.workState == 7) {
                        //显示中心模式
                        viewHolder?.enable(R.id.centre_button, false)
                        viewHolder?.tv(R.id.preview_button)?.text =
                            _string(R.string.preview_continue)
                    } else {
                        viewHolder?.tv(R.id.preview_button)?.text =
                            _string(R.string.print_v2_package_preview_over)
                    }
                } else if (mode == QueryStateParser.WORK_MODE_IDLE) {
                    viewHolder?.tv(R.id.preview_button)?.text = _string(R.string.preview_continue)
                }
            }
        }
    }

    override fun onIViewShow() {
        super.onIViewShow()
        //init
        viewHolder?.v<DslSeekBar>(R.id.brightness_seek_bar)?.apply {
            setProgress((LaserPeckerHelper.lastPwrProgress * 100).toInt())
            config {
                onSeekChanged = { value, fraction, fromUser ->
                    LaserPeckerHelper.lastPwrProgress = fraction
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
                bracketUpCmd { bean, error ->
                    //支架上升
                }
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
                bracketDownCmd { bean, error ->
                    //支架下降
                }
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
                //继续预览
                ExitCmd().enqueue()
                startPreviewCmd(canvasDelegate, true, false)
            } else if (laserPeckerModel.deviceStateData.value?.isModeIdle() == true) {
                //空闲模式中
                startPreviewCmd(canvasDelegate, true, false)
            } else {
                //结束预览
                hide()
            }
            /*if (laserPeckerModel.isEngravePreviewMode()) {
                exitCmd { bean, error ->
                    queryDeviceStateCmd()
                }
            } else if (laserPeckerModel.isIdleMode()) {
                startPreviewCmd(canvasDelegate)
            } else {
                exitCmd { bean, error ->
                    queryDeviceStateCmd()
                }
            }*/
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
     * [updateState] 是否要更新状态*/
    fun startPreviewCmd(canvasDelegate: CanvasDelegate?, updateState: Boolean, async: Boolean) {
        val bounds = previewBounds ?: canvasDelegate?.getSelectedRenderer()?.getRotateBounds()

        bounds?.let {
            val cmd = EngravePreviewCmd.previewRange(
                bounds.left.toInt(),
                bounds.top.toInt(),
                bounds.width().toInt(),
                bounds.height().toInt()
            )
            val flag =
                if (async) CommandQueueHelper.FLAG_ASYNC else CommandQueueHelper.FLAG_NORMAL
            cmd.enqueue(flag)
            queryDeviceStateCmd()
        }.elseNull {
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

    /**支架上升*/
    fun bracketUpCmd(action: IReceiveBeanAction) {
        val cmd = EngravePreviewCmd.previewBracketUp(BRACKET_MAX_STEP)
        cmd.enqueue { bean, error ->
            action(bean, error)
        }
    }

    /**支架下降*/
    fun bracketDownCmd(action: IReceiveBeanAction) {
        val cmd = EngravePreviewCmd.previewBracketDown(BRACKET_MAX_STEP)
        cmd.enqueue { bean, error ->
            action(bean, error)
        }
    }

    /**停止支架*/
    fun bracketStopCmd() {
        val cmd = EngravePreviewCmd.previewBracketStop()
        cmd.enqueue { bean, error ->

        }
    }

    /**显示中心*/
    fun showPreviewCenterCmd(updateState: Boolean) {
        val cmd = EngravePreviewCmd.previewShowCenter()
        cmd.enqueue(CommandQueueHelper.FLAG_ASYNC)
        if (updateState) {
            queryDeviceStateCmd()
        }
    }

    //endregion
}