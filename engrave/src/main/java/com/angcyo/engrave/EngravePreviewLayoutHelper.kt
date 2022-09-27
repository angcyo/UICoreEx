//package com.angcyo.engrave
//
//import com.angcyo.bluetooth.fsc.enqueue
//import com.angcyo.bluetooth.fsc.laserpacker.command.ExitCmd
//import com.angcyo.bluetooth.fsc.laserpacker.queryDeviceState
//import com.angcyo.canvas.utils.CanvasConstant
//import com.angcyo.engrave.dslitem.engrave.EngraveOptionDiameterItem
//import com.angcyo.engrave.dslitem.preview.PreviewBracketItem
//import com.angcyo.engrave.dslitem.preview.PreviewBrightnessItem
//import com.angcyo.engrave.dslitem.preview.PreviewControlItem
//import com.angcyo.engrave.dslitem.preview.PreviewTipItem
//import com.angcyo.engrave.model.PreviewModel
//import com.angcyo.fragment.AbsLifecycleFragment
//import com.angcyo.item.DslBlackButtonItem
//import com.angcyo.item.DslLineItem
//import com.angcyo.library.annotation.CallPoint
//import com.angcyo.library.ex.ClickAction
//import com.angcyo.library.ex._string
//import com.angcyo.library.ex.dpi
//import com.hingin.umeng.UMEvent
//import com.hingin.umeng.umengEventValue
//
///**
// * 雕刻预览布局相关操作
// * @author <a href="mailto:angcyo@126.com">angcyo</a>
// * @since 2022/06/01
// */
//class EngravePreviewLayoutHelper(val fragment: AbsLifecycleFragment) : BaseEngraveLayoutHelper() {
//
//    /**下一步回调*/
//    var onNextAction: ClickAction? = null
//
//    /**物理尺寸逻辑*/
//    var engraveOptionDiameterItem: EngraveOptionDiameterItem = EngraveOptionDiameterItem().apply {
//        itemEngraveOptionInfo = engraveModel.engraveOptionInfoData.value!!
//    }
//
//    init {
//        iViewLayoutId = R.layout.canvas_engrave_preview_layout
//    }
//
//    override fun onIViewCreate() {
//        super.onIViewCreate()
//        bindDeviceState()
//    }
//
//    override fun onIViewShow() {
//        super.onIViewShow()
//        //init
//
//        //close按钮
//        showCloseView()
//
//        /*if (laserPeckerModel.haveExDevice()) {
//            //如果有外置设备, 则需要先设置外置设备信息, 才能开始预览
//            renderPreviewItems()
//        } else {
//            renderPreviewItems()
//        }*/
//
//        /*viewHolder?.v<DslSeekBar>(R.id.brightness_seek_bar)?.apply {
//            setProgress((HawkKeys.lastPwrProgress * 100).toInt())
//            config {
//                onSeekChanged = { value, fraction, fromUser ->
//                    HawkKeys.lastPwrProgress = fraction
//                    if (laserPeckerModel.isEngravePreviewMode()) {
//                        startPreviewCmd(canvasDelegate, false, true)
//                    } else if (laserPeckerModel.isEngravePreviewShowCenterMode()) {
//                        showPreviewCenterCmd(false)
//                    } else if (laserPeckerModel.isIdleMode()) {
//                        //空闲模式, 继续预览
//                        startPreviewCmd(canvasDelegate, true, true)
//                    } else {
//                        ExitCmd().enqueue()
//                        queryDeviceStateCmd()
//                    }
//                }
//            }
//        }*/
//        /*viewHolder?.v<TouchCompatImageView>(R.id.bracket_up_view)?.touchAction = { view, event ->
//            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
//                view.disableParentInterceptTouchEvent()
//                view.longFeedback()
//                //支架上升
//                bracketUpCmd()
//            } else if (event.actionMasked == MotionEvent.ACTION_UP ||
//                event.actionMasked == MotionEvent.ACTION_CANCEL
//            ) {
//                view.disableParentInterceptTouchEvent(false)
//                bracketStopCmd()
//            }
//        }
//        viewHolder?.v<TouchCompatImageView>(R.id.bracket_down_view)?.touchAction = { view, event ->
//            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
//                view.disableParentInterceptTouchEvent()
//                view.longFeedback()
//                //支架下降
//                bracketDownCmd()
//            } else if (event.actionMasked == MotionEvent.ACTION_UP ||
//                event.actionMasked == MotionEvent.ACTION_CANCEL
//            ) {
//                view.disableParentInterceptTouchEvent(false)
//                bracketStopCmd()
//            }
//        }
//        //click
//        viewHolder?.click(R.id.bracket_stop_view) {
//            bracketStopCmd()
//        }*/
//        //物理尺寸
//        val rOpen = laserPeckerModel.isROpen()
//        /*viewHolder?.visible(R.id.diameter_wrap_layout, rOpen)
//        if (rOpen) {
//            engraveOptionDiameterItem.bindInRootView(viewHolder?.view(R.id.diameter_wrap_layout))
//        }
//        viewHolder?.throttleClick(R.id.close_layout_view) {
//            hide()
//        }
//        viewHolder?.click(R.id.centre_button) {
//            if (laserPeckerModel.isEngravePreviewShowCenterMode()) {
//                showPreviewCenterCmd(true)
//            } else {
//                ExitCmd().enqueue()
//                showPreviewCenterCmd(true)
//            }
//        }
//        viewHolder?.click(R.id.preview_button) {
//            if (engraveModel.isRestore() && canvasDelegate?.getSelectedRenderer() == null) {
//                //结束预览
//                hide()
//            } else if (laserPeckerModel.isEngravePreviewShowCenterMode()) {
//                //中心点预览模式下, 继续预览
//                ExitCmd().enqueue()
//                startPreviewCmd(canvasDelegate, true, false)
//            } else if (laserPeckerModel.deviceStateData.value?.isModeIdle() == true) {
//                //空闲模式中
//                startPreviewCmd(canvasDelegate, true, false)
//            } else if (laserPeckerModel.haveExDevice() *//*&& laserPeckerModel.isEngravePreviewPause() 这个状态会有延迟*//*) {
//                if (laserPeckerModel.isEngravePreviewZ()) {
//                    //第三轴滚动中, 则需要暂停滚动
//                    loopCheckDeviceState = false
//                    startPreviewCmd(canvasDelegate, true, false, true)
//                } else {
//                    //Z轴暂停滚动中, 则滚动预览
//                    zContinuePreviewCmd()
//                    loopCheckDeviceState = true
//                    checkDeviceState()
//                }
//            } else {
//                //结束预览
//                hide()
//            }
//        }*/
//        //val isRestoreState = canvasDelegate?.getSelectedRenderer() == null
//
//        //next
//        /*viewHolder?.visible(R.id.brightness_layout, !isRestoreState)
//        viewHolder?.visible(R.id.centre_button, !isRestoreState)
//        viewHolder?.visible(R.id.next_button, onNextAction != null && !isRestoreState)
//
//        viewHolder?.click(R.id.next_button) {
//            onNextAction?.invoke(it)
//        }*/
//
//        //cmd
//        //startPreviewCmd(canvasDelegate, true, false)
//
//
//        //
//        UMEvent.PREVIEW.umengEventValue()
//    }
//
//
//
//
//    //
//
//
//}