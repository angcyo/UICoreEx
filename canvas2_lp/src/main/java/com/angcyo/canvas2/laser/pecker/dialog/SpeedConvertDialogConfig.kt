package com.angcyo.canvas2.laser.pecker.dialog

import android.app.Dialog
import android.content.Context
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker._deviceConfigBean
import com.angcyo.bluetooth.fsc.laserpacker.command.EngraveCmd.Companion.speedToDepth
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dialog.dslitem.LayerSegmentItem
import com.angcyo.canvas2.laser.pecker.dialog.dslitem.SpeedConvertItem
import com.angcyo.canvas2.laser.pecker.dialog.dslitem.SpeedConvertTestItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.transfer.TransferDataPxItem
import com.angcyo.dialog.BaseRecyclerDialogConfig
import com.angcyo.dialog.configBottomDialog
import com.angcyo.item.DslTextItem
import com.angcyo.item.style.itemCurrentIndex
import com.angcyo.item.style.itemTabEquWidthCountRange
import com.angcyo.item.style.itemText
import com.angcyo.laserpacker.device.LayerHelper
import com.angcyo.library.annotation.DSL
import com.angcyo.library.ex._color
import com.angcyo.library.ex._string
import com.angcyo.library.ex.abs
import com.angcyo.library.ex.ceilInt
import com.angcyo.library.ex.decimal
import com.angcyo.library.ex.floorInt
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.span.span
import kotlin.math.absoluteValue


/**
 * LP4 654/655速度转换对话框配置
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/12/18
 */
class SpeedConvertDialogConfig : BaseRecyclerDialogConfig() {

    /**是否要指定转换类型, 指定之后, 就不能互相转换了
     * 通常在雕刻属性设置界面需要指定*/
    var appointConvertType: Int? = null
        set(value) {
            field = value
            if (value != null) {
                convertType = value
            }
        }

    var selectLayerId: String? = null
    var selectDpi = 254.0f

    private var convertType: Int = TYPE_TO_NEW

    private var _depth = HawkEngraveKeys.lastDepth

    /**转换后的深度返回*/
    var onDepthResultAction: (Int) -> Unit = {}

    companion object {
        const val TYPE_TO_NEW = 1
        const val TYPE_TO_OLD = 2
    }

