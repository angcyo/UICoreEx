package com.angcyo.engrave

import android.content.Context
import com.angcyo.bluetooth.fsc.enqueue
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.command.ExitCmd
import com.angcyo.bluetooth.fsc.laserpacker.queryDeviceState
import com.angcyo.core.vmApp
import com.angcyo.dialog.messageDialog
import com.angcyo.engrave.BaseFlowLayoutHelper.Companion.ENGRAVE_FLOW_TRANSFER_BEFORE_CONFIG
import com.angcyo.engrave.model.EngraveModel
import com.angcyo.engrave.model.PreviewModel
import com.angcyo.iview.BaseRecyclerIView
import com.angcyo.library.annotation.CallPoint
import com.angcyo.library.component._delay
import com.angcyo.library.ex._drawable
import com.angcyo.library.ex._string

/**
 * 雕刻流程相关布局基类
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/07
 */
abstract class BaseFlowLayoutHelper : BaseRecyclerIView() {

    companion object {
        /**雕刻流程: 预览前的配置*/
        const val ENGRAVE_FLOW_PREVIEW_BEFORE_CONFIG = 0x01

        /**雕刻流程: 预览中*/
        const val ENGRAVE_FLOW_PREVIEW = 0x02

        /**数据传输之前的配置*/
        const val ENGRAVE_FLOW_TRANSFER_BEFORE_CONFIG = 0x10

        /**雕刻流程: 雕刻数据传输中...*/
        const val ENGRAVE_FLOW_TRANSMITTING = 0x20

        /**雕刻流程: 雕刻前的配置*/
        const val ENGRAVE_FLOW_BEFORE_CONFIG = 0x40

        /**雕刻流程: 雕刻中...*/
        const val ENGRAVE_FLOW_ENGRAVING = 0x80

        /**雕刻流程: 雕刻完成.*/
        const val ENGRAVE_FLOW_FINISH = 0x100
    }

    /**当前处于那个雕刻流程*/
    var engraveFlow: Int = 0
        set(value) {
            val old = field
            field = value
            onEngraveFlowChanged(old, value)
        }

    /**当前[engraveFlow]能够回退到的模式*/
    var engraveBackFlow: Int = 0

    /**雕刻绑定的界面*/
    var engraveCanvasFragment: IEngraveCanvasFragment? = null

    //产品模式
    val laserPeckerModel = vmApp<LaserPeckerModel>()

    //雕刻模式
    val engraveModel = vmApp<EngraveModel>()

    //预览模式
    val previewModel = vmApp<PreviewModel>()

    /**是否循环检测设备状态*/
    var loopCheckDeviceState: Boolean = false

    //

    override fun onIViewCreate() {
        super.onIViewCreate()
        bindDeviceState()
    }

    override fun onIViewShow() {
        super.onIViewShow()
        renderFlowItems()
    }

    override fun onIViewRemove() {
        super.onIViewRemove()
        loopCheckDeviceState = false
        if (engraveFlow == ENGRAVE_FLOW_PREVIEW) {
            //在预览界面
            if (laserPeckerModel.deviceStateData.value?.isModeEngravePreview() == true) {
                //关闭界面时, 如果在预览状态, 则退出预览
                ExitCmd().enqueue()
                queryDeviceState()
            }
        }
    }

    override fun hide(end: (() -> Unit)?) {
        if (cancelable && engraveBackFlow > 0 && engraveBackFlow != engraveFlow) {
            //需要回退
            engraveFlow = engraveBackFlow
            renderFlowItems()
        } else {
            super.hide(end)
        }
    }

    /**雕刻模式改变通知*/
    var onEngraveFlowChangedAction: (from: Int, to: Int) -> Unit = { _, _ ->

    }

    /**雕刻模式改变通知*/
    open fun onEngraveFlowChanged(from: Int, to: Int) {
        onEngraveFlowChangedAction(from, to)
    }

    //

    @CallPoint
    open fun bindDeviceState() {
        //模式改变监听, 改变按钮的文本
        laserPeckerModel.deviceStateData.observe(this) {
            _dslAdapter?.updateAllItem()

/*
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
            }*/
        }
    }

    /**根据不同的流程, 渲染不同的界面*/
    open fun renderFlowItems() {

    }

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

    //
}

/**是否进入了雕刻流程*/
fun Int.isEngraveFlow() = this >= ENGRAVE_FLOW_TRANSFER_BEFORE_CONFIG