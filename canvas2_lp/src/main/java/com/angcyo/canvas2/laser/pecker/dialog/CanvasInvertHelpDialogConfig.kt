package com.angcyo.canvas2.laser.pecker.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.dialog.BaseDialogConfig
import com.angcyo.dialog.TargetWindow
import com.angcyo.library.annotation.DSL
import com.angcyo.widget.DslViewHolder

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023-3-9
 */
class CanvasInvertHelpDialogConfig : BaseDialogConfig() {

    init {
        dialogLayoutId = R.layout.dialog_invert_help_layout
        dialogWidth = -1
        dialogMessageGravity = Gravity.CENTER
        negativeButtonText = null //cancel
        neutralButtonText = null
        dialogBgDrawable = ColorDrawable(Color.TRANSPARENT)
    }

    override fun initDialogView(dialog: Dialog, dialogViewHolder: DslViewHolder) {
        super.initDialogView(dialog, dialogViewHolder)
    }

}

/**Dsl
 * 画布图片编辑属性弹窗*/
@DSL
fun Context.invertHelpDialog(config: CanvasInvertHelpDialogConfig.() -> Unit = {}): TargetWindow {
    val dialogConfig = CanvasInvertHelpDialogConfig()
    dialogConfig.dialogContext = this
    dialogConfig.config()
    return dialogConfig.show()
}