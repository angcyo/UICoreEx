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
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.engrave.appendDrawable
import com.angcyo.canvas2.laser.pecker.util.lpElementBean
import com.angcyo.core.component.model.NightModel
import com.angcyo.core.vmApp
import com.angcyo.dialog.messageDialog
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave2.EngraveFlowDataHelper
import com.angcyo.engrave2.model.EngraveModel
import com.angcyo.engrave2.transition.overflowBoundsMessage
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.laserpacker.bean.LPElementBean
import com.angcyo.laserpacker.device.DeviceHelper
import com.angcyo.laserpacker.toTypeNameString
import com.angcyo.library.ex.Action
import com.angcyo.library.ex.Anim
import com.angcyo.library.ex._color
import com.angcyo.library.ex._string
import com.angcyo.library.ex.dp
import com.angcyo.library.ex.dpi
import com.angcyo.library.ex.isDebug
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.progress.DslProgressBar
import com.angcyo.widget.span.span

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
    var itemShowEngraveParams = false

    /**item是否在雕刻图层tab页中*/
    var itemInEngraveLayerTab: Boolean = false

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
    protected val engraveModel = vmApp<EngraveModel>()

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

        //2024-3-14 高亮选中的item
        itemIsSelected = itemRenderDelegate?.selectorManager?.getSelectorRendererList()
            ?.contains(itemRenderer) == true

        itemHolder.visible(R.id.background_view, itemIsSelected)

        //进度通知
        itemHolder.visible(
            R.id.layer_item_progress_view,
            itemInEngraveLayerTab && HawkEngraveKeys.enableLayerEngraveInfo
        )
        if (itemInEngraveLayerTab && HawkEngraveKeys.enableLayerEngraveInfo) {
            val progress = engraveModel.getEngraveIndexProgress(operateElementBean?.index) ?: -1
            itemHolder.v<DslProgressBar>(R.id.layer_item_progress_view)?.apply {
                enableProgressFlowMode = progress in 0..99
                setProgress(
                    progress,
                    animDuration = if (enableProgressFlowMode) Anim.ANIM_DURATION else 0L
                )
            }
        }

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
        if (itemShowEngraveParams && renderer is CanvasElementRenderer) {
            itemHolder.visible(R.id.layer_item_params_view)
            itemHolder.tv(R.id.layer_item_params_view)?.text = span {
                renderer.lpElementBean()?.let { bean ->
                    bean.initEngraveParamsIfNeed()
                    val drawableSize = 18 * dpi

                    if (isDebug()) {
                        //显示数据索引, 方便调试
                        bean.index?.let {
                            append("$it"); append(" ")
                        }
                    }

                    //雕刻模块
                    val type = (bean.printType ?: DeviceHelper.getProductLaserType()).toByte()
                    val laserInfo = vmApp<DeviceStateModel>().getDeviceLaserModule(type)
                    val label = laserInfo?.toLabel() ?: "$type"
                    appendDrawable(R.drawable.engrave_config_module_svg, drawableSize)
                    append(label); append(" ")

                    //材质名
                    EngraveFlowDataHelper.getEngraveMaterNameByKey(bean.materialKey).let {
                        appendDrawable(R.drawable.engrave_config_material_svg, drawableSize)
                        append(it); append(" ")
                    }

                    //换行分割
                    appendLine()

                    bean.dpi?.let {
                        val findPxInfo = LaserPeckerHelper.findPxInfo(
                            bean._layerId ?: LaserPeckerHelper.LAYER_LINE, it
                        )
                        appendDrawable(R.drawable.engrave_config_dpi_svg, drawableSize)
                        append(findPxInfo.toText()); append(" ")
                    }

                    if (vmApp<LaserPeckerModel>().isCSeries()) {
                        appendDrawable(R.drawable.engrave_config_precision_svg, drawableSize)
                        append(bean.printPrecision ?: HawkEngraveKeys.lastPrecision); append(" ")
                    }

                    appendDrawable(R.drawable.engrave_config_power_svg, drawableSize)
                    append(bean.printPower ?: HawkEngraveKeys.lastPower);append("% ")

                    appendDrawable(R.drawable.engrave_config_depth_svg, drawableSize)
                    append(bean.printDepth ?: HawkEngraveKeys.lastDepth);append("% ")

                    appendDrawable(R.drawable.engrave_config_times_svg, drawableSize)
                    append(bean.printCount ?: 1);append(" ")
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
            if (operateElementBean?._layerId == LaserPeckerHelper.LAYER_CUT) {
                _color(R.color.colorAccent)
            } else {
                _color(R.color.lib_text_color)
            }
        )

        itemHolder.click(R.id.layer_slicing_view) {
            operateElementBean?.isCut = operateElementBean?._layerId != LaserPeckerHelper.LAYER_CUT
            operateElementBean?.clearIndex("切割类型改变")
            onItemCutTypeChangeAction?.invoke()
        }
    }

    /**将画布移动显示到目标元素*/
    fun showItemRendererBounds() {
        itemRenderDelegate?.showRendererBounds(itemRenderer)
    }
}