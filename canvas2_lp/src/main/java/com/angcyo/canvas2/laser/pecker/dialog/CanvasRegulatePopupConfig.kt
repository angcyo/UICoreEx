package com.angcyo.canvas2.laser.pecker.dialog

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.canvas.render.util.canvasDecimal
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dslitem.*
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.EngraveSegmentScrollItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.engrave.appendDrawable
import com.angcyo.dialog.TargetWindow
import com.angcyo.dialog.dismissWindow
import com.angcyo.dialog.popup.MenuPopupConfig
import com.angcyo.dsladapter.DslAdapter
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.enableItemTags
import com.angcyo.item.DslBlackButtonItem
import com.angcyo.item.DslSeekBarInfoItem
import com.angcyo.item.style.*
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.laserpacker.bean.MeshShapeInfo
import com.angcyo.library._screenHeight
import com.angcyo.library._screenWidth
import com.angcyo.library.annotation.DSL
import com.angcyo.library.annotation.MM
import com.angcyo.library.component.hawk.LibHawkKeys
import com.angcyo.library.component.pad.isInPadMode
import com.angcyo.library.ex._string
import com.angcyo.library.ex.dpi
import com.angcyo.library.ex.toStr
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.recycler.DslRecyclerView
import com.angcyo.widget.span.span

/**
 * 属性调整弹窗
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/09
 */
class CanvasRegulatePopupConfig : MenuPopupConfig() {

