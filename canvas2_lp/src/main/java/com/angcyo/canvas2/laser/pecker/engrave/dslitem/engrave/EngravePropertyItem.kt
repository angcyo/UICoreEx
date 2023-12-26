package com.angcyo.canvas2.laser.pecker.engrave.dslitem.engrave

import android.graphics.Typeface
import android.view.View
import androidx.core.view.postDelayed
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker._deviceConfigBean
import com.angcyo.bluetooth.fsc.laserpacker.bean._showRefVelocity
import com.angcyo.bluetooth.fsc.laserpacker.bean._showSpeedConvertRange
import com.angcyo.bluetooth.fsc.laserpacker.bean.matchesProductVersion
import com.angcyo.bluetooth.fsc.laserpacker.command.EngraveCmd
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dialog.SpeedConvertDialogConfig
import com.angcyo.canvas2.laser.pecker.dialog.SpeedInfo
import com.angcyo.canvas2.laser.pecker.dialog.speedConvertDialogConfig
import com.angcyo.canvas2.laser.pecker.util.LPConstant
import com.angcyo.canvas2.laser.pecker.util.mmToRenderUnitValue
import com.angcyo.core.component.model.NightModel
import com.angcyo.core.vmApp
import com.angcyo.dialog.popup.popupTipWindow
import com.angcyo.dialog2.WheelDialogConfig
import com.angcyo.dialog2.wheelDialog
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.updateAdapterItem
import com.angcyo.laserpacker.bean.LPElementBean
import com.angcyo.laserpacker.device.EngraveHelper
import com.angcyo.library.ex.ViewAction
import com.angcyo.library.ex._color
import com.angcyo.library.ex._drawable
import com.angcyo.library.ex._string
import com.angcyo.library.ex.calcIncrementValue
import com.angcyo.library.ex.decimal
import com.angcyo.library.ex.dpi
import com.angcyo.library.ex.hawkGetString
import com.angcyo.library.ex.hawkPut
import com.angcyo.library.ex.toStr
import com.angcyo.library.getAppVersionCode
import com.angcyo.library.unit.InchRenderUnit
import com.angcyo.objectbox.laser.pecker.entity.EngraveConfigEntity
import com.angcyo.objectbox.laser.pecker.lpSaveEntity
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.span.span
import kotlin.math.roundToInt

/**
 * 雕刻属性item, 包含功率/深度/次数
 *
 * [com.angcyo.engrave.dslitem.engrave.EngraveOptionWheelItem]
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/10/15
 */
class EngravePropertyItem : DslAdapterItem() {

    companion object {

        /**获取深度对应的参考速度*/
        fun getReferenceVelocity(layoutId: String?, depth: Int, dpi: Float): String? {
            val laserPeckerModel = vmApp<LaserPeckerModel>()
            val digit = if (LPConstant.renderUnit is InchRenderUnit) {
                //英制单位下用3位小数
                3
            } else {
                2
            }
            if (laserPeckerModel.isC1()) {
                val unit = "${LPConstant.renderUnit.getUnit()}/s"
                if (layoutId == LaserPeckerHelper.LAYER_LINE || layoutId == LaserPeckerHelper.LAYER_CUT) {
                    val value = (if (depth <= 40) {
                        calcIncrementValue(depth, 1, 80f, -1.5f)
                    } else if (depth <= 60) {
                        calcIncrementValue(depth, 41, 20f, -0.5f)
                    } else if (depth <= 100) {
                        calcIncrementValue(depth, 61, 9.85f, -0.25f)
                    } else {
                        0f
                    })
                    return value.mmToRenderUnitValue().decimal(digit, false, false) + unit
                } else {
                    val value = EngraveCmd.depthToSpeed(depth) * 2
                    return value.toFloat().mmToRenderUnitValue().roundToInt().toString() + unit
                }
            } else {
                var newSpeedList: List<SpeedInfo>? = null
                if ("655~699".matchesProductVersion()) {
                    //655新固件速度
                    newSpeedList = SpeedInfo.getNewSpeedList(dpi)
                } else if ("800~899 8000~8499".matchesProductVersion()) {
                    //LP5固件速度
                    newSpeedList = SpeedInfo.getLp5VelocityList(dpi)
                }
                return newSpeedList?.find { it.depth == depth }?.run {
                    val unit = "${LPConstant.renderUnit.getUnit()}/s"
                    return speed.mmToRenderUnitValue().decimal(digit, true, false) + unit
                }
            }
        }
    }

    /**参数配置实体*/
    var itemEngraveConfigEntity: EngraveConfigEntity? = null

    /**单元素参数配置*/
    var itemEngraveItemBean: LPElementBean? = null

