package com.angcyo.canvas.laser.pecker

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import com.angcyo.canvas.data.ItemDataBean.Companion.DEFAULT_LINE_SPACE
import com.angcyo.canvas.laser.pecker.dslitem.CanvasDirectionItem2
import com.angcyo.canvas.laser.pecker.dslitem.CanvasSeekBarItem
import com.angcyo.canvas.laser.pecker.dslitem.CanvasSwitchItem
import com.angcyo.canvas.utils.canvasDecimal
import com.angcyo.dialog.TargetWindow
import com.angcyo.dialog.dismissWindow
import com.angcyo.dialog.popup.MenuPopupConfig
import com.angcyo.dsladapter.DslAdapter
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.item.DslBlackButtonItem
import com.angcyo.item.DslSeekBarInfoItem
import com.angcyo.item.style.itemInfoText
import com.angcyo.item.style.itemSwitchChangedAction
import com.angcyo.item.style.itemText
import com.angcyo.library.annotation.DSL
import com.angcyo.library.ex._string
import com.angcyo.library.ex.dpi
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.recycler.DslRecyclerView

/**
 * 属性调整弹窗
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/20
 */
class CanvasRegulatePopupConfig2 : MenuPopupConfig() {

    /**需要调整的项目, 需要啥就添加对应的项
     * [com.angcyo.canvas.laser.pecker.CanvasRegulatePopupConfig.KEY_INVERT]
     * [com.angcyo.canvas.laser.pecker.CanvasRegulatePopupConfig.KEY_THRESHOLD]
     * [com.angcyo.canvas.laser.pecker.CanvasRegulatePopupConfig.KEY_LINE_SPACE]
     * [com.angcyo.canvas.laser.pecker.CanvasRegulatePopupConfig.KEY_DIRECTION]
     * [com.angcyo.canvas.laser.pecker.CanvasRegulatePopupConfig.KEY_ANGLE]
     * [com.angcyo.canvas.laser.pecker.CanvasRegulatePopupConfig.KEY_CONTRAST]
     * [com.angcyo.canvas.laser.pecker.CanvasRegulatePopupConfig.KEY_BRIGHTNESS]
     * */
    val regulateList = mutableListOf<String>()

    /**保存修改后的属性, 用来恢复*/
    var property = CanvasRegulatePopupConfig.keepProperty

    /**是否实时监听改变*/
    var realTimeApply: Boolean = true

    /**首次加载是否需要应用*/
    var firstApply: Boolean = true

    /**应用属性实现方法的回调
     * [dismiss] 是否销毁了弹窗*/
    var onApplyAction: (dismiss: Boolean) -> Unit = {}

    init {
        minHorizontalOffset = 20 * dpi
        onDismiss = {
            onApplyAction(true)
            false
        }
    }

    override fun initRecyclerView(
        window: TargetWindow,
        viewHolder: DslViewHolder,
        recyclerView: DslRecyclerView,
        adapter: DslAdapter
    ) {
        super.initRecyclerView(window, viewHolder, recyclerView, adapter)
        adapter.apply {
            //版画阈值
            if (regulateList.contains(CanvasRegulatePopupConfig.KEY_PRINT_THRESHOLD)) {
                renderThresholdItem(CanvasRegulatePopupConfig.KEY_PRINT_THRESHOLD)
            }
            //印章阈值
            if (regulateList.contains(CanvasRegulatePopupConfig.KEY_SEAL_THRESHOLD)) {
                renderThresholdItem(CanvasRegulatePopupConfig.KEY_SEAL_THRESHOLD)
            }

            //GCode
            if (regulateList.contains(CanvasRegulatePopupConfig.KEY_LINE_SPACE)) {
                CanvasSeekBarItem()() {
                    itemInfoText = _string(R.string.canvas_line_space) //0.125-5
                    initItem()

                    val start = 0.125f
                    val max = 5f
                    val def =
                        getFloatOrDef(CanvasRegulatePopupConfig.KEY_LINE_SPACE, DEFAULT_LINE_SPACE)

                    itemProgressTextFormatAction = {
                        (start + (max - start) * it._progressFraction).canvasDecimal(3)
                    }

                    property[CanvasRegulatePopupConfig.KEY_LINE_SPACE] = def
                    itemSeekProgress = if (def == start) {
                        0
                    } else {
                        ((def / (max - start)) * 100).toInt()
                    }

                    itemSeekTouchEnd = { value, fraction ->
                        property[CanvasRegulatePopupConfig.KEY_LINE_SPACE] =
                            start + (max - start) * fraction
                    }
                }
            }
            if (regulateList.contains(CanvasRegulatePopupConfig.KEY_ANGLE)) {
                CanvasSeekBarItem()() {
                    itemInfoText = _string(R.string.canvas_angle) //0-90
                    initItem()

                    itemProgressTextFormatAction = {
                        (90 * it._progressFraction).canvasDecimal(1)
                    }

                    val def = getFloatOrDef(CanvasRegulatePopupConfig.KEY_ANGLE, 0f)
                    property[CanvasRegulatePopupConfig.KEY_ANGLE] = def
                    itemSeekProgress = ((def / 90f) * 100).toInt()

                    itemSeekTouchEnd = { value, fraction ->
                        property[CanvasRegulatePopupConfig.KEY_ANGLE] = 90f * fraction
                    }
                }
            }
            if (regulateList.contains(CanvasRegulatePopupConfig.KEY_DIRECTION)) {
                CanvasDirectionItem2()() {
                    itemText = _string(R.string.canvas_direction) //0:0 1:90 2:180 3:270
                    itemDirection = getIntOrDef(CanvasRegulatePopupConfig.KEY_DIRECTION, 0)
                    property[CanvasRegulatePopupConfig.KEY_DIRECTION] = itemDirection
                    initItem()

                    itemSelectChangedAction = { fromIndex, toIndex, reselect, fromUser ->
                        property[CanvasRegulatePopupConfig.KEY_DIRECTION] = toIndex
                    }
                }
            }
            //确认按钮
            if (regulateList.contains(CanvasRegulatePopupConfig.KEY_SUBMIT)) {
                DslBlackButtonItem()() {
                    itemButtonText = _string(R.string.dialog_positive)
                    itemClick = {
                        onApplyAction(false)
                        onApplyAction(true)
                        window.dismissWindow()
                    }
                }
            }

            //黑白画
            if (regulateList.contains(CanvasRegulatePopupConfig.KEY_BW_INVERT)) {
                renderInvertItem(CanvasRegulatePopupConfig.KEY_BW_INVERT)
            }
            if (regulateList.contains(CanvasRegulatePopupConfig.KEY_BW_THRESHOLD)) {
                renderThresholdItem(CanvasRegulatePopupConfig.KEY_BW_THRESHOLD)
            }

            //抖动
            if (regulateList.contains(CanvasRegulatePopupConfig.KEY_SHAKE_INVERT)) {
                renderInvertItem(CanvasRegulatePopupConfig.KEY_SHAKE_INVERT)
            }
            if (regulateList.contains(CanvasRegulatePopupConfig.KEY_CONTRAST)) {
                renderThresholdItem(CanvasRegulatePopupConfig.KEY_CONTRAST)
            }
            if (regulateList.contains(CanvasRegulatePopupConfig.KEY_BRIGHTNESS)) {
                renderThresholdItem(CanvasRegulatePopupConfig.KEY_BRIGHTNESS)
            }

        }

        if (firstApply) {
            //首次触发
            checkValueChangedRunnable()
        }
    }

