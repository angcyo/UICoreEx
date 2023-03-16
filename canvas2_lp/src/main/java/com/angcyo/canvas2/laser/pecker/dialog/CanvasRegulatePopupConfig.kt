package com.angcyo.canvas2.laser.pecker.dialog

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import com.angcyo.canvas.render.util.canvasDecimal
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.bean.MeshShapeInfo
import com.angcyo.canvas2.laser.pecker.dslitem.*
import com.angcyo.canvas2.laser.pecker.util.LPConstant.DEFAULT_LINE_SPACE
import com.angcyo.dialog.TargetWindow
import com.angcyo.dialog.dismissWindow
import com.angcyo.dialog.popup.MenuPopupConfig
import com.angcyo.dsladapter.DslAdapter
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.data.HawkEngraveKeys
import com.angcyo.engrave.dslitem.EngraveSegmentScrollItem
import com.angcyo.item.DslBlackButtonItem
import com.angcyo.item.DslSeekBarInfoItem
import com.angcyo.item.style.*
import com.angcyo.library._screenHeight
import com.angcyo.library._screenWidth
import com.angcyo.library.annotation.DSL
import com.angcyo.library.component.pad.isInPadMode
import com.angcyo.library.ex._string
import com.angcyo.library.ex.dpi
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.recycler.DslRecyclerView

/**
 * 属性调整弹窗
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/09
 */
class CanvasRegulatePopupConfig : MenuPopupConfig() {

    companion object {

        //属性key

        /**版画阈值*/
        const val KEY_PRINT_THRESHOLD = "key_print_threshold"

        /**黑白反色*/
        const val KEY_BW_INVERT = "key_bw_invert"

        /**黑白画阈值*/
        const val KEY_BW_THRESHOLD = "key_bw_threshold"

        /**印章阈值*/
        const val KEY_SEAL_THRESHOLD = "key_seal_threshold"

        /**GCode旋转方向*/
        const val KEY_DIRECTION = "key_direction"

        /**GCode线距*/
        const val KEY_LINE_SPACE = "key_line_space"

        /**路径填充*/
        const val KEY_PATH_FILL_LINE_SPACE = "key_path_fill_line_space"

        /**路径填充角度*/
        const val KEY_PATH_FILL_ANGLE = "key_path_fill_angle"

        /**GCode线的角度*/
        const val KEY_ANGLE = "key_angle"

        /**GCode是否要轮廓*/
        const val KEY_OUTLINE = "key_outline"

        /**抖动反色*/
        const val KEY_SHAKE_INVERT = "key_shake_invert"

        /**对比度*/
        const val KEY_CONTRAST = "key_contrast"

        /**亮度*/
        const val KEY_BRIGHTNESS = "key_brightness"

        /**大直径, 小直径*/
        const val KEY_MIN_DIAMETER = "min_diameter"
        const val KEY_MAX_DIAMETER = "max_diameter"

        /**扭曲类型*/
        const val KEY_MESH_SHAPE = "mesh_shape"

        /**默认的扭曲类型
         * "CONE" 圆锥
         * "BALL" 球体
         * */
        const val MESH_SHAPE_CONE = "CONE"
        const val MESH_SHAPE_BALL = "BALL" //球体
        const val DEFAULT_MESH_SHAPE = MESH_SHAPE_CONE

        /**单独的确定按钮*/
        const val KEY_SUBMIT = "key_submit"
    }

    /**需要调整的项目, 需要啥就添加对应的项
     * [com.angcyo.canvas.laser.pecker.CanvasRegulatePopupConfig2.KEY_INVERT]
     * [com.angcyo.canvas.laser.pecker.CanvasRegulatePopupConfig2.KEY_THRESHOLD]
     * [com.angcyo.canvas.laser.pecker.CanvasRegulatePopupConfig2.KEY_LINE_SPACE]
     * [com.angcyo.canvas.laser.pecker.CanvasRegulatePopupConfig2.KEY_DIRECTION]
     * [com.angcyo.canvas.laser.pecker.CanvasRegulatePopupConfig2.KEY_ANGLE]
     * [com.angcyo.canvas.laser.pecker.CanvasRegulatePopupConfig2.KEY_CONTRAST]
     * [com.angcyo.canvas.laser.pecker.CanvasRegulatePopupConfig2.KEY_BRIGHTNESS]
     *
     * [com.angcyo.canvas.laser.pecker.CanvasRegulatePopupConfig2.KEY_MIN_DIAMETER]
     * [com.angcyo.canvas.laser.pecker.CanvasRegulatePopupConfig2.KEY_MAX_DIAMETER]
     * */
    val regulateList = mutableListOf<String>()

