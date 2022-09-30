package com.angcyo.engrave.dslitem.engrave

import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.R
import com.angcyo.engrave.model.EngraveModel
import com.angcyo.engrave.transition.EngraveTransitionManager
import com.angcyo.engrave.widget.EngraveProgressView
import com.angcyo.library.ex._string
import com.angcyo.widget.DslViewHolder

/**
 * 雕刻进度item, 多图层的雕刻进度
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/02
 */
class EngraveProgressItem : DslAdapterItem() {

    /**雕刻的信息*/
    var itemEngraveState: EngraveModel.EngraveState? = null

    init {
        itemLayoutId = R.layout.item_engrave_progress_layout
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        itemHolder.tv(R.id.lib_text_view)?.text = "${_string(R.string.progress)}:"
        val layerInfo =
            EngraveTransitionManager.getEngraveLayer(itemEngraveState?.engraveDataParam?.layerMode)
        itemHolder.tv(R.id.engrave_layer_view)?.text = layerInfo?.toText()

        itemHolder.v<EngraveProgressView>(R.id.engrave_progress_view)?.apply {
            progressValue = itemEngraveState?.progress ?: 0
        }
    }
}