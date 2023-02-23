package com.angcyo.engrave.dslitem.preview

import android.view.MotionEvent
import android.view.View
import com.angcyo.dialog.TargetWindow
import com.angcyo.dialog.popup.PopupTipConfig
import com.angcyo.dialog.popup.popupTipWindow
import com.angcyo.drawable.BubbleDrawable
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.R
import com.angcyo.engrave.data.HawkEngraveKeys
import com.angcyo.engrave.data.PreviewInfo
import com.angcyo.library._screenWidth
import com.angcyo.library.ex.interceptParentTouchEvent
import com.angcyo.objectbox.laser.pecker.lpSaveEntity
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.progress.DslProgressBar
import com.angcyo.widget.progress.DslSeekBar

/**
 * 激光亮度item
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2022/09/23
 * Copyright (c) 2020 angcyo. All rights reserved.
 */
class PreviewBrightnessItem : BasePreviewItem() {

    /**提示布局id*/
    var itemPopupTipLayoutId: Int = R.layout.lib_bubble_tip_layout

    /**进度文本格式化*/
    var itemProgressTextFormatAction: (DslProgressBar) -> String = {
        "${(it._progressFraction * 100).toInt()}"
    }

    init {
        itemLayoutId = R.layout.item_preview_laser_brightness_layout
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        itemHolder.v<DslSeekBar>(R.id.lib_seek_view)?.apply {
            progressTextFormatAction = itemProgressTextFormatAction
            val pwrProgress =
                itemPreviewConfigEntity?.pwrProgress ?: HawkEngraveKeys.lastPwrProgress
            setProgress(pwrProgress * 100, animDuration = 0)
            config {
                /*onSeekChanged = { value, fraction, fromUser ->
                    if (!isTouchDown && fromUser) {
                        HawkEngraveKeys.lastPwrProgress = fraction
                        previewModel.refreshPreview(true, false)
                    }
                }*/
                onSeekTouchEnd = { value, fraction ->
                    itemPreviewConfigEntity?.pwrProgress = fraction
                    itemPreviewConfigEntity?.lpSaveEntity()
                    HawkEngraveKeys.lastPwrProgress = fraction

                    itemChanged = true
                    //通知机器
                    previewModel.refreshPreview(true) {
                        zState = PreviewInfo.Z_STATE_PAUSE
                    }
                }
            }
        }

        itemHolder.touch(R.id.lib_seek_view) { view, event ->
            showBubblePopupTip(view, event)
            true
        }
    }

    override fun onItemChangeListener(item: DslAdapterItem) {
        //super.onItemChangeListener(item)
    }

    //---弹窗提示---

    var _window: TargetWindow? = null
    var _popupTipConfig: PopupTipConfig? = null

    fun showBubblePopupTip(view: View, event: MotionEvent) {
        view.interceptParentTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                _window = view.context.popupTipWindow(view, itemPopupTipLayoutId) {
                    touchX = event.x
                    _popupTipConfig = this
                    onInitLayout = { window, viewHolder ->
                        viewHolder.view(com.angcyo.item.R.id.lib_bubble_view)?.background =
                            BubbleDrawable()
                        viewHolder.tv(com.angcyo.item.R.id.lib_text_view)?.text =
                            if (view is DslProgressBar) {
                                itemProgressTextFormatAction(view)
                            } else {
                                "${(touchX * 1f / _screenWidth * 100).toInt()}"
                            }
                    }
                    if (view is DslSeekBar) {
                        limitTouchRect = view._progressBound
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                _popupTipConfig?.apply {
                    touchX = event.x
                    updatePopup()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                //window?.dismiss()
                _popupTipConfig?.hide()
            }
        }
    }
}