    companion object {

        /**窗口销毁触发*/
        const val APPLY_TYPE_DISMISS = 0

        /**点击提交按钮*/
        const val APPLY_TYPE_SUBMIT = 1

        /**值改变*/
        const val APPLY_TYPE_CHANGE = 2

        //属性key

        /**版画阈值*/
        const val KEY_PRINT_THRESHOLD = "key_print_threshold"

        /**黑白反色*/
        const val KEY_BW_INVERT = "key_bw_invert"

        /**黑白画阈值*/
        const val KEY_BW_THRESHOLD = "key_bw_threshold"

        /**印章阈值*/
        const val KEY_SEAL_THRESHOLD = "key_seal_threshold"

        /**切片数量*/
        const val KEY_SLICE = "key_slice"

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

        /**图片偏移量, 正负1英寸*/
        @MM
        const val KEY_OUTLINE_OFFSET = "key_outline_offset"

        /**图片偏移是否保留空洞*/
        const val KEY_OUTLINE_HOLE = "key_outline_hole"

        /**曲线文本曲率*/
        const val KEY_CURVATURE = "key_curvature"
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

    /**相同类型的回调
     * [onApplyAction]
     * [onSubmitAction]
     *
     * [APPLY_TYPE_DISMISS] submit后不会触发dismiss类型
     * [APPLY_TYPE_SUBMIT]
     * [APPLY_TYPE_CHANGE]
     * */
    var onApplyValueAction: (type: Int) -> Unit = {}

    /**[dismiss] 和 [submit] 互斥触发
     * [dismiss] 是否销毁了弹窗
     * [submit] 是否点击了提交按钮*/
    var onSubmitAction: (dismiss: Boolean, submit: Boolean) -> Unit = { _, _ -> }

    /**值是否发生过改变*/
    var _valueChange: Boolean = false

    /**是否是点击按钮触发的销毁*/
    private var _dismissFromSubmit: Boolean = false

    init {
        minHorizontalOffset = 20 * dpi
        onDismiss = {
            if (!_dismissFromSubmit) {
                onApplyAction(true)
                onApplyValueAction(APPLY_TYPE_DISMISS)
            }
            onSubmitAction(true, false)
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
                    //itemSwitchChecked = def

                    itemSwitchChangedAction = {
                        property[KEY_OUTLINE] = it
                        enableItemTags(!it, listOf(KEY_LINE_SPACE, KEY_ANGLE, KEY_DIRECTION))
                    }

                    viewHolder.post {
                        updateSwitchChecked(def, true)
                    }
                }
            }

            if (regulateList.contains(KEY_LINE_SPACE)) {
                CanvasSeekBarItem()() {
                    itemTag = KEY_LINE_SPACE
                    itemInfoText = _string(R.string.canvas_line_space) //0.125-5
                    initItem()

                    val start = 0.1f //0.125f
                    val max = 5f
                    val def = getFloatOrDef(KEY_LINE_SPACE, LPDataConstant.DEFAULT_LINE_SPACE)

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
                    itemTag = KEY_ANGLE
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
                    itemTag = KEY_DIRECTION
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
                    LPDataConstant.DEFAULT_LINE_SPACE
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

            //轮廓
            if (regulateList.contains(KEY_OUTLINE_OFFSET)) {
                //偏移距离
                CanvasOutlineOffsetItem()() {
                    initItem()
                    itemShowProgressText = false
                    itemValue = getFloatOrDef(KEY_OUTLINE_OFFSET, 0f)
                    property[KEY_OUTLINE_OFFSET] = itemValue

                    itemValueChangeAction = {
                        property[KEY_OUTLINE_OFFSET] = it

                        _valueChange = true
                        if (realTimeApply) {
                            //实时预览
                            checkValueChangedRunnable()
                        }
                    }
                }
            }
            if (regulateList.contains(KEY_OUTLINE_HOLE)) {
                //保留空洞
                CanvasSwitchItem()() {
                    itemInfoText = _string(R.string.canvas_outline_hole)
                    initItem()

                    val def = getBooleanOrDef(KEY_OUTLINE_HOLE, true)
                    property[KEY_OUTLINE_HOLE] = def
                    itemSwitchChecked = def

                    itemSwitchChangedAction = {
                        property[KEY_OUTLINE_HOLE] = it
                    }
                }
            }

            if (regulateList.contains(KEY_CURVATURE)) {
                //曲线
                CanvasTextCurvatureItem()() {
                    initItem()
                    itemShowProgressText = false
                    itemValue = getFloatOrDef(KEY_CURVATURE, 0f)
                    property[KEY_CURVATURE] = itemValue

                    itemValueChangeAction = {
                        property[KEY_CURVATURE] = it

                        _valueChange = true
                        if (realTimeApply) {
                            //实时预览
                            checkValueChangedRunnable()
                        }
                    }
                }
            }

            //切片数量
            if (regulateList.contains(KEY_SLICE)) {
                renderSeekBarItem(
                    KEY_SLICE,
                    _string(R.string.canvas_slice_count),
                    getIntOrDef(KEY_SLICE, 0),
                    0,
                    LibHawkKeys.grayThreshold
                )
            }

            //---last---

            //确认按钮
            if (regulateList.contains(KEY_SUBMIT)) {
                DslBlackButtonItem()() {
                    itemButtonText = _string(R.string.dialog_positive)
                    itemClick = {
                        _valueChange = true //2023-3-10 need?
                        _dismissFromSubmit = true //2023-5-15
                        onApplyAction(false)
                        onApplyAction(true)
                        onSubmitAction(false, true)
                        onApplyValueAction(APPLY_TYPE_SUBMIT)
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
                itemInfoTooltipText = _string(R.string.canvas_contrast) //-1~1   0-255
                itemInfoText = span {
                    appendDrawable(R.drawable.canvas_regulate_contrast)
                }
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
                itemInfoTooltipText = _string(R.string.canvas_brightness) //-1~1   0-255
                itemInfoText = span {
                    appendDrawable(R.drawable.canvas_regulate_brightness)
                }
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

    /**float类型 渲染滑块*/
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

    /**int类型 [renderSeekBarItem]*/
    fun DslAdapter.renderSeekBarItem(
        key: String,
        label: CharSequence?,
        defValue: Int = 0,
        minValue: Int = 0,
        maxValue: Int = 100,
        init: CanvasSeekBarItem.() -> Unit = {}
    ) {
        CanvasSeekBarItem()() { //路径填充的角度
            itemInfoText = label
            initItem()
            val sum = maxValue - minValue

            itemProgressTextFormatAction = {
                (minValue + sum * it._progressFraction).toInt().toStr()
            }

            val def = getIntOrDef(key, defValue)
            val ratio = def * 1f / sum
            itemSeekProgress = ratio * 100
            property[key] = def

            itemSeekTouchEnd = { value, fraction ->
                property[key] = (minValue + sum * fraction).toInt()
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
        onSubmitAction(false, false)
        onApplyValueAction(APPLY_TYPE_CHANGE)
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