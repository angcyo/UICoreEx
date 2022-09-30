package com.angcyo.engrave.dslitem.engrave

import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.R
import com.angcyo.engrave.dslitem.EngraveSegmentScrollItem
import com.angcyo.library.ex._string

/**
 * 雕刻图层配置切换item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/27
 */
class EngraveLayerConfigItem : EngraveSegmentScrollItem() {

    init {
        itemText = _string(R.string.engrave_layer_config)
    }

    override fun onItemChangeListener(item: DslAdapterItem) {
        super.onItemChangeListener(item)
    }

}