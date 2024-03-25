package com.angcyo.canvas2.laser.pecker.dslitem.control

import androidx.fragment.app.Fragment
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasIconItem
import com.angcyo.canvas2.laser.pecker.util.LPBitmapHandler
import com.angcyo.dsladapter.item.IFragmentItem
import com.angcyo.dsladapter.updateItemSelected
import com.angcyo.item.style.itemHaveNew
import com.angcyo.library.ex._string

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2024/03/25
 *
 * 路径偏移, 支持多个路径
 */
class PathOffsetItem : CanvasIconItem(), IFragmentItem {
    override var itemFragment: Fragment? = null

    init {
        itemIco = R.drawable.crop_auto_side_icon
        itemText = _string(R.string.canvas_outline)

        itemClick = {
            itemHaveNew = false
            updateItemSelected(!itemIsSelected)
            if (itemIsSelected) {
                //UMEvent.CANVAS_PATH_FILL.umengEventValue()
                LPBitmapHandler.handlePathOffset(
                    itemRenderDelegate,
                    it,
                    itemFragment,
                    itemRenderer
                ) {
                    updateItemSelected(false)
                }
            }
        }
    }
}