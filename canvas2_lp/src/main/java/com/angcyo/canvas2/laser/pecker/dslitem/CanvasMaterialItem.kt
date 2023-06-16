package com.angcyo.canvas2.laser.pecker.dslitem

import android.graphics.drawable.Drawable
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.core.component.model.NightModel
import com.angcyo.core.vmApp
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.library.ex._color
import com.angcyo.widget.DslViewHolder

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/07/09
 */
class CanvasMaterialItem : DslAdapterItem() {

    var itemDrawable: Drawable? = null

    val nightModel = vmApp<NightModel>()

    init {
        itemLayoutId = R.layout.item_canvas_material_layout
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        itemHolder.img(R.id.lib_image_view)?.apply {
            if (nightModel.isDarkMode) {
                setBackgroundColor(_color(R.color.colorPrimaryDark))
            }
            //nightModel.tintImageViewNight(this)
            setImageDrawable(itemDrawable)
        }
    }

}