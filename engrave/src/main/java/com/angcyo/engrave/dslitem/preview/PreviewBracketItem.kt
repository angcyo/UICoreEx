package com.angcyo.engrave.dslitem.preview

import android.widget.TextView
import com.angcyo.bluetooth.fsc.IReceiveBeanAction
import com.angcyo.bluetooth.fsc.enqueue
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.command.EngravePreviewCmd
import com.angcyo.bluetooth.fsc.laserpacker.parse.EngravePreviewParser
import com.angcyo.bluetooth.fsc.parse
import com.angcyo.canvas.data.ItemDataBean.Companion.mmUnit
import com.angcyo.canvas.data.toMm
import com.angcyo.canvas.data.toPixel
import com.angcyo.core.vmApp
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.R
import com.angcyo.engrave.data.HawkEngraveKeys
import com.angcyo.item.keyboard.keyboardNumberWindow
import com.angcyo.library.annotation.MM
import com.angcyo.library.ex._string
import com.angcyo.library.ex.clamp
import com.angcyo.library.toast
import com.angcyo.library.unit.IValueUnit
import com.angcyo.library.unit.unitDecimal
import com.angcyo.widget.DslViewHolder

/**
 * 支架控制item
 * 支架上升/下降/停止
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2022/09/23
 * Copyright (c) 2020 angcyo. All rights reserved.
 */
class PreviewBracketItem : DslAdapterItem() {

    companion object {
        /**支架的最大移动步长*/
        @MM
        val BRACKET_MAX_STEP: Int = 65535//130, 65535
    }

    /**单位*/
    var itemValueUnit: IValueUnit? = null

    //产品模式
    val laserPeckerModel = vmApp<LaserPeckerModel>()

    init {
        itemLayoutId = R.layout.item_preview_bracket_layout
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        val heightPixel = HawkEngraveKeys.lastBracketHeight.toPixel()
        val valueUnit = itemValueUnit ?: mmUnit
        val value = valueUnit.convertPixelToValue(heightPixel)

        itemHolder.tv(R.id.bracket_height_view)?.text = value.unitDecimal()
        itemHolder.tv(R.id.unit_label_view)?.text = valueUnit.getUnit()

        //
        itemHolder.click(R.id.bracket_height_view) {
            it.context.keyboardNumberWindow(it) {
                onDismiss = {
                    updateAdapterItem()
                    false
                }
                keyboardBindTextView = it as? TextView
                onNumberResultAction = { number ->
                    val numberPixel = valueUnit.convertValueToPixel(number)
                    var size = numberPixel.toMm()
                    size = clamp(size, 1f, BRACKET_MAX_STEP.toFloat())
                    HawkEngraveKeys.lastBracketHeight = size
                }
            }
        }
        itemHolder.click(R.id.bracket_up_view) {
            bracketUpCmd()
        }
        itemHolder.click(R.id.bracket_down_view) {
            bracketDownCmd()
        }
        itemHolder.click(R.id.bracket_stop_view) {
            bracketStopCmd()
        }
    }

    //

    /**支架上升*/
    fun bracketUpCmd(action: IReceiveBeanAction? = null) {
        val cmd = EngravePreviewCmd.previewBracketUp(HawkEngraveKeys.lastBracketHeight.toInt())
        cmd.enqueue { bean, error ->
            if (bean?.parse<EngravePreviewParser>()?.isBracketConnect() != true) {
                toast(_string(R.string.bracket_not_connect))
            }
            action?.invoke(bean, error)
        }
    }

    /**支架下降*/
    fun bracketDownCmd(action: IReceiveBeanAction? = null) {
        val cmd = EngravePreviewCmd.previewBracketDown(HawkEngraveKeys.lastBracketHeight.toInt())
        cmd.enqueue { bean, error ->
            if (bean?.parse<EngravePreviewParser>()?.isBracketConnect() != true) {
                toast(_string(R.string.bracket_not_connect))
            }
            action?.invoke(bean, error)
        }
    }

    /**停止支架*/
    fun bracketStopCmd(action: IReceiveBeanAction? = null) {
        val cmd = EngravePreviewCmd.previewBracketStop()
        cmd.enqueue { bean, error ->
            if (bean?.parse<EngravePreviewParser>()?.isBracketConnect() != true) {
                toast(_string(R.string.bracket_not_connect))
            }
            action?.invoke(bean, error)
        }
    }
}