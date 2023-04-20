package com.angcyo.canvas2.laser.pecker

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.ViewGroup
import com.angcyo.canvas.CanvasRenderView
import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas2.laser.pecker.util.lpElementBean
import com.angcyo.dsladapter.DslAdapter
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter._dslAdapter
import com.angcyo.dsladapter.drawRight
import com.angcyo.laserpacker.bean.LPElementBean
import com.angcyo.library.app
import com.angcyo.library.ex.*
import com.angcyo.svg.Svg
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.recycler.DslRecyclerView
import com.pixplicity.sharp.SharpDrawable

/** 扩展方法
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/03
 */

//---

/**画板视图*/
val DslViewHolder.canvasView: CanvasRenderView?
    get() = v(R.id.canvas_view)

/**画板核心操作代理类*/
val DslViewHolder.renderDelegate: CanvasRenderDelegate?
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

/**图层列表rv*/
val DslViewHolder.canvasLayerRv: DslRecyclerView?
    get() = v(R.id.canvas_layer_view)

/**[canvasLayerRv]*/
val DslViewHolder.canvasLayerAdapter: DslAdapter?
    get() = canvasLayerRv?._dslAdapter

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

/**只读取[SVG]中的[Path]数据
 * [com.angcyo.svg.StylePath]
 * [com.pixplicity.sharp.SharpDrawable.pathList]*/
fun loadAssetsSvgPath(
    assetsName: String,
    color: Int = Color.BLACK, // Color.BLACK 黑色边
    drawStyle: Paint.Style? = null, //Paint.Style.STROKE //描边
    viewWidth: Int = 0,
    viewHeight: Int = 0,
): Pair<String?, SharpDrawable?>? {
    val svg = app().readAssets(assetsName)
    return try {
        svg to Svg.loadSvgPathDrawable(svg!!, color, drawStyle, null, viewWidth, viewHeight)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**从图片中, 获取雕刻需要用到的像素信息*/
fun Bitmap.engraveColorBytes(channelType: Int = Color.RED): ByteArray {
    return colorChannel(channelType) { color, channelValue ->
        if (color == Color.TRANSPARENT) {
            0xFF //255 白色像素, 白色在纸上不雕刻, 在金属上雕刻
        } else {
            channelValue
        }
    }
}

/**获取所有元素Bean结构*/
fun CanvasRenderDelegate?.getAllElementBean(): List<LPElementBean> {
    return this?.getAllSingleElementRendererList().getAllElementBean()
}

fun List<BaseRenderer>?.getAllElementBean(): List<LPElementBean> {
    val result = mutableListOf<LPElementBean>()
    this?.forEach {
        it.lpElementBean()?.let {
            result.add(it)
        }
    }
    return result
}

fun List<LPElementBean>.updateGroupInfo(groupId: String?, groupName: String?) {
    forEach {
        it.groupName = groupName
        it.groupId = groupId
    }
}

fun List<LPElementBean>.updateGroupName(name: String?) {
    forEach {
        it.groupName = name
    }
}

fun List<LPElementBean>.updateGroupId(groupId: String?) {
    forEach {
        it.groupId = groupId
    }
}