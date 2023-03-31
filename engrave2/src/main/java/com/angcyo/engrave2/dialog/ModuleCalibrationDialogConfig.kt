package com.angcyo.engrave2.dialog

import android.app.Dialog
import android.content.Context
import com.angcyo.bluetooth.fsc.enqueue
import com.angcyo.bluetooth.fsc.laserpacker.command.EngravePreviewCmd
import com.angcyo.dialog.DslDialogConfig
import com.angcyo.dialog.configBottomDialog
import com.angcyo.engrave2.BuildConfig
import com.angcyo.engrave2.R
import com.angcyo.library.annotation.DSL
import com.angcyo.widget.DslViewHolder

/**
 * 校准对话框
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/11/01
 */
class ModuleCalibrationDialogConfig : DslDialogConfig() {

    /**校准是否完成的回调*/
    var onModuleCalibrationAction: (Boolean) -> Unit = {}

    init {
        dialogLayoutId = R.layout.dialog_module_calibration_layout
        cancelable = true //开始校准之后, 才不允许关闭
    }

    override fun initDialogView(dialog: Dialog, dialogViewHolder: DslViewHolder) {
        super.initDialogView(dialog, dialogViewHolder)

        if (BuildConfig.DEBUG) {
            dialogViewHolder.click(R.id.lib_image_view) {
                startCalibrationCmd(dialogViewHolder)
            }
        }

        dialogViewHolder.click(R.id.start_button) {
            startCalibrationCmd(dialogViewHolder)
        }

        dialogViewHolder.click(R.id.finish_button) {
            EngravePreviewCmd.finishCalibrationCmd().enqueue { bean, error ->
                if (error == null) {
                    dialog.dismiss()
                    onModuleCalibrationAction(true)
                }
            }
        }
    }

    /**开始对笔指令*/
    fun startCalibrationCmd(dialogViewHolder: DslViewHolder) {
        EngravePreviewCmd.startCalibrationCmd().enqueue { bean, error ->
            if (error == null) {
                dialogViewHolder.invisible(R.id.start_button)
                dialogViewHolder.visible(R.id.finish_button)
                cancelable = false
            }
        }
    }

    override fun onDialogCancel(dialog: Dialog, dialogViewHolder: DslViewHolder) {
        super.onDialogCancel(dialog, dialogViewHolder)
        onModuleCalibrationAction(false)
    }
}

@DSL
fun Context.moduleCalibrationDialog(config: ModuleCalibrationDialogConfig.() -> Unit = {}): Dialog {
    return ModuleCalibrationDialogConfig().run {
        dialogContext = this@moduleCalibrationDialog
        configBottomDialog()
        config()
        show()
    }
}