    init {
        dialogTitle = _string(R.string.speed_convert_calculate, "LP4")
        dialogTitleLayoutId = R.layout.lib_dialog_base_ico_title_layout
        dialogMaxHeight = "0.8sh"

        positiveButton { dialog, dialogViewHolder ->
            onDepthResultAction(_depth)
            dialog.dismiss()
        }

        onRenderAdapterAction = {
            if (appointConvertType == null) {
                val layerList = LayerHelper.getEngraveLayerList().filter {
                    it.layerId == LaserPeckerHelper.LAYER_FILL || it.layerId == LaserPeckerHelper.LAYER_LINE
                }
                selectLayerId =
                    selectLayerId ?: layerList.firstOrNull()?.layerId
                            ?: LaserPeckerHelper.LAYER_LINE
                LayerSegmentItem()() {
                    itemIncludeCutLayer = true
                    itemSegmentList = layerList
                    itemCurrentIndex = layerList.indexOfFirst { it.layerId == selectLayerId }
                    observeItemChange {
                        selectLayerId = currentLayerInfo().layerId
                        refreshDslAdapter()
                    }
                    itemTabEquWidthCountRange = ""
                }

                TransferDataPxItem()() {
                    itemPxList = LaserPeckerHelper.findProductLayerSupportPxList(selectLayerId!!)
                    selectorCurrentDpi(selectDpi)
                    itemHidden = itemPxList.isNullOrEmpty() //自动隐藏
                    observeItemChange {
                        //保存最后一次选择的dpi
                        val dpi =
                            itemPxList?.get(itemCurrentIndex)?.dpi ?: LaserPeckerHelper.DPI_254
                        selectDpi = dpi
                        refreshDslAdapter()
                    }
                }

                val oldSpeedList = SpeedInfo.getOldSpeedList(selectLayerId!!, selectDpi)
                val newSpeedList = SpeedInfo.getNewSpeedList(selectDpi)

                //旧值转成新的值
                var speedInfo = oldSpeedList.find { it.depth == HawkEngraveKeys.lastDepth }
                var newSpeedInfo = SpeedInfo.findNearestSpeed(newSpeedList, speedInfo?.speed)
                _depth = newSpeedInfo?.depth ?: _depth

                if (convertType == TYPE_TO_OLD) {
                    //新值转成旧的值
                    newSpeedInfo = newSpeedList.find { it.depth == HawkEngraveKeys.lastDepth }
                    speedInfo = SpeedInfo.findNearestSpeed(oldSpeedList, newSpeedInfo?.speed)
                    _depth = speedInfo?.depth ?: _depth
                }

                DslTextItem()() {
                    /*itemText = "${_string(R.string.firmware_version)} <655 ↓"
                    configTextStyle {
                        boldStyle()
                    }*/
                    itemText = _string(R.string.old_firmware_version_label)
                    configTextStyle {
                        textBold = true
                    }
                }

                SpeedConvertTestItem()() {
                    itemSpeedInfo = speedInfo
                    observeItemChange {
                        convertType = TYPE_TO_NEW
                        refreshDslAdapter()
                    }
                }

                DslTextItem()() {
                    /*itemText = "${_string(R.string.firmware_version)} >=655 ↓"
                    */
                    itemText = _string(R.string.new_firmware_version_label)
                    configTextStyle {
                        textBold = true
                    }
                }

                if (appointConvertType == null) {
                    SpeedConvertTestItem()() {
                        itemSpeedInfo = newSpeedInfo
                        observeItemChange {
                            convertType = TYPE_TO_OLD
                            refreshDslAdapter()
                        }
                    }
                } else {
                    DslTextItem()() {
                        itemText = span {
                            append("≈ ")
                            append(_string(R.string.custom_speed))
                            append(":")
                            append("${newSpeedInfo?.depth}") {
                                foregroundColor = _color(R.color.colorAccent)
                            }

                            append(" ")
                            append(_string(R.string.engrave_speed))
                            append(": ")
                            append("${newSpeedInfo?.speed?.decimal(fadedUp = true) ?: "--"}mm/s") {
                                foregroundColor = _color(R.color.colorAccent)
                            }
                            appendLine()
                        }
                    }
                }
            } else {
                SpeedConvertItem()() {
                    itemLayerId = selectLayerId
                    itemDpi = selectDpi
                    itemDepth = HawkEngraveKeys.lastDepth
                    onItemDepthChanged = {
                        _depth = it
                    }
                }
            }
        }
    }

    override fun initDialogView(dialog: Dialog, dialogViewHolder: DslViewHolder) {
        super.initDialogView(dialog, dialogViewHolder)
    }
}

@DSL
fun Context.speedConvertDialogConfig(config: SpeedConvertDialogConfig.() -> Unit = {}) {
    return SpeedConvertDialogConfig().run {
        dialogContext = this@speedConvertDialogConfig
        configBottomDialog(this@speedConvertDialogConfig)
        canceledOnTouchOutside = true
        config()
        show()
    }
}

