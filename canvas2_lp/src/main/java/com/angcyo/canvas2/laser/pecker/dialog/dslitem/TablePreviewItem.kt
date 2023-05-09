package com.angcyo.canvas2.laser.pecker.dialog.dslitem

import android.graphics.Bitmap
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.canvas.render.renderer.CanvasGroupRenderer
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dialog.ParameterComparisonTableDialogConfig
import com.angcyo.canvas2.laser.pecker.util.lpElementBean
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.library.ex._color
import com.angcyo.library.ex.createTextPaint
import com.angcyo.library.ex.drawTextCenter
import com.angcyo.library.unit.toPixel
import com.angcyo.widget.DslViewHolder

/**
 * 用来预览参数推荐表
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/04/17
 */
class TablePreviewItem : DslAdapterItem() {

    var parameterComparisonTableDialogConfig: ParameterComparisonTableDialogConfig? = null
        set(value) {
            field = value
            updatePreview()
        }

    var itemPreviewBitmap: Bitmap? = null

    init {
        itemLayoutId = R.layout.item_table_preview__layout
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
        itemHolder.img(R.id.lib_image_view)?.apply {
            if (itemPreviewBitmap == null) {
                setImageResource(R.drawable.lib_empty_tip)
            } else {
                setImageBitmap(itemPreviewBitmap)
            }
        }
    }

    fun updatePreview() {
        val paint = createTextPaint(
            _color(R.color.colorAccent),
            ParameterComparisonTableDialogConfig.pctTextFontSize.toPixel()
        )
        itemPreviewBitmap =
            CanvasGroupRenderer.createRenderBitmap(
                parameterComparisonTableDialogConfig?.parseParameterComparisonTable(),
                HawkEngraveKeys.projectOutSize.toFloat()
            ) { renderer, canvas, renderProperty, params ->
                val printCount = renderer.lpElementBean()?.printCount ?: 0
                if (printCount > 1) {
                    val renderBounds = renderProperty.getRenderBounds()
                    /*canvas.withSave {
                        translate(renderBounds.left, renderBounds.top)//平移到指定位置
                        drawTextCenter(
                            "$printCount",
                            renderBounds.width() / 2f,
                            renderBounds.height() / 2f,
                            paint
                        )
                    }*/
                    canvas.drawTextCenter(
                        "$printCount",
                        renderBounds.centerX(),
                        renderBounds.centerY(),
                        paint
                    )
                }
            }
        updateAdapterItem()
    }

}