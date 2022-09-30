package com.angcyo.engrave.dslitem.engrave

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.core.vmApp
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.R
import com.angcyo.engrave.data.EngraveConfigInfo
import com.angcyo.engrave.model.EngraveModel
import com.angcyo.engrave.toEngraveTime
import com.angcyo.library.ex._string
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.span.span

/**
 * 雕刻完成信息item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/27
 */
class EngraveFinishTopItem : DslAdapterItem() {

    /**雕刻配置信息*/
    var itemEngraveConfigInfo: EngraveConfigInfo? = null

    val engraveMode = vmApp<EngraveModel>()

    init {
        itemLayoutId = R.layout.item_engrave_finish_top_layout
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        val engraveDataParam = itemEngraveConfigInfo?.engraveDataParamList?.firstOrNull()
        itemHolder.tv(R.id.lib_text_view)?.text = span {
            append(_string(R.string.custom_material))
            append(":")
            append(engraveDataParam?.materialName)

            append(" ")
            append(_string(R.string.resolution_ratio))
            append(":")
            val pxInfo = LaserPeckerHelper.findPxInfo(engraveDataParam?.dataList?.firstOrNull()?.px)
            append(pxInfo?.des)

            append(" ")
            append(_string(R.string.work_time))
            append(":")
            val startEngraveTime = engraveMode._engraveTask?.startTime ?: 0
            val endEngraveTime = engraveMode._engraveTask?.finishTime ?: 0
            val engraveTime = (endEngraveTime - startEngraveTime).toEngraveTime()
            append(engraveTime)
        }
    }

}