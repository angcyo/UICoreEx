package com.angcyo.canvas2.laser.pecker.engrave.dslitem.engrave

import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.widget.DslViewHolder

/**
 * 雕刻完成后的控制item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/27
 */
class EngraveFinishControlItem : DslAdapterItem() {

    /**是否要显示分享按钮*/
    var itemShowShareButton = true

    /**是否要显示继续雕刻按钮*/
    var itemShowContinueButton = false

    /**工程分享回调*/
    var itemShareAction: () -> Unit = {}

    /**再雕一次回调*/
    var itemAgainAction: () -> Unit = {}

    /**继续雕刻回调*/
    var itemContinueAction: () -> Unit = {}

    init {
        itemLayoutId = R.layout.item_engrave_finish_control_layout
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        itemHolder.visible(R.id.share_button, itemShowShareButton)
        itemHolder.visible(R.id.continue_button, itemShowContinueButton)

        itemHolder.click(R.id.share_button) {
            itemShareAction()
        }
        itemHolder.click(R.id.again_button) {
            itemAgainAction()
        }
        itemHolder.click(R.id.continue_button) {
            itemContinueAction()
        }
    }

}