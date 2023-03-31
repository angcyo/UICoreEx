package com.angcyo.canvas2.laser.pecker.engrave.dslitem.preview

import android.widget.TextView
import com.angcyo.bluetooth.fsc.IReceiveBeanAction
import com.angcyo.bluetooth.fsc.enqueue
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.command.EngravePreviewCmd
import com.angcyo.bluetooth.fsc.laserpacker.parse.EngravePreviewParser
import com.angcyo.bluetooth.fsc.parse
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.util.LPConstant
import com.angcyo.core.vmApp
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.laserpacker.device.HawkEngraveKeys
import com.angcyo.item.keyboard.keyboardNumberWindow
import com.angcyo.library.annotation.MM
import com.angcyo.library.ex._string
import com.angcyo.library.ex.clamp
import com.angcyo.library.toast
import com.angcyo.library.unit.toMm
import com.angcyo.library.unit.toPixel
import com.angcyo.library.unit.unitDecimal
import com.angcyo.widget.DslViewHolder
import com.angcyo.library.ex.isTouchFinish

/**
 * 支架控制item
 * 支架上升/下降/停止
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2022/09/23
 * Copyright (c) 2020 angcyo. All rights reserved.
 */
class PreviewBracketItem : DslAdapterItem() {

    //产品模式
    val laserPeckerModel = vmApp<LaserPeckerModel>()

    init {
        itemLayoutId = R.layout.item_preview_bracket_layout
    }

    var _isLongPressHappen = false

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        val heightPixel = HawkEngraveKeys.lastBracketHeight.toPixel()
        val valueUnit = LPConstant.renderUnit
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
                bindPendingDelay = -1 //关闭限流输入
                onNumberResultAction = { number ->
                    val numberPixel = valueUnit.convertValueToPixel(number)
                    var size = numberPixel.toMm()
                    size = clamp(size, 1f, EngravePreviewCmd.BRACKET_MAX_STEP.toFloat())
                    HawkEngraveKeys.lastBracketHeight = size
                }
            }
        }
        //支架上升
        itemHolder.longTouch(R.id.bracket_up_view) { view, event, eventType ->
            when (eventType) {
                DslViewHolder.EVENT_TYPE_CLICK -> {
                    _isLongPressHappen = false
                    bracketUpCmd(HawkEngraveKeys.lastBracketHeight.toInt())
                }
                DslViewHolder.EVENT_TYPE_LONG_PRESS -> {
                    _isLongPressHappen = true
                    bracketUpCmd(EngravePreviewCmd.BRACKET_MAX_STEP)
                }
            }
            if (event.isTouchFinish() && _isLongPressHappen) {
                bracketStopCmd()
            }
            true
        }
        //支架下降
        itemHolder.longTouch(R.id.bracket_down_view) { view, event, eventType ->
            when (eventType) {
                DslViewHolder.EVENT_TYPE_CLICK -> {
                    _isLongPressHappen = false
                    bracketDownCmd(HawkEngraveKeys.lastBracketHeight.toInt())
                }
                DslViewHolder.EVENT_TYPE_LONG_PRESS -> {
                    _isLongPressHappen = true
                    bracketDownCmd(EngravePreviewCmd.BRACKET_MAX_STEP)
                }
            }
            if (event.isTouchFinish() && _isLongPressHappen) {
                bracketStopCmd()
            }
            true
        }
        //停止
        itemHolder.click(R.id.bracket_stop_view) {
            bracketStopCmd()
        }
    }

    //

    /**支架上升*/
    fun bracketUpCmd(
        @MM step: Int = EngravePreviewCmd.BRACKET_MAX_STEP,
        action: IReceiveBeanAction? = null
    ) {
        val cmd = EngravePreviewCmd.previewBracketUpCmd(step)
        cmd.enqueue { bean, error ->
            if (bean?.parse<EngravePreviewParser>()?.isBracketConnect() != true) {
                toast(_string(R.string.bracket_not_connect))
            }
            action?.invoke(bean, error)
        }
    }

    /**支架下降*/
    fun bracketDownCmd(
        @MM step: Int = EngravePreviewCmd.BRACKET_MAX_STEP,
        action: IReceiveBeanAction? = null
    ) {
        val cmd = EngravePreviewCmd.previewBracketDownCmd(step)
        cmd.enqueue { bean, error ->
            if (bean?.parse<EngravePreviewParser>()?.isBracketConnect() != true) {
                toast(_string(R.string.bracket_not_connect))
            }
            action?.invoke(bean, error)
        }
    }

    /**停止支架*/
    fun bracketStopCmd(action: IReceiveBeanAction? = null) {
        val cmd = EngravePreviewCmd.previewBracketStopCmd()
        cmd.enqueue { bean, error ->
            if (bean?.parse<EngravePreviewParser>()?.isBracketConnect() != true) {
                toast(_string(R.string.bracket_not_connect))
            }
            action?.invoke(bean, error)
        }
    }
}