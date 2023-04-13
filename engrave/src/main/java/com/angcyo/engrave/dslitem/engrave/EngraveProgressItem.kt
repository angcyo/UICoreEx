package com.angcyo.engrave.dslitem.engrave

import android.view.Gravity
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.EngraveFlowDataHelper
import com.angcyo.engrave.R
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.library.ex._string
import com.angcyo.library.ex.size
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.progress.DslProgressBar
import com.angcyo.widget.span.span

/**
 * 雕刻进度item, 多图层的雕刻进度
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/02
 */
class EngraveProgressItem : DslAdapterItem() {

    /**雕刻任务id*/
    var itemTaskId: String? = null

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
        itemHolder.tv(R.id.engrave_layer_view)?.text = if (HawkEngraveKeys.enableItemTopOrder) {
            null
        } else {
            span {
                val engraveLayerList = EngraveFlowDataHelper.getEngraveLayerList(itemTaskId)
                engraveLayerList.forEach {
                    drawable {
                        showText = it.toText()
                        spanWeight = 1f / engraveLayerList.size() - 0.001f
                        textGravity = Gravity.CENTER
                    }
                }
            }
        }

        /*val engraveLayerInfo = EngraveFlowDataHelper.getCurrentEngraveLayer(itemTaskId)
        itemHolder.tv(R.id.engrave_layer_view)?.text = engraveLayerInfo?.toText()*/

        /*itemHolder.v<EngraveProgressView>(R.id.engrave_progress_view)?.apply {
            val progress = EngraveFlowDataHelper.calcEngraveProgress(itemTaskId)
            progressValue = progress
        }*/

        itemHolder.v<DslProgressBar>(R.id.engrave_progress_view)?.apply {
            val progress = EngraveFlowDataHelper.calcEngraveProgress(itemTaskId)
            setProgress(progress.toFloat())
        }
    }
}