package com.angcyo.canvas.laser.pecker

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.PopupWindow
import com.angcyo.canvas.items.renderer.IItemRenderer
import com.angcyo.canvas.laser.pecker.dslitem.CanvasDirectionItem
import com.angcyo.canvas.laser.pecker.dslitem.CanvasSeekBarItem
import com.angcyo.canvas.laser.pecker.dslitem.CanvasSwitchItem
import com.angcyo.canvas.utils.canvasDecimal
import com.angcyo.dialog.TargetWindow
import com.angcyo.dialog.popup.ShadowAnchorPopupConfig
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.drawBottom
import com.angcyo.item.DslSeekBarInfoItem
import com.angcyo.item.style.itemInfoText
import com.angcyo.item.style.itemSwitchChangedAction
import com.angcyo.item.style.itemText
import com.angcyo.library.ex._dimen
import com.angcyo.library.ex._string
import com.angcyo.library.ex.dpi
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.recycler.renderDslAdapter

/**
 * 画布图片/GCode属性调节弹窗
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/16
 */
class CanvasRegulatePopupConfig : ShadowAnchorPopupConfig() {

    companion object {
        const val REGULATE_INVERT = 1
        const val REGULATE_THRESHOLD = 2
        const val REGULATE_LINE_SPACE = 3
        const val REGULATE_DIRECTION = 4
        const val REGULATE_ANGLE = 5

        //对比度
        const val REGULATE_CONTRAST = 6

        //亮度
        const val REGULATE_BRIGHTNESS = 7

        //属性key
        const val KEY_INVERT = "key_invert"
        const val KEY_THRESHOLD = "key_threshold"
        const val KEY_LINE_SPACE = "key_line_space"
        const val KEY_DIRECTION = "key_direction"
        const val KEY_ANGLE = "key_angle"
        const val KEY_CONTRAST = "key_contrast"
        const val KEY_BRIGHTNESS = "key_brightness"

        //缓存
        val keepProperty = hashMapOf<String, Any?>()
    }

    /**操作的渲染对象*/
    var itemRenderer: IItemRenderer<*>? = null

    /**需要调整的项目, 需要啥就添加对应的项*/
    val regulateList = mutableListOf<Int>()

    /**保存修改后的属性*/
    var property = keepProperty // hashMapOf<String, Any?>()

    /**实时预览*/
    var livePreview: Boolean = true

    /**应用属性实现方法的回调
     * [preview] 是否来自预览
     * [cancel] 是否需要取消
     * [valueChanged] 相对于上一次预览时, 值是否有改变*/
    var onApplyAction: (preview: Boolean, cancel: Boolean, valueChanged: Boolean) -> Unit =
        { preview, cancel, valueChanged ->

        }

    /**窗口销毁*/
    var onPopupDismissAction: () -> Unit = {}

    //抖动处理
    val livePreviewRunnable = Runnable {
        onApplyAction(true, false, true)
        _itemValueChanged = false //清除状态
    }

    var shakeDelay: Long = 160L

    val handler = Handler(Looper.getMainLooper())

    init {
        contentLayoutId = R.layout.canvas_regulate_layout
        triangleMinMargin = 24 * dpi
        yoff = -10 * dpi

        //取消
        onDismiss = {
            onApplyAction(false, true, false)
            onPopupDismissAction()
            false
        }
    }

