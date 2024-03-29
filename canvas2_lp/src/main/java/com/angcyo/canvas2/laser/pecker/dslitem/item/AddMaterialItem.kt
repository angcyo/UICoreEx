package com.angcyo.canvas2.laser.pecker.dslitem.item

import android.graphics.drawable.BitmapDrawable
import com.angcyo.base.contentView
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dialog.canvasMaterialWindow
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasIconItem
import com.angcyo.canvas2.laser.pecker.manager.GuideManager
import com.angcyo.canvas2.laser.pecker.util.LPElementHelper
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.updateItemSelected
import com.angcyo.gcode.GCodeDrawable
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.library.component.lastActivity
import com.angcyo.library.ex._string
import com.angcyo.library.ex.toBitmap
import com.angcyo.widget.DslViewHolder
import com.hingin.umeng.UMEvent
import com.hingin.umeng.umengEventValue
import com.pixplicity.sharp.SharpDrawable

/**
 * 添加素材
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/03
 */
class AddMaterialItem : CanvasIconItem() {

    init {
        itemIco = R.drawable.canvas_material_ico
        itemText = _string(R.string.canvas_material)
        itemEnable = true

        itemClick = {
            updateItemSelected(!itemIsSelected)
            it.context.canvasMaterialWindow(it) {
                onDismiss = {
                    updateItemSelected(false)
                    false
                }
                onDrawableAction = { data, drawable ->
                    when (drawable) {
                        //bitmap
                        is BitmapDrawable -> LPElementHelper.addBitmapElement(
                            itemRenderDelegate,
                            drawable.bitmap
                        )
                        //gcode
                        is GCodeDrawable -> LPElementHelper.addPathElement(
                            itemRenderDelegate,
                            LPDataConstant.DATA_TYPE_GCODE,
                            data as String,
                            drawable.gCodePath.run { listOf(this) }
                        )
                        //svg
                        is SharpDrawable -> LPElementHelper.addPathElement(
                            itemRenderDelegate,
                            LPDataConstant.DATA_TYPE_SVG,
                            data as String,
                            drawable.pathList
                        )
                        //other
                        else -> {
                            LPElementHelper.addBitmapElement(
                                itemRenderDelegate,
                                drawable.toBitmap()
                            )
                        }
                    }
                    UMEvent.CANVAS_MATERIAL.umengEventValue()
                }
            }
        }
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
        GuideManager.checkOrShowGuide(lastActivity?.window?.contentView(), itemHolder.itemView, 1)
    }
}