    /**保存修改后的属性, 用来恢复*/
    var property = hashMapOf<String, Any?>()

    /**是否实时监听改变*/
    var realTimeApply: Boolean = true

    /**首次加载是否需要应用*/
    var firstApply: Boolean = true

    /**应用属性实现方法的回调
     * [dismiss] 是否销毁了弹窗
     * 当前提交按钮[DslBlackButtonItem]时, 会先触发一次false, 然后再触发true
     * */
    var onApplyAction: (dismiss: Boolean) -> Unit = {}

    /**值是否发生过改变*/
    var _valueChange: Boolean = false

    init {
        minHorizontalOffset = 20 * dpi
        onDismiss = {
            if (!regulateList.contains(KEY_SUBMIT)) {
                //在没有确定按钮的情况下, 销毁窗口时需要apply一下
                onApplyAction(true)
            }
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
            if (regulateList.contains(KEY_PRINT_THRESHOLD)) {
                renderThresholdItem(KEY_PRINT_THRESHOLD, HawkEngraveKeys.lastPrintThreshold)
            }
            //印章阈值
            if (regulateList.contains(KEY_SEAL_THRESHOLD)) {
                renderThresholdItem(KEY_SEAL_THRESHOLD, HawkEngraveKeys.lastSealThreshold)
            }

            //GCode
            if (regulateList.contains(KEY_OUTLINE)) {
                CanvasSwitchItem()() {
                    itemInfoText = _string(R.string.canvas_outline) //轮廓
                    initItem()

                    val def = getBooleanOrDef(KEY_OUTLINE, true)
                    property[KEY_OUTLINE] = def
                    itemSwitchChecked = def

                    itemSwitchChangedAction = {
                        property[KEY_OUTLINE] = it
                    }
                }
            }

            if (regulateList.contains(KEY_LINE_SPACE)) {
                CanvasSeekBarItem()() {
                    itemInfoText = _string(R.string.canvas_line_space) //0.125-5
                    initItem()

                    val start = 0.1f //0.125f
                    val max = 5f
                    val def = getFloatOrDef(KEY_LINE_SPACE, DEFAULT_LINE_SPACE)

                    itemProgressTextFormatAction = {
                        (start + (max - start) * it._progressFraction).canvasDecimal(3)
                    }

                    property[KEY_LINE_SPACE] = def
                    itemSeekProgress = if (def == start) {
                        0f
                    } else {
                        (def / (max - start)) * 100
                    }

                    itemSeekTouchEnd = { value, fraction ->
                        property[KEY_LINE_SPACE] = start + (max - start) * fraction
                    }
                }
            }
            if (regulateList.contains(KEY_ANGLE)) {
                CanvasSeekBarItem()() {
                    itemInfoText = _string(R.string.canvas_angle) //0-90
                    initItem()

                    itemProgressTextFormatAction = {
                        (90 * it._progressFraction).canvasDecimal(1)
                    }

                    val def = getFloatOrDef(KEY_ANGLE, 0f)
                    property[KEY_ANGLE] = def
                    itemSeekProgress = (def / 90f) * 100

                    itemSeekTouchEnd = { value, fraction ->
                        property[KEY_ANGLE] = 90f * fraction
                    }
                }
            }
            //2023-3-9 废弃:因为可以手动调整方向
            if (regulateList.contains(KEY_DIRECTION)) {
                CanvasGCodeDirectionItem()() {
                    itemText = _string(R.string.canvas_direction) //0:0 1:90 2:180 3:270
                    itemDirection = getIntOrDef(KEY_DIRECTION, 0)
                    property[KEY_DIRECTION] = itemDirection
                    initItem()

                    itemSelectChangedAction = { fromIndex, toIndex, reselect, fromUser ->
                        property[KEY_DIRECTION] = toIndex
                    }
                }
            }
            //路径填充的线距
            if (regulateList.contains(KEY_PATH_FILL_LINE_SPACE)) {
                renderSeekBarItem(
                    KEY_PATH_FILL_LINE_SPACE,
                    _string(R.string.canvas_path_fill_line_space),
                    0f,
                    0f,
                    DEFAULT_LINE_SPACE
                )
            }
            //路径填充的角度
            if (regulateList.contains(KEY_PATH_FILL_ANGLE)) {
                renderSeekBarItem(
                    KEY_PATH_FILL_ANGLE,
                    _string(R.string.canvas_path_fill_angle),
                    0f,
                    0f,
                    360f
                )
            }

            //黑白画
            if (regulateList.contains(KEY_BW_INVERT)) {
                renderInvertItem(KEY_BW_INVERT)
            }
            if (regulateList.contains(KEY_BW_THRESHOLD)) {
                renderThresholdItem(KEY_BW_THRESHOLD, HawkEngraveKeys.lastBWThreshold)
            }

            //抖动
            if (regulateList.contains(KEY_SHAKE_INVERT)) {
                renderInvertItem(KEY_SHAKE_INVERT)
            }
            if (regulateList.contains(KEY_CONTRAST)) {
                renderThresholdItem(KEY_CONTRAST)
            }
            if (regulateList.contains(KEY_BRIGHTNESS)) {
                renderThresholdItem(KEY_BRIGHTNESS)
            }

            //椎体/球体
            if (regulateList.contains(KEY_MESH_SHAPE)) {
                EngraveSegmentScrollItem()() {
                    itemText = _string(R.string.canvas_rotation_axis)
                    // "CONE" 圆锥
                    // "BALL" 球体
                    val shape = getStringOrDef(KEY_MESH_SHAPE, DEFAULT_MESH_SHAPE)
                    val meshShapeInfoList =
                        listOf(MeshShapeInfo(MESH_SHAPE_CONE), MeshShapeInfo(MESH_SHAPE_BALL))
                    itemSegmentList = meshShapeInfoList
                    itemCurrentIndex = meshShapeInfoList.indexOfFirst { it.shape == shape }

                    observeItemChange {
                        property[KEY_MESH_SHAPE] = meshShapeInfoList[itemCurrentIndex].shape
                    }
                }
            }
            //大小直径
            if (regulateList.contains(KEY_MIN_DIAMETER)) {
                renderDiameterItem(KEY_MIN_DIAMETER)
            }
            if (regulateList.contains(KEY_MAX_DIAMETER)) {
                renderDiameterItem(KEY_MAX_DIAMETER)
            }

            //确认按钮
            if (regulateList.contains(KEY_SUBMIT)) {
                DslBlackButtonItem()() {
                    itemButtonText = _string(R.string.dialog_positive)
                    itemClick = {
                        _valueChange = true //2023-3-10 need?
                        onApplyAction(false)
                        onApplyAction(true)
                        window.dismissWindow()
                    }
                }
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
            KEY_CONTRAST -> CanvasSeekBarItem()() {
                itemInfoText = _string(R.string.canvas_contrast) //-1~1   0-255
                initItem()

                itemProgressTextFormatAction = {
                    (-1f + 2 * it._progressFraction).canvasDecimal(1)
                }

                val def = getFloatOrDef(key, 0f)
                itemSeekProgress = ((def + 1) / 2f) * 100
                property[key] = def

                itemSeekTouchEnd = { value, fraction ->
                    property[key] = -1f + 2 * fraction
                }
            }
            KEY_BRIGHTNESS -> CanvasSeekBarItem()() {
                itemInfoText = _string(R.string.canvas_brightness) //-1~1   0-255
                initItem()

                itemProgressTextFormatAction = {
                    (-1f + 2 * it._progressFraction).canvasDecimal(1)
                }

                val def = getFloatOrDef(key, 0f)
                itemSeekProgress = ((def + 1) / 2f) * 100
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
                itemSeekProgress = (def / 255f) * 100
                property[key] = def

                itemSeekTouchEnd = { value, fraction ->
                    val threshold = 255f * fraction
                    property[key] = threshold

                    //阈值持久化
                    when (key) {
                        KEY_PRINT_THRESHOLD -> HawkEngraveKeys.lastPrintThreshold = threshold
                        KEY_SEAL_THRESHOLD -> HawkEngraveKeys.lastSealThreshold = threshold
                        KEY_BW_THRESHOLD -> HawkEngraveKeys.lastBWThreshold = threshold
                    }
                }
            }
        }
    }

    /**渲染滑块*/
    fun DslAdapter.renderSeekBarItem(
        key: String,
        label: CharSequence?,
        defValue: Float = 0f,
        minValue: Float = 0f,
        maxValue: Float = 100f,
        init: CanvasSeekBarItem.() -> Unit = {}
    ) {
        CanvasSeekBarItem()() { //路径填充的角度
            itemInfoText = label
            initItem()
            val sum = maxValue - minValue

            itemProgressTextFormatAction = {
                (minValue + sum * it._progressFraction).canvasDecimal(1)
            }

            val def = getFloatOrDef(key, defValue)
            val ratio = def / sum
            itemSeekProgress = ratio * 100
            property[key] = def

            itemSeekTouchEnd = { value, fraction ->
                property[key] = minValue + sum * fraction
            }

            init()
        }
    }

    /**渲染反色*/
    fun DslAdapter.renderInvertItem(key: String, defValue: Boolean = false) {
        CanvasInvertSwitchItem()() {
            itemInfoText = _string(R.string.canvas_invert)
            initItem()

            val def = getBooleanOrDef(key, defValue)
            property[key] = def
            itemSwitchChecked = def

            itemSwitchChangedAction = {
                property[key] = it
            }
        }
    }

    /**渲染直径item*/
    fun DslAdapter.renderDiameterItem(key: String) {
        CanvasDiameterItem()() {
            //itemInfoText = _string(R.string.canvas_invert)
            itemDiameterLabel = if (key == KEY_MIN_DIAMETER) {
                _string(R.string.canvas_min_diameter)
            } else {
                _string(R.string.canvas_max_diameter)
            }
            itemDiameter = if (key == KEY_MIN_DIAMETER) {
                getFloatOrDef(
                    KEY_MIN_DIAMETER,
                    HawkEngraveKeys.lastMinDiameterPixel
                )
            } else {
                getFloatOrDef(
                    KEY_MAX_DIAMETER,
                    HawkEngraveKeys.lastDiameterPixel
                )
            }
            initItem()
            property[key] = itemDiameter

            observeItemChange {
                property[key] = itemDiameter

                if (key == KEY_MIN_DIAMETER) {
                    HawkEngraveKeys.lastMinDiameterPixel = itemDiameter
                } else {
                    HawkEngraveKeys.lastDiameterPixel = itemDiameter
                }
            }
        }
    }

    fun DslAdapterItem.initItem() {
        if (this is DslSeekBarInfoItem) {
            itemShowProgressText = true
        }
        itemChangeListener = {
            _valueChange = true
            if (realTimeApply) {
                //实时预览
                checkValueChangedRunnable()
            }
        }
    }

    //抖动处理
    val valueChangedRunnable = Runnable {
        _valueChange = true
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
        if (key == KEY_SUBMIT) {
            realTimeApply = false
            firstApply = false
        }
    }

    /**添加一个属性, 并且设置对应的属性默认值*/
    fun addRegulate(key: String, value: Any?) {
        addRegulate(key)
        setProperty(key, value)
    }

    /**设置属性*/
    fun setProperty(key: String, value: Any?) {
        property[key] = value
    }

    fun getIntOrDef(key: String, def: Int): Int {
        return property[key]?.toString()?.toFloatOrNull()?.toInt() ?: def
    }

    fun getStringOrDef(key: String, def: String): String {
        return property[key]?.toString() ?: def
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
fun Context.canvasRegulateWindow(
    anchor: View?,
    config: CanvasRegulatePopupConfig.() -> Unit
): TargetWindow {
    val popupConfig = CanvasRegulatePopupConfig()
    popupConfig.anchor = anchor
    if (isInPadMode()) {
        popupConfig.width = Integer.min(_screenWidth, _screenHeight)
    }
    //popupConfig.addRegulate()
    popupConfig.config()
    return popupConfig.show(this)
}