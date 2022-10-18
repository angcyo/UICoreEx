package com.angcyo.engrave

import com.angcyo.bluetooth.fsc.laserpacker.command.*
import com.angcyo.engrave.dslitem.engrave.*
import com.angcyo.library.ex.*

/**
 * 雕刻布局相关操作
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/30
 */
class EngraveFlowLayoutHelper : BaseEngraveLayoutHelper() {

    init {
        iViewLayoutId = R.layout.canvas_engrave_flow_layout
    }

    /**检查开始雕刻
     * [index] 需要雕刻的数据索引
     * [option] 需要雕刻的数据选项*//*
    fun checkStartEngrave(index: Int, option: EngraveOptionInfo) {
        val zFlag = laserPeckerModel.deviceSettingData.value?.zFlag
        if (zFlag == 1) {
            //Z轴开关打开
            val zConnect = laserPeckerModel.deviceStateData.value?.zConnect
            if (zConnect != 1) {
                //未连接z轴, 弹窗提示
                viewHolder?.context?.messageDialog {
                    dialogMessageLeftIco = _drawable(R.mipmap.safe_tips)
                    dialogMessage = _string(R.string.zflag_discontent_tips)

                    if (isDebug()) {
                        negativeButtonText = _string(com.angcyo.dialog.R.string.dialog_negative)
                        positiveButtonListener = { dialog, dialogViewHolder ->
                            dialog.dismiss()
                            checkSafeTip(index, option)
                        }
                    }

                    onDismissListener = {
                        laserPeckerModel.queryDeviceState()
                    }
                }
                return
            }
        }
        val rFlag = laserPeckerModel.deviceSettingData.value?.rFlag
        if (rFlag == 1) {
            //旋转轴开关打开
            val rConnect = laserPeckerModel.deviceStateData.value?.rConnect
            if (rConnect != 1) {
                //未连接r轴, 弹窗提示
                viewHolder?.context?.messageDialog {
                    dialogMessageLeftIco = _drawable(R.mipmap.safe_tips)
                    dialogMessage = _string(R.string.zflag_discontent_tips)

                    if (isDebug()) {
                        negativeButtonText = _string(com.angcyo.dialog.R.string.dialog_negative)
                        positiveButtonListener = { dialog, dialogViewHolder ->
                            dialog.dismiss()
                            checkSafeTip(index, option)
                        }
                    }

                    onDismissListener = {
                        laserPeckerModel.queryDeviceState()
                    }
                }
                return
            }
        }
        val sFlag = laserPeckerModel.deviceSettingData.value?.sFlag
        if (sFlag == 1) {
            //滑台轴开关打开
            val sConnect = laserPeckerModel.deviceStateData.value?.sConnect
            if (sConnect != 1) {
                //未连接r轴, 弹窗提示
                viewHolder?.context?.messageDialog {
                    dialogMessageLeftIco = _drawable(R.mipmap.safe_tips)
                    dialogMessage = _string(R.string.zflag_discontent_tips)

                    if (isDebug()) {
                        negativeButtonText = _string(com.angcyo.dialog.R.string.dialog_negative)
                        positiveButtonListener = { dialog, dialogViewHolder ->
                            dialog.dismiss()
                            checkSafeTip(index, option)
                        }
                    }

                    onDismissListener = {
                        laserPeckerModel.queryDeviceState()
                    }
                }
                return
            }
        }
        checkSafeTip(index, option)
    }

    */
}