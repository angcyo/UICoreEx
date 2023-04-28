package com.angcyo.canvas2.laser.pecker.dialog

import android.content.Context
import android.view.View
import com.angcyo.dialog.TargetWindow
import com.angcyo.dialog.popup.MenuPopupConfig
import com.angcyo.library._screenHeight
import com.angcyo.library._screenWidth
import com.angcyo.library.annotation.DSL
import com.angcyo.library.component.pad.isInPadMode
import kotlin.math.max

/**
 * Canvas相关的菜单弹窗
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/04/28
 */
class CanvasMenuPopupConfig : MenuPopupConfig() {

    init {
        if (isInPadMode()) {
            width = max(_screenWidth, _screenHeight) / 2
        }
    }

}

/**Dsl
 * [Context.menuPopupWindow]*/
@DSL
fun Context.canvasMenuPopupWindow(
    anchor: View?,
    config: CanvasMenuPopupConfig.() -> Unit
): TargetWindow {
    val popupConfig = CanvasMenuPopupConfig()
    popupConfig.anchor = anchor

    /*popupConfig.renderAdapterAction = {
        //设置内容
    }*/

    popupConfig.config()
    return popupConfig.show(this)
}