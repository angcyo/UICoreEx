package com.angcyo.github.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import com.angcyo.dialog.BaseDialogConfig
import com.angcyo.dialog.configBottomDialog
import com.angcyo.github.R
import com.angcyo.library.L
import com.angcyo.library.ex.elseNull
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.base.motionEvent
import com.skydoves.colorpickerview.AlphaTileView
import com.skydoves.colorpickerview.ColorEnvelope
import com.skydoves.colorpickerview.ColorPickerView
import com.skydoves.colorpickerview.flag.BubbleFlag
import com.skydoves.colorpickerview.flag.FlagMode
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import com.skydoves.colorpickerview.sliders.AlphaSlideBar
import com.skydoves.colorpickerview.sliders.BrightnessSlideBar

/**
 * 颜色选择Dialog
 * https://github.com/LaserPeckerIst/ColorPickerView
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/14
 */
class ColorPickerDialogConfig : BaseDialogConfig() {

    /**默认的颜色*/
    var initialColor: Int = Color.WHITE

    /**选中的颜色*/
    var selectedColor: Int = Color.TRANSPARENT

    /**调色板, 从此[Drawable]中获取颜色*/
    var paletteDrawable: Drawable? = null

    /**选中回调, 返回true拦截默认操作*/
    var colorPickerAction: (dialog: Dialog, color: Int) -> Boolean =
        { dialog, color ->
            L.i("选中颜色->$color")
            false
        }

    init {
        dialogTitle = "请选择颜色"
        dialogLayoutId = R.layout.lib_dialog_color_picker_layout

        positiveButtonListener = { dialog, _ ->

            if (colorPickerAction.invoke(dialog, selectedColor)) {
                //被拦截
            } else {
                dialog.dismiss()
            }
        }
    }

    override fun initDialogView(dialog: Dialog, dialogViewHolder: DslViewHolder) {
        super.initDialogView(dialog, dialogViewHolder)

        val colorPickerView = dialogViewHolder.v<ColorPickerView>(R.id.lib_color_picker_view)
        val alphaSlideBar = dialogViewHolder.v<AlphaSlideBar>(R.id.lib_alpha_slide_bar)
        val brightnessSlideBar =
            dialogViewHolder.v<BrightnessSlideBar>(R.id.lib_brightness_slide_bar)
        val alphaTileView =
            dialogViewHolder.v<AlphaTileView>(R.id.lib_alpha_tile_view)
        val textView = dialogViewHolder.tv(R.id.lib_text_view)

        colorPickerView?.apply {
            //slider
            attachAlphaSlider(alphaSlideBar!!)
            attachBrightnessSlider(brightnessSlideBar!!)

            //回调
            setColorListener(object : ColorEnvelopeListener {
                override fun onColorSelected(envelope: ColorEnvelope, fromUser: Boolean) {
                    selectedColor = envelope.color
                    alphaTileView?.setPaintColor(selectedColor)
                    textView?.text = "#${envelope.hexCode}"
                }
            })

            //set
            paletteDrawable?.let {
                setPaletteDrawable(it)
                post {
                    //发送一个手势事件, 触发颜色回调
                    val event =
                        motionEvent(MotionEvent.ACTION_UP, measuredWidth / 2f, measuredHeight / 2f)
                    dispatchTouchEvent(event)
                    event.recycle()

                    //flag
                    val bubbleFlag = BubbleFlag(context)
                    bubbleFlag.flagMode = FlagMode.FADE
                    colorPickerView.flagView = bubbleFlag
                }
            }.elseNull {
                //flag
                val bubbleFlag = BubbleFlag(context)
                bubbleFlag.flagMode = FlagMode.FADE
                colorPickerView.flagView = bubbleFlag

                setInitialColor(initialColor)
            }
        }
    }
}


/**
 * 颜色选择对话框
 * */
fun Context.colorPickerDialog(config: ColorPickerDialogConfig.() -> Unit): Dialog {
    return ColorPickerDialogConfig().run {
        configBottomDialog(this@colorPickerDialog)
        //initialColor
        //paletteDrawable
        config()
        show()
    }
}

