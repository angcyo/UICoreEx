package com.angcyo.engrave

import androidx.core.view.doOnPreDraw
import com.angcyo.canvas.CanvasDelegate
import com.angcyo.iview.IView
import com.angcyo.library.ex.mH

/**
 * 雕刻相关布局助手
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/07
 */
abstract class BaseEngraveLayoutHelper : IView() {

    var canvasDelegate: CanvasDelegate? = null

    init {
        iViewAddTransition = { rootView, end ->
            rootView.doOnPreDraw {
                it.translationY = it.mH().toFloat()
                it.animate().translationY(0f).setDuration(300).start()
            }
            rootView.requestLayout()
            end()
        }
        iViewRemoveTransition = { rootView, end ->
            rootView.animate().translationY(rootView.mH().toFloat()).withEndAction {
                end()
            }.setDuration(300).start()
        }
    }
}