    /**是否要显示雕刻次数设置*/
    var itemShowTimes: Boolean = true

    /**是否要显示雕刻速度(深度)设置*/
    var itemShowSpeed: Boolean = true

    /**是否要显示功率*/
    var itemShowPower: Boolean = true

    /**需要显示的文本标签*/
    var itemLabelText: CharSequence? = null

    /**是否要显示速度参考值*/
    var itemShowRefVelocity: Boolean = _showRefVelocity

    /**Label的点击事件*/
    var itemLabelClickAction: ViewAction? = null

    /**是否显示Popup提示*/
    var itemShowPopupTip: Boolean = true

    private val nightModel = vmApp<NightModel>()
    private val laserPeckerModel = vmApp<LaserPeckerModel>()

    val _dpi: Float
        get() = itemEngraveConfigEntity?.dpi ?: itemEngraveItemBean?.dpi ?: 254f

    val _layerId: String
        get() = itemEngraveConfigEntity?.layerId ?: itemEngraveItemBean?._layerId
        ?: LaserPeckerHelper.LAYER_LINE

    init {
        itemLayoutId = R.layout.item_engrave_property_layout
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
        val context = itemHolder.context

        //label
        itemHolder.tv(R.id.lib_label_view)?.text = itemLabelText
        itemHolder.click(R.id.lib_label_view, itemLabelClickAction)

        //属性, 功率
        val powerLabel = _string(R.string.custom_power)
        val power = itemEngraveConfigEntity?.power ?: (itemEngraveItemBean?.printPower
            ?: HawkEngraveKeys.lastPower)
        itemHolder.visible(R.id.power_view, itemShowPower)
        itemHolder.tv(R.id.power_view)?.apply {
            text = span {
                if (HawkEngraveKeys.enableConfigIcon) {
                    appendDrawable(nightModel.tintDrawableNight(_drawable(R.drawable.engrave_config_power_svg)))
                } else {
                    append(powerLabel)
                }
                appendln()
                append("$power") {
                    fontSize = 40 * dpi
                    style = Typeface.BOLD
                    foregroundColor = _color(R.color.device_primary_color)
                }
                append("%")
            }
            if (itemShowPower) {
                checkAndShowTip(this, powerLabel, HawkEngraveKeys::showPowerTipVersion.name)
            }
        }

        //深度-速度
        itemHolder.visible(R.id.speed_wrap_layout, itemShowSpeed)
        val speedLabel = _string(R.string.custom_speed)
        val depth = itemEngraveConfigEntity?.depth ?: (itemEngraveItemBean?.printDepth
            ?: HawkEngraveKeys.lastDepth)
        itemHolder.tv(R.id.speed_view)?.apply {
            text = span {
                if (HawkEngraveKeys.enableConfigIcon) {
                    appendDrawable(nightModel.tintDrawableNight(_drawable(R.drawable.engrave_config_depth_svg)))
                } else {
                    append(speedLabel)
                }
                appendln()
                append("$depth") {
                    fontSize = 40 * dpi
                    style = Typeface.BOLD
                    foregroundColor = _color(R.color.device_primary_color)
                }
                append("%")
            }
            if (itemShowSpeed) {
                checkAndShowTip(this, speedLabel, HawkEngraveKeys::showSpeedTipVersion.name)
            }
        }
        //速度转换计算
        itemHolder.visible(R.id.speed_convert_view, needShowSpeedConvert(_layerId))
        itemHolder.click(R.id.speed_convert_view) {
            it.context.speedConvertDialogConfig {
                appointConvertType = SpeedConvertDialogConfig.TYPE_TO_NEW
                _selectLayerId = _layerId
                _selectDpi = _dpi

                onDepthResultAction = { depth ->
                    updateDepth(depth)
                }
            }
        }

        //雕刻次数
        itemHolder.visible(R.id.times_view, itemShowTimes)
        val timesLabel = _string(R.string.print_times)
        val time = itemEngraveConfigEntity?.time ?: (itemEngraveItemBean?.printCount ?: 1)
        itemHolder.tv(R.id.times_view)?.apply {
            text = span {
                if (HawkEngraveKeys.enableConfigIcon) {
                    appendDrawable(nightModel.tintDrawableNight(_drawable(R.drawable.engrave_config_times_svg)))
                } else {
                    append(timesLabel)
                }
                appendln()
                append("$time") {
                    fontSize = 40 * dpi
                    style = Typeface.BOLD
                    foregroundColor = _color(R.color.device_primary_color)
                }
            }
            if (itemShowTimes) {
                checkAndShowTip(this, timesLabel, HawkEngraveKeys::showTimesTipVersion.name)
            }
        }

        //事件-功率
        itemHolder.click(R.id.power_view) {
            context.wheelDialog {
                dialogTitle = powerLabel
                wheelItems = EngraveHelper.percentList()
                wheelSelectedIndex = EngraveHelper.findOptionIndex(wheelItems, power)
                wheelUnit = "%"

                wheelItemSelectorAction = { dialog, index, item ->
                    getSelectedInt(index, power).let {
                        HawkEngraveKeys.lastPower = it
                        itemEngraveConfigEntity?.power = HawkEngraveKeys.lastPower
                        itemEngraveItemBean?.printPower = HawkEngraveKeys.lastPower
                    }

                    itemChanging = true
                    false
                }
            }
        }
        //深度-速度
        itemHolder.click(R.id.speed_view) {
            context.wheelDialog {
                dialogTitle = speedLabel
                wheelItems = EngraveHelper.percentList()
                wheelSelectedIndex = EngraveHelper.findOptionIndex(wheelItems, depth)
                wheelUnit = "%"

                if (needShowRefVelocity(_layerId)) {
                    wheelItemToStringAction = {

                        val dpi = itemEngraveConfigEntity?.dpi ?: itemEngraveItemBean?.dpi ?: 254f
                        //速度
                        val velocity = getReferenceVelocity(
                            _layerId,
                            it.toStr().toIntOrNull() ?: 0,
                            dpi
                        )

                        //显示速度参考值
                        if (velocity.isNullOrEmpty()) {
                            it.toStr()
                        } else {
                            it.toStr() + " ($velocity)"
                        }
                    }
                }

                wheelItemSelectorAction = { dialog, index, item ->
                    updateDepth(getSelectedInt(index, depth))
                    false
                }
            }
        }
        itemHolder.click(R.id.times_view) {
            context.wheelDialog {
                dialogTitle = timesLabel
                wheelItems = EngraveHelper.percentList(50)//2022-10-21
                wheelSelectedIndex = EngraveHelper.findOptionIndex(wheelItems, time)

                wheelItemSelectorAction = { dialog, index, item ->
                    val times = getSelectedInt(index, time)
                    itemEngraveConfigEntity?.time = times
                    itemEngraveItemBean?.printCount = times
                    itemChanging = true //雕刻次数改变时, 不通知刷新 //2023-7-25 雕刻次数也纳入材质推荐
                    //itemEngraveConfigEntity?.lpSaveEntity() //所以需要主动保存
                    //updateAdapterItem()
                    false
                }
            }
        }
    }

