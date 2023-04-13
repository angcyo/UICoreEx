package com.angcyo.canvas2.laser.pecker.engrave.dslitem.preview

import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.core.vmApp
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.BaseDiameterItem
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.engrave2.model.PreviewModel
import com.angcyo.library.ex._string
import com.angcyo.objectbox.laser.pecker.entity.PreviewConfigEntity
import com.angcyo.objectbox.laser.pecker.lpSaveEntity
import com.angcyo.widget.DslViewHolder

/**
 * 雕刻物理直径item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/07/14
 */

class PreviewDiameterItem : BaseDiameterItem() {

    /**参数配置实体*/
    var itemPreviewConfigEntity: PreviewConfigEntity? = null

    //预览模式
    val previewModel = vmApp<PreviewModel>()

    init {
        itemDiameterLabel = _string(R.string.object_diameter)
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        val diameterPixel =
            itemPreviewConfigEntity?.diameterPixel ?: HawkEngraveKeys.lastDiameterPixel
        itemDiameter = diameterPixel
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
    }

    override fun onItemChangeListener(item: DslAdapterItem) {
        super.onItemChangeListener(item)
        HawkEngraveKeys.lastDiameterPixel = itemDiameter

        itemPreviewConfigEntity?.diameterPixel = itemDiameter
        itemPreviewConfigEntity?.lpSaveEntity()

        //通知机器
        previewModel.refreshPreview()
    }
}