    override fun initContentLayout(window: TargetWindow, viewHolder: DslViewHolder) {
        super.initContentLayout(window, viewHolder)
        viewHolder.rv(R.id.lib_recycler_view)?.renderDslAdapter {
            if (regulateList.contains(REGULATE_INVERT)) {
                CanvasSwitchItem()() {
                    itemInfoText = _string(R.string.canvas_invert)
                    initItem()
                    property[KEY_INVERT] = false

                    itemSwitchChangedAction = {
                        property[KEY_INVERT] = it
                    }
                }
            }
            if (regulateList.contains(REGULATE_THRESHOLD)) {
                CanvasSeekBarItem()() {
                    itemInfoText = _string(R.string.canvas_threshold) //0-255
                    initItem()

                    itemProgressTextFormatAction = {
                        "${(255f * it._progressFraction).toInt()}"
                    }

                    val def = getFloatOrDef(KEY_THRESHOLD, 140f)
                    itemSeekProgress = ((def / 255f) * 100).toInt()
                    property[KEY_THRESHOLD] = def

                    itemSeekTouchEnd = { value, fraction ->
                        property[KEY_THRESHOLD] = 255f * fraction
                    }
                }
            }
            if (regulateList.contains(REGULATE_CONTRAST)) {
                CanvasSeekBarItem()() {
                    itemInfoText = _string(R.string.canvas_contrast) //-1~1   0-255
                    initItem()

                    itemProgressTextFormatAction = {
                        (-1f + 2 * it._progressFraction).canvasDecimal(1)
                    }

                    val def = getFloatOrDef(KEY_CONTRAST, 0f)
                    itemSeekProgress = (((def + 1) / 2f) * 100).toInt()
                    property[KEY_CONTRAST] = def

                    itemSeekTouchEnd = { value, fraction ->
                        property[KEY_CONTRAST] = -1f + 2 * fraction
                    }
                }
            }
            if (regulateList.contains(REGULATE_BRIGHTNESS)) {
                CanvasSeekBarItem()() {
                    itemInfoText = _string(R.string.canvas_brightness) //-1~1   0-255
                    initItem()

                    itemProgressTextFormatAction = {
                        (-1f + 2 * it._progressFraction).canvasDecimal(1)
                    }

                    val def = getFloatOrDef(KEY_BRIGHTNESS, 0f)
                    itemSeekProgress = (((def + 1) / 2f) * 100).toInt()
                    property[KEY_BRIGHTNESS] = def

                    itemSeekTouchEnd = { value, fraction ->
                        property[KEY_BRIGHTNESS] = -1f + 2 * fraction
                    }
                }
            }
            if (regulateList.contains(REGULATE_LINE_SPACE)) {
                CanvasSeekBarItem()() {
                    itemInfoText = _string(R.string.canvas_line_space) //0.125-5
                    initItem()

                    val start = 0.125f
                    val max = 5f
                    val def = getFloatOrDef(KEY_LINE_SPACE, max)

                    itemProgressTextFormatAction = {
                        (start + (5 - start) * it._progressFraction).canvasDecimal(3)
                    }

                    property[KEY_LINE_SPACE] = def
                    itemSeekProgress = if (def == start) {
                        0
                    } else {
                        ((def / (5 - start)) * 100).toInt()
                    }

                    itemSeekTouchEnd = { value, fraction ->
                        property[KEY_LINE_SPACE] = start + (5 - start) * fraction
                    }
                }
            }
            if (regulateList.contains(REGULATE_ANGLE)) {
                CanvasSeekBarItem()() {
                    itemInfoText = _string(R.string.canvas_angle) //0-90
                    initItem()

                    itemProgressTextFormatAction = {
                        (90 * it._progressFraction).canvasDecimal(1)
                    }

                    val def = getFloatOrDef(KEY_ANGLE, 0f)
                    property[KEY_ANGLE] = def
                    itemSeekProgress = ((def / 90f) * 100).toInt()

                    itemSeekTouchEnd = { value, fraction ->
                        property[KEY_ANGLE] = 90f * fraction
                    }
                }
            }
            if (regulateList.contains(REGULATE_DIRECTION)) {
                CanvasDirectionItem()() {
                    itemText = _string(R.string.canvas_direction) //0:0 1:90 2:180 3:270
                    itemDirection = getIntOrDef(KEY_DIRECTION, 0)
                    property[KEY_DIRECTION] = itemDirection
                    initItem()

                    itemSelectChangedAction = { fromIndex, selectIndexList, reselect, fromUser ->
                        property[KEY_DIRECTION] = selectIndexList.first() * 1f
                    }
                }
            }
        }

        //预览
        viewHolder.click(R.id.preview_view) {
            if (_itemValueChanged) {
                onApplyAction(true, false, true)
                _itemValueChanged = false //清除状态
            }
        }
        viewHolder.gone(R.id.preview_view, livePreview)
        //确定
        viewHolder.click(R.id.confirm_view) {
            onApplyAction(false, false, _itemValueChanged)
            _itemValueChanged = false
            onDismiss = { false } //重置
            onPopupDismissAction()
            if (window is PopupWindow) {
                window.dismiss()
            }
        }
        checkLivePreviewRunnable()
    }

    /**检查是否需要实时预览*/
    fun checkLivePreviewRunnable() {
        if (livePreview) {
            handler.removeCallbacks(livePreviewRunnable)
            handler.postDelayed(livePreviewRunnable, shakeDelay)
        }
    }

    /**是否有值改变了*/
    var _itemValueChanged: Boolean = true

    fun DslAdapterItem.initItem() {
        drawBottom(_dimen(R.dimen.lib_line_px), 0, 0)
        if (this is DslSeekBarInfoItem) {
            itemShowProgressText = true
        }
        itemChangeListener = {
            //拦截改成事件, 并且不需要更新界面
            _itemValueChanged = true

            //实时预览
            checkLivePreviewRunnable()
        }
    }

    fun addRegulate(type: Int) {
        regulateList.add(type)
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
fun Context.canvasRegulateWindow(anchor: View?, config: CanvasRegulatePopupConfig.() -> Unit): Any {
    val popupConfig = CanvasRegulatePopupConfig()
    popupConfig.anchor = anchor
    popupConfig.config()
    return popupConfig.show(this)
}