package com.angcyo.engrave.ble

import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.R
import com.angcyo.library.ex._string
import com.angcyo.library.ex.toElapsedTime

import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.progress.DslProgressBar

/**
 * 进度提示/时间信息item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/02
 */
class EngraveProgressItem : DslAdapterItem() {

    /**[0~100]*/
    var itemProgress: Int = 0

    /**进度条的Flow模式*/
    var itemEnableProgressFlowMode: Boolean = true

    /**提示*/
    var itemTip: CharSequence? = null

    /**剩余时间: 全字符*/
    var itemTimeStr: CharSequence? = null

    /**剩余时间, 毫秒*/
    var itemTime: Long? = null

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

        itemHolder.v<DslProgressBar>(R.id.lib_progress_bar)?.apply {
            enableProgressFlowMode = itemEnableProgressFlowMode
            setProgress(itemProgress, animDuration = 0)
        }

        itemHolder.tv(R.id.lib_tip_view)?.text = itemTip
        itemHolder.visible(R.id.lib_time_view, itemTimeStr != null || itemTime != null)

        itemHolder.tv(R.id.lib_time_view)?.text = if (itemTimeStr != null) {
            itemTimeStr
        } else if ((itemTime ?: 0) <= 0) {
            "${_string(R.string.estimated_remaining_time)}--"
        } else {
            "${_string(R.string.estimated_remaining_time)}${
                itemTime?.toElapsedTime(
                    pattern = intArrayOf(-1, 1, 1),
                    units = arrayOf("", "", ":", ":", ":")
                )
            }"
        }
    }
}