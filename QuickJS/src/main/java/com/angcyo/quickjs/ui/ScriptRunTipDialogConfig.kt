package com.angcyo.quickjs.ui

import android.app.Dialog
import android.content.Context
import android.view.Window
import com.angcyo.base.enableLayoutFullScreen
import com.angcyo.base.translucentStatusBar
import com.angcyo.dialog.DslDialogConfig
import com.angcyo.library.annotation.DSL
import com.angcyo.quickjs.R
import com.angcyo.widget.DslViewHolder
import java.lang.ref.WeakReference

/**
 * 脚本运行提示对话框配置 0 1 角度变换动画
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/06/13
 */
class ScriptRunTipDialogConfig(context: Context? = null) : DslDialogConfig(context) {

    companion object {

        private var weakDialog: WeakReference<Dialog?>? = null

        /**隐藏对话框*/
        fun hideScriptRunTipDialog() {
            weakDialog?.get()?.dismiss()
            weakDialog?.clear()
            weakDialog = null
        }
    }

    init {
        dialogLayoutId = R.layout.dialog_script_run_tip_layout
        setFullScreen()
    }

    override fun configWindow(window: Window) {
        super.configWindow(window)
        //开启布局全屏, 体验更佳
        window.enableLayoutFullScreen(true)
        window.translucentStatusBar(true)
        //window.translucentNavigationBar(true)

        weakDialog = WeakReference(_dialog)
    }

    override fun onDialogDestroy(dialog: Dialog, dialogViewHolder: DslViewHolder) {
        super.onDialogDestroy(dialog, dialogViewHolder)
        weakDialog?.clear()
        weakDialog = null
    }
}

@DSL
fun Context.scriptRunTipDialog(config: ScriptRunTipDialogConfig.() -> Unit = {}): Dialog {
    return ScriptRunTipDialogConfig().run {
        dialogContext = this@scriptRunTipDialog
        config()
        show()
    }
}
