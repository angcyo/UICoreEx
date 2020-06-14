package com.angcyo.amap3d.dslitem

import com.angcyo.amap3d.R
import com.angcyo.amap3d.core.MapLocation
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.widget.DslViewHolder

/**
 * 地图选点POI item
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/06/13
 * Copyright (c) 2020 angcyo. All rights reserved.
 */
class DslSelectPoiItem : DslAdapterItem() {

    var itemPoiName: CharSequence? = null

    var itemPoiAddress: CharSequence? = null

    var itemMapLocation: MapLocation? = null
        set(value) {
            field = value
            itemPoiName = value?.poiName
            itemPoiAddress = value?.address
        }

    init {
        itemLayoutId = R.layout.map_select_poi_item
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        itemHolder.visible(R.id.lib_select_view, itemIsSelected)

        itemHolder.tv(R.id.lib_text_view)?.text = itemPoiName

        itemHolder.tv(R.id.lib_sub_text_view)?.text = itemPoiAddress
        itemHolder.visible(R.id.lib_sub_text_view, itemPoiAddress != null)
    }
}