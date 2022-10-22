package com.angcyo.engrave.auto

import android.graphics.drawable.Drawable
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.core.vmApp
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.library.ex._string
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.progress.DslProgressBar

/**
 * 自动雕刻控制item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/10/22
 */
class AutoEngraveItem : DslAdapterItem() {

    /**需要显示的名字*/
    var itemShowName: CharSequence? = null

    /**提示信息, 传输中...雕刻中...*/
    var itemTip: CharSequence? = null

    /**需要预览图片*/
    var itemDrawable: Drawable? = null

    /**进度*/
    var itemProgress: Int = -1

    var itemPauseAction: (isPause: Boolean) -> Unit = {}

    var itemStopAction: () -> Unit = {}

    init {
        itemLayoutId = R.layout.item_auto_engrave_layout
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        itemHolder.tv(R.id.name_view)?.text = itemShowName
        itemHolder.tv(R.id.tip_view)?.text = itemTip
        itemHolder.img(R.id.image_view)?.setImageDrawable(itemDrawable)
        itemHolder.v<DslProgressBar>(R.id.progress_view)?.apply {
            enableProgressFlowMode = true
            if (itemProgress == -1) {
                showProgressText = false
                setProgress(100, animDuration = 0)
            } else {
                showProgressText = true
                setProgress(itemProgress)
            }
        }
        val isPause = vmApp<LaserPeckerModel>().deviceStateData.value?.isEngravePause() == true
        itemHolder.tv(R.id.pause_button)?.text = if (isPause) {
            _string(R.string.engrave_continue)
        } else {
            _string(R.string.engrave_pause)
        }

        //
        itemHolder.click(R.id.pause_button) {
            itemPauseAction(isPause)
        }

        itemHolder.click(R.id.stop_button) {
            itemStopAction()
        }
    }
}