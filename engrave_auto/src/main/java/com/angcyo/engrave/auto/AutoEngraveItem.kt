package com.angcyo.engrave.auto

import android.graphics.drawable.Drawable
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.core.vmApp
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.model.AutoEngraveModel
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

    /**需要预览图片*/
    var itemDrawable: Drawable? = null

    /**自动雕刻任务*/
    var itemAutoEngraveTask: AutoEngraveModel.AutoEngraveTask? = null

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
        itemHolder.img(R.id.image_view)?.setImageDrawable(itemDrawable)
        itemHolder.v<DslProgressBar>(R.id.progress_view)?.apply {
            val progress = itemAutoEngraveTask?.progress ?: -1
            enableProgressFlowMode = true
            if (progress == -1) {
                showProgressText = false
                setProgress(100, animDuration = 0)
            } else {
                showProgressText = true
                setProgress(progress)
            }
        }
        val isPause = vmApp<LaserPeckerModel>().deviceStateData.value?.isEngravePause() == true
        itemHolder.tv(R.id.pause_button)?.text = if (isPause) {
            _string(R.string.engrave_continue)
        } else {
            _string(R.string.engrave_pause)
        }

        //
        itemHolder.tv(R.id.tip_view)?.text = when (itemAutoEngraveTask?.state) {
            AutoEngraveModel.STATE_CREATE -> "正在创建数据..."
            AutoEngraveModel.STATE_TRANSFER -> "正在传输数据..."
            AutoEngraveModel.STATE_ENGRAVE -> "正在镭雕中..."
            else -> "准备中..."
        }
        val visible = itemAutoEngraveTask?.state == AutoEngraveModel.STATE_ENGRAVE
        itemHolder.visible(R.id.pause_button, visible)
        itemHolder.visible(R.id.stop_button, visible)

        //
        itemHolder.click(R.id.pause_button) {
            itemPauseAction(isPause)
            itemHolder.tv(R.id.pause_button)?.text = if (isPause) {
                //已经暂停了, 点击后变成继续预览, 所有按钮还是显示暂停预览
                _string(R.string.engrave_pause)
            } else {
                //
                _string(R.string.engrave_continue)
            }
        }

        itemHolder.click(R.id.stop_button) {
            itemStopAction()
        }
    }
}