    /**渲染阈值*/
    fun DslAdapter.renderThresholdItem(key: String, defValue: Float = 140f) {
        when (key) {
            CanvasRegulatePopupConfig.KEY_CONTRAST -> CanvasSeekBarItem()() {
                itemInfoText = _string(R.string.canvas_contrast) //-1~1   0-255
                initItem()

                itemProgressTextFormatAction = {
                    (-1f + 2 * it._progressFraction).canvasDecimal(1)
                }

                val def = getFloatOrDef(key, 0f)
                itemSeekProgress = (((def + 1) / 2f) * 100).toInt()
                property[key] = def

                itemSeekTouchEnd = { value, fraction ->
                    property[key] = -1f + 2 * fraction
                }
            }
            CanvasRegulatePopupConfig.KEY_BRIGHTNESS -> CanvasSeekBarItem()() {
                itemInfoText = _string(R.string.canvas_brightness) //-1~1   0-255
                initItem()

                itemProgressTextFormatAction = {
                    (-1f + 2 * it._progressFraction).canvasDecimal(1)
                }

                val def = getFloatOrDef(key, 0f)
                itemSeekProgress = (((def + 1) / 2f) * 100).toInt()
                property[key] = def

                itemSeekTouchEnd = { value, fraction ->
                    property[key] = -1f + 2 * fraction
                }
            }
            else -> CanvasSeekBarItem()() {
                itemInfoText = _string(R.string.canvas_threshold) //0-255
                initItem()

                itemProgressTextFormatAction = {
                    "${(255f * it._progressFraction).toInt()}"
                }

                val def = getFloatOrDef(key, defValue)
                itemSeekProgress = ((def / 255f) * 100).toInt()
                property[key] = def

                itemSeekTouchEnd = { value, fraction ->
                    property[key] = 255f * fraction
                }
            }
        }
    }

    /**渲染反色*/
    fun DslAdapter.renderInvertItem(key: String, defValue: Boolean = false) {
        CanvasSwitchItem()() {
            itemInfoText = _string(R.string.canvas_invert)
            initItem()
            property[key] = getBooleanOrDef(key, defValue)

            itemSwitchChangedAction = {
                property[key] = it
            }
        }
    }

    fun DslAdapterItem.initItem() {
        if (this is DslSeekBarInfoItem) {
            itemShowProgressText = true
        }
        if (realTimeApply) {
            itemChangeListener = {
                //实时预览
                checkValueChangedRunnable()
            }
        }
    }

    //抖动处理
    val valueChangedRunnable = Runnable {
        onApplyAction(false)
    }

    var shakeDelay: Long = 600L
    val handler = Handler(Looper.getMainLooper())

    /**检查是否需要实时预览*/
    fun checkValueChangedRunnable() {
        handler.removeCallbacks(valueChangedRunnable)
        handler.postDelayed(valueChangedRunnable, shakeDelay)
    }

    fun addRegulate(key: String) {
        regulateList.add(key)
        if (key == CanvasRegulatePopupConfig.KEY_SUBMIT) {
            realTimeApply = false
            firstApply = false
        }
    }

    fun getIntOrDef(key: String, def: Int): Int {
        return property[key]?.toString()?.toFloatOrNull()?.toInt() ?: def
    }

    fun getBooleanOrDef(key: String, def: Boolean): Boolean {
        val value = property[key]
        if (value is Boolean) {
            return value
        }
        return def
    }

    fun getFloatOrDef(key: String, def: Float): Float {
        return property[key]?.toString()?.toFloatOrNull() ?: def
    }
}

/**Dsl
 * 画布图片编辑属性弹窗*/
@DSL
fun Context.canvasRegulateWindow2(
    anchor: View?,
    config: CanvasRegulatePopupConfig2.() -> Unit
): TargetWindow {
    val popupConfig = CanvasRegulatePopupConfig2()
    popupConfig.anchor = anchor
    //popupConfig.addRegulate()
    popupConfig.config()
    return popupConfig.show(this)
}