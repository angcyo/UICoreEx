package com.angcyo.canvas2.laser.pecker.dslitem

import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import com.angcyo.bluetooth.fsc.laserpacker.DeviceStateModel
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.isOverflowProductBounds
import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas.render.data.RenderParams
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas.render.renderer.CanvasElementRenderer
import com.angcyo.canvas.render.renderer.CanvasGroupRenderer
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.util.lpElementBean
import com.angcyo.core.component.model.NightModel
import com.angcyo.core.vmApp
import com.angcyo.dialog.messageDialog
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave2.transition.overflowBoundsMessage
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.laserpacker.bean.LPElementBean
import com.angcyo.laserpacker.device.DeviceHelper
import com.angcyo.laserpacker.device.LayerHelper
import com.angcyo.laserpacker.toTypeNameString
import com.angcyo.library.ex.Action
import com.angcyo.library.ex._color
import com.angcyo.library.ex._string
import com.angcyo.library.ex.dp
import com.angcyo.library.ex.isDebug
import com.angcyo.widget.DslViewHolder

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/29
 */
open class CanvasLayerBaseItem : DslAdapterItem(), ICanvasRendererItem {

    //region ---core---

    override var itemRenderer: BaseRenderer? = null

    override var itemRenderDelegate: CanvasRenderDelegate? = null

    val itemRenderParams = RenderParams(overrideSize = 30 * dp)

    /**是否需要显示雕刻参数参数*/
    var itemShowEngraveParams = true

    /**切割类型改变的回调*/
    var onItemCutTypeChangeAction: Action? = null

    //endregion ---core---

    //region ---计算属性---

    val operateRenderer: BaseRenderer?
        get() = if (itemRenderer is CanvasGroupRenderer) {
            (itemRenderer as CanvasGroupRenderer).rendererList.firstOrNull()
        } else {
            itemRenderer
        }

    val itemLayerHide: Boolean get() = itemRenderer?.isVisible == false

    val itemLayerLock: Boolean get() = itemRenderer?.isLock == true

    val itemItemDrawable: Drawable?
        get() = itemRenderer?.requestRenderDrawable(itemRenderParams.overrideSize)

    /**操作的元素结构*/
    val operateElementBean: LPElementBean? get() = operateRenderer?.lpElementBean()

    val itemItemName: CharSequence?
        get() = itemRenderer?.run {
            if (this is CanvasGroupRenderer) {
                operateElementBean?.groupName
            } else {
                lpElementBean()?.name
            }
        }

    /**当前的[itemRenderer]范围是否超出设备物理尺寸*/
    val isOverflowBounds: Boolean
        get() = itemRenderer?.renderProperty?.getRenderBounds().isOverflowProductBounds()

    protected val laserPeckerModel = vmApp<LaserPeckerModel>()
    protected val deviceStateModel = vmApp<DeviceStateModel>()
    protected val nightModel = vmApp<NightModel>()

    /**是否要显示切割按钮*/
    val itemShowSlicingView: Boolean
        get() = deviceStateModel.haveCutLayer() && operateElementBean?._layerMode == LPDataConstant.DATA_MODE_GCODE

    //endregion ---计算属性---

    init {
        itemLayoutId = R.layout.item_canvas_base_layer_layout
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
        val renderer = itemRenderer

        //item 名称
        itemHolder.tv(R.id.layer_item_name_view)?.text =
            itemItemName ?: operateElementBean?.mtype?.toTypeNameString()

        itemHolder.img(R.id.layer_item_drawable_view)?.apply {
            setImageDrawable(itemItemDrawable /*?: renderer?.preview(itemRenderParams)*/)
            if (nightModel.isDarkMode) {
                setBackgroundColor(_color(R.color.lib_theme_icon_color))
            }
        }

        //元素参数
        if (itemShowEngraveParams && HawkEngraveKeys.enableItemEngraveParams && renderer is CanvasElementRenderer) {
            itemHolder.visible(R.id.layer_item_params_view)
            itemHolder.tv(R.id.layer_item_params_view)?.text = buildString {
                renderer.lpElementBean()?.let { bean ->
                    if (vmApp<LaserPeckerModel>().isCSeries()) {
                        append(_string(R.string.engrave_precision));append(":")
                        append(bean.printPrecision ?: HawkEngraveKeys.lastPrecision);append(" ")
                    } else {
                        LaserPeckerHelper.findProductSupportLaserTypeList()
                            .find {
                                it.type == (bean.printType
                                    ?: DeviceHelper.getProductLaserType()).toByte()
                            }
                            ?.let { append(it.toText());append(" ") }
                    }

                    append(_string(R.string.custom_power));append(":")
                    append(bean.printPower ?: HawkEngraveKeys.lastPower);append("% ")

                    append(_string(R.string.custom_speed));append(":")
                    append(bean.printDepth ?: HawkEngraveKeys.lastDepth);append("% ")

                    append(_string(R.string.print_times));append(":")
                    append(bean.printCount ?: 1);append(" ")

                    if (isDebug()) {
                        //显示数据索引, 方便调试
                        bean.index?.let {
                            appendLine()
                            append("$it")
                        }
                    }
                }
            }
        } else {
            itemHolder.gone(R.id.layer_item_params_view)
        }

        //超范围警告
        itemHolder.visible(R.id.layer_item_warn_view, isOverflowBounds)
        itemHolder.click(R.id.layer_item_warn_view) {
            it.context.messageDialog {
                dialogTitle = _string(R.string.engrave_bounds_warn)
                dialogMessage = overflowBoundsMessage
            }
        }

        //转换切割图层
        itemHolder.invisible(R.id.layer_slicing_view, !itemShowSlicingView)
        itemHolder.img(R.id.layer_slicing_view)?.imageTintList = ColorStateList.valueOf(
            if (operateElementBean?._layerId == LayerHelper.LAYER_CUT) {
                _color(R.color.colorAccent)
            } else {
                _color(R.color.lib_text_color)
            }
        )

        itemHolder.click(R.id.layer_slicing_view) {
            if (operateElementBean?._layerId == LayerHelper.LAYER_CUT) {
                operateElementBean?.layerId = null
                operateElementBean?.isCut = false
            } else {
                operateElementBean?.isCut = true
                operateElementBean?.layerId = LayerHelper.LAYER_CUT
            }
            operateElementBean?.index = null // 重置索引
            onItemCutTypeChangeAction?.invoke()
        }
    }

    /**将画布移动显示到目标元素*/
    fun showItemRendererBounds() {
        itemRenderDelegate?.showRendererBounds(itemRenderer)
    }
}