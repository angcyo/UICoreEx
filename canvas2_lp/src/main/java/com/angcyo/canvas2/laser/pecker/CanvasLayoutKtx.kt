package com.angcyo.canvas2.laser.pecker

import android.view.ViewGroup
import com.angcyo.canvas.CanvasRenderView
import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.dsladapter.DslAdapter
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter._dslAdapter
import com.angcyo.dsladapter.drawRight
import com.angcyo.library.ex._color
import com.angcyo.library.ex._dimen
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.recycler.DslRecyclerView

/** 扩展方法
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/03
 */

//---

/**画板视图*/
val DslViewHolder.canvasView: CanvasRenderView?
    get() = v(R.id.canvas_view)

/**画板核心操作代理类*/
val DslViewHolder.canvasDelegate: CanvasRenderDelegate?
    get() = canvasView?.delegate

//---

/**画板的操作item容器*/
val DslViewHolder.canvasItemRv: DslRecyclerView?
    get() = v(R.id.canvas_item_view)

/**[canvasItemRv]*/
val DslViewHolder.canvasItemAdapter: DslAdapter?
    get() = canvasItemRv?._dslAdapter

//---

/**具体元素的编辑控制布局, 包含[canvasControlRv]*/
val DslViewHolder.canvasControlLayout: ViewGroup?
    get() = v(R.id.canvas_control_layout)

/**具体的属性控制item容器*/
val DslViewHolder.canvasControlRv: DslRecyclerView?
    get() = v(R.id.canvas_control_view)

/**[canvasControlRv]*/
val DslViewHolder.canvasControlAdapter: DslAdapter?
    get() = canvasControlRv?._dslAdapter

//---

fun DslAdapterItem.drawCanvasRight(
    insertRight: Int = _dimen(R.dimen.lib_line),
    offsetTop: Int = _dimen(R.dimen.lib_drawable_padding),
    offsetBottom: Int = _dimen(R.dimen.lib_drawable_padding),
    color: Int = _color(R.color.canvas_dark_gray)
) {
    drawRight(insertRight, offsetTop, offsetBottom, color)
}

//---