    private fun updateDepth(depth: Int) {
        HawkEngraveKeys.lastDepth = depth
        itemEngraveConfigEntity?.depth = depth
        itemEngraveItemBean?.printDepth = depth

        //气泵参数推荐
        LaserPeckerHelper.getRecommendPump(itemEngraveConfigEntity?.layerId, depth)
            ?.let {
                itemEngraveConfigEntity?.pump = it
                itemDslAdapter?.get<EngravePumpItem>()
                    ?.updateAdapterItem(EngravePumpItem.PAYLOAD_UPDATE_PUMP)
            }
        itemChanging = true
    }

    override fun onItemChangeListener(item: DslAdapterItem) {
        itemEngraveConfigEntity?.lpSaveEntity()
        super.onItemChangeListener(item)
    }

    /**获取选中的byte数据*/
    fun WheelDialogConfig.getSelectedInt(index: Int, def: Int): Int =
        wheelItems?.get(index)?.toString()?.toIntOrNull() ?: def

    /**检查或者显示tip窗口*/
    private fun checkAndShowTip(anchor: View, tip: CharSequence?, hawkKey: String) {
        val value = hawkKey.hawkGetString()?.toLongOrNull()
        val versionCode = getAppVersionCode()
        if (itemShowPopupTip && value != versionCode) {
            anchor.postDelayed(360L) {
                anchor.popupTipWindow(tip)
                hawkKey.hawkPut("$versionCode")
            }
        }
    }

    /**是否需要显示速率参考值*/
    private fun needShowRefVelocity(layoutId: String?): Boolean {
        if (!itemShowRefVelocity) {
            return false
        }
        if (layoutId.isNullOrBlank()) {
            return false
        }
        return _deviceConfigBean?.refVelocityLayer?.contains(layoutId) == true
    }

    /**是否需要速度转换按钮*/
    private fun needShowSpeedConvert(layoutId: String?): Boolean {
        if (!_showSpeedConvertRange) {
            return false
        }
        if (layoutId.isNullOrBlank()) {
            return false
        }
        return _deviceConfigBean?.speedConvertLayer?.contains(layoutId) == true
    }
}
