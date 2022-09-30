package com.angcyo.engrave.dslitem.engrave

import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.R
import com.angcyo.engrave.model.EngraveModel
import com.angcyo.library.ex._string
import com.angcyo.widget.DslViewHolder

/**
 * 雕刻控制item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/27
 */
class EngravingControlItem : DslAdapterItem() {

    /**雕刻状态*/
    var itemEngraveState: EngraveModel.EngraveState? = null

    var itemPauseAction: (isPause: Boolean) -> Unit = {}

    var itemStopAction: () -> Unit = {}

    init {
        itemLayoutId = R.layout.item_engrave_control_layout
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        val isPause = itemEngraveState?.state == EngraveModel.ENGRAVE_STATE_PAUSE
        itemHolder.tv(R.id.pause_button)?.text = if (isPause) {
            _string(R.string.engrave_continue)
        } else {
            _string(R.string.engrave_pause)
        }

        itemHolder.click(R.id.pause_button) {
            itemPauseAction(isPause)
        }

        itemHolder.click(R.id.stop_button) {
            itemStopAction()
        }
    }

}