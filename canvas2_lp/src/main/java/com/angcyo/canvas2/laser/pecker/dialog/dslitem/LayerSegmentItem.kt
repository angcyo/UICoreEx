package com.angcyo.canvas2.laser.pecker.dialog.dslitem

import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.EngraveSegmentScrollItem
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.item.style.itemCurrentIndex
import com.angcyo.laserpacker.device.LayerHelper
import com.angcyo.library.ex._string

/**
 * 材质参数对照表-图层选择
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/04/04
 */
class LayerSegmentItem : EngraveSegmentScrollItem() {

    /**是否要包含切割图层*/
    var itemIncludeCutLayer = true

    init {
        itemText = _string(R.string.engrave_layer_config)
        itemSegmentList = LayerHelper.getEngraveLayerList(itemIncludeCutLayer)
    }

    override fun onItemChangeListener(item: DslAdapterItem) {
        //super.onItemChangeListener(item)
    }

    /**当前图层信息*/
    fun currentLayerInfo() = LayerHelper.getEngraveLayerList(itemIncludeCutLayer)[itemCurrentIndex]
}