data class SpeedInfo(
    /**对应的图层*/
    var layerId: String = LaserPeckerHelper.LAYER_FILL,
    /**速度 mm/s*/
    var speed: Float = 0f,
    /**雕刻深度*/
    var depth: Int = 0,
    /**dpi*/
    var dpi: Float = 254f,
) {

    companion object {
        /**旧的速度映射表, <655固件 */
        fun getOldSpeedList(layerId: String, dpi: Float): List<SpeedInfo> {
            val result = mutableListOf<SpeedInfo>()

            //常量
            val c = 1000000

            //最大周期
            val pMax = 1250
            //最小周期
            val pMin = 12.5f

            //定时器时钟 MHz
            val timing = 72

            //定时器分频
            val frequency = if (layerId == LaserPeckerHelper.LAYER_FILL) 360 else 144

            //步进
            val step = if (layerId == LaserPeckerHelper.LAYER_FILL) 0.1f else 0.01f

            for (speedLevel in 1..100) {
                //定时器周期, 向下取整
                val timingCycle = (pMin + (pMax - pMin) / 99f * (100 - speedLevel)).floorInt()
                //定时时间
                val time = (timingCycle + 1f) * (frequency + 1) / timing

                //这是1k 254的速度
                val speed1K = step / time * c
                val speed = speed1K * 254 / dpi
                result.add(SpeedInfo(layerId, speed, speedToDepth(speedLevel), dpi))
            }
            return result
        }

        /**新的速度映射表, >=655固件
         * 没有图层区分*/
        fun getNewSpeedList(dpi: Float): List<SpeedInfo> {
            val result = mutableListOf<SpeedInfo>()
            for (speedLevel in 1..100) {
                //这是1k 254的速度
                val speed1K =
                    ((4000 - 4) * 1f / ((100 - 1) * (100 - 1)) * (speedLevel - 1) * (speedLevel - 1) + 4).ceilInt()
                val speed = speed1K * 254 / dpi
                result.add(SpeedInfo("", speed, speedToDepth(speedLevel), dpi))
            }
            return result
        }

        /**查找速度最接近的一个信息*/
        fun findNearestSpeed(list: List<SpeedInfo>, refSpeed: Float?): SpeedInfo? {
            refSpeed ?: return null
            var lastDV = Float.MAX_VALUE
            var lastInfo: SpeedInfo? = null
            for (info in list) {
                val dv = (info.speed - refSpeed).abs()
                if (lastInfo == null) {
                    lastInfo = info
                    lastDV = dv
                    continue
                }
                if (dv < lastDV) {
                    lastInfo = info
                    lastDV = dv
                }
            }
            return lastInfo
        }


        /**获取速度曲线值
         * [input] 输入的值[0~1] 输出对应修正后的插值 */
        fun getVelocityCurve(input: Double): Double? {
            val velocityCurve = _deviceConfigBean?.velocityCurve
            if (velocityCurve.isNullOrEmpty()) {
                return null
            }
            val split = velocityCurve.split(",")
            val a = split.getOrNull(0)?.toDoubleOrNull() ?: 0.0
            val b = split.getOrNull(1)?.toDoubleOrNull() ?: 0.0
            val c = split.getOrNull(2)?.toDoubleOrNull() ?: 0.0
            val d = split.getOrNull(3)?.toDoubleOrNull() ?: 0.0
            val cubicErrorBound = split.getOrNull(5)?.toDoubleOrNull() ?: 0.001

            fun evaluateCubic(a: Double, b: Double, m: Double): Double {
                return 3 * a * (1 - m) * (1 - m) * m + 3 * b * (1 - m) * m * m + m * m * m
            }

            var start = 0.0
            var end = 1.0
            while (true) {
                val midpoint = (start + end) / 2
                val estimate: Double = evaluateCubic(a, c, midpoint)
                if ((input - estimate).absoluteValue < cubicErrorBound) {
                    return evaluateCubic(b, d, midpoint)
                }
                if (estimate < input) {
                    start = midpoint
                } else {
                    end = midpoint
                }
            }
        }

        /**缓存*/
        val _lp5VelocityMap = mutableMapOf<Float, List<SpeedInfo>>()

        /**获取lp5速度列表*/
        fun getLp5VelocityList(dpi: Float): List<SpeedInfo>? {
            val cache = _lp5VelocityMap[dpi]
            if (!cache.isNullOrEmpty()) {
                return cache
            }
            val result = mutableListOf<SpeedInfo>()
            for (speedLevel in 1..100) {
                //这是1k 254的速度
                val velocityCurve = getVelocityCurve(speedLevel / 100.0) ?: return null
                val speed1K = velocityCurve * (_deviceConfigBean?.supportMaxSpeed ?: 15000)
                val speed = speed1K * 254 / dpi
                result.add(SpeedInfo("", speed.toFloat(), speedToDepth(speedLevel), dpi))
            }
            _lp5VelocityMap[dpi] = result
            return result
        }
    }

}