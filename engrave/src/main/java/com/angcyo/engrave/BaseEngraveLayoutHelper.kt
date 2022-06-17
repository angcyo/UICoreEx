package com.angcyo.engrave

import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.core.view.doOnPreDraw
import com.angcyo.canvas.CanvasDelegate
import com.angcyo.canvas.core.CanvasEntryPoint
import com.angcyo.library.ex.inflate
import com.angcyo.library.ex.mH
import com.angcyo.library.ex.removeFromParent
import com.angcyo.widget.DslViewHolder

/**
 * 雕刻相关布局助手
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/07
 */
abstract class BaseEngraveLayoutHelper {

    /**控件操作*/
    var viewHolder: DslViewHolder? = null

    /**需要填充的布局*/
    @LayoutRes
    var layoutId: Int = -1

    /**显示布局*/
    @CanvasEntryPoint
    open fun showLayout(viewGroup: ViewGroup, canvasDelegate: CanvasDelegate?) {
        if (viewHolder?.itemView?.parent == null) {
            val rootView = viewGroup.inflate(layoutId, true)
            viewHolder = DslViewHolder(rootView)

            //transition 动画
            rootView.doOnPreDraw {
                it.translationY = it.mH().toFloat()
                it.animate().translationY(0f).setDuration(300).start()
            }
        }
    }

    /**隐藏布局*/
    @CanvasEntryPoint
    open fun hideLayout() {
        viewHolder?.itemView?.let {
            it.animate().translationY(it.mH().toFloat()).withEndAction {
                it.removeFromParent()
            }.setDuration(300).start()
        }
    }

}