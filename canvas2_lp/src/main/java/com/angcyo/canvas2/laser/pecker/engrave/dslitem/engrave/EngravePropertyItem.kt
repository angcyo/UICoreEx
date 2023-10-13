package com.angcyo.canvas2.laser.pecker.engrave.dslitem.engrave

import android.graphics.Typeface
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.bean._showRefVelocity
import com.angcyo.bluetooth.fsc.laserpacker.command.EngraveCmd
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.core.component.model.NightModel
import com.angcyo.core.vmApp
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
import com.angcyo.library.ex.toStr
import com.angcyo.objectbox.laser.pecker.entity.EngraveConfigEntity
import com.angcyo.objectbox.laser.pecker.lpSaveEntity
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.span.span

/**
 * 雕刻属性item, 包含功率/深度/次数
 *
 * [com.angcyo.engrave.dslitem.engrave.EngraveOptionWheelItem]
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/10/15
 */
class EngravePropertyItem : DslAdapterItem() {

    /**参数配置实体*/
    var itemEngraveConfigEntity: EngraveConfigEntity? = null

    /**单元素参数配置*/
    var itemEngraveItemBean: LPElementBean? = null

    /**是否要显示雕刻次数*/
    var itemShowTimes: Boolean = true

    /**需要显示的文本标签*/
    var itemLabelText: CharSequence? = null

    /**是否要显示速度参考值*/
    var itemShowRefVelocity: Boolean = _showRefVelocity

    /**Label的点击事件*/
    var itemLabelClickAction: ViewAction? = null

    private val nightModel = vmApp<NightModel>()

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
        itemHolder.tv(R.id.power_view)?.text = span {
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

        //深度-速度
        val speedLabel = _string(R.string.custom_speed)
        val depth = itemEngraveConfigEntity?.depth ?: (itemEngraveItemBean?.printDepth
            ?: HawkEngraveKeys.lastDepth)
        itemHolder.tv(R.id.speed_view)?.text = span {
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

        //雕刻次数
        itemHolder.visible(R.id.times_view, itemShowTimes)
        val timesLabel = _string(R.string.print_times)
        val time = itemEngraveConfigEntity?.time ?: (itemEngraveItemBean?.printCount ?: 1)
        itemHolder.tv(R.id.times_view)?.text = span {
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

        //事件
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

                if (itemShowRefVelocity) {
                    wheelItemToStringAction = {
                        it.toStr() + " (${getReferenceVelocity(it.toStr().toIntOrNull() ?: 0)}mm/s)"
                    }
                }

                wheelItemSelectorAction = { dialog, index, item ->
                    getSelectedInt(index, depth).let {
                        HawkEngraveKeys.lastDepth = it
                        itemEngraveConfigEntity?.depth = it
                        itemEngraveItemBean?.printDepth = it

                        //气泵参数推荐
                        LaserPeckerHelper.getRecommendPump(itemEngraveConfigEntity?.layerId, it)
                            ?.let {
                                itemEngraveConfigEntity?.pump = it
                                itemDslAdapter?.get<EngravePumpItem>()
                                    ?.updateAdapterItem(EngravePumpItem.PAYLOAD_UPDATE_PUMP)
                            }
                    }
                    itemChanging = true
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

    override fun onItemChangeListener(item: DslAdapterItem) {
        itemEngraveConfigEntity?.lpSaveEntity()
        super.onItemChangeListener(item)
    }

    /**获取深度对应的参考速度*/
    private fun getReferenceVelocity(depth: Int): String? {
        val layoutId = itemEngraveConfigEntity?.layerId ?: itemEngraveItemBean?._layerId
        val digit = 2
        if (layoutId == LaserPeckerHelper.LAYER_LINE || layoutId == LaserPeckerHelper.LAYER_CUT) {
            return (if (depth <= 40) {
                calcIncrementValue(depth, 1, 80f, -1.5f)
            } else if (depth <= 60) {
                calcIncrementValue(depth, 41, 20f, -0.5f)
            } else if (depth <= 100) {
                calcIncrementValue(depth, 61, 9.85f, -0.25f)
            } else {
                null
            })?.decimal(digit, false, false)
        } else {
            return "${EngraveCmd.depthToSpeed(depth) * 2}"
        }
    }

    /**获取选中的byte数据*/
    fun WheelDialogConfig.getSelectedInt(index: Int, def: Int): Int =
        wheelItems?.get(index)?.toString()?.toIntOrNull() ?: def
}
