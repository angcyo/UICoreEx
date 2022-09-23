package com.angcyo.engrave.dslitem.engrave

import com.angcyo.bluetooth.fsc.enqueue
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.command.EngraveCmd
import com.angcyo.bluetooth.fsc.laserpacker.parse.QuerySettingParser
import com.angcyo.bluetooth.fsc.laserpacker.parse.QueryStateParser
import com.angcyo.bluetooth.fsc.laserpacker.parse.QueryStateParser.Companion.WORK_MODE_ENGRAVE
import com.angcyo.bluetooth.fsc.laserpacker.toPxDes
import com.angcyo.core.vmApp
import com.angcyo.dialog.messageDialog
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.R
import com.angcyo.engrave.ble.toZModeString
import com.angcyo.engrave.data.LabelDesData
import com.angcyo.engrave.model.EngraveModel
import com.angcyo.engrave.toEngraveTime
import com.angcyo.engrave.toLaserTypeString
import com.angcyo.library.ex.*
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.base.dslViewHolder
import com.angcyo.widget.base.resetChild
import com.angcyo.widget.flow
import com.angcyo.widget.span.span

/**
 * 雕刻中/雕刻结束的item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/20
 */
class EngravingItem : DslAdapterItem() {

    val laserPeckerModel = vmApp<LaserPeckerModel>()

    val engraveModel = vmApp<EngraveModel>()

    /**再雕一次*/
    var againAction: ClickAction? = null

    //所有需要提示的数据
    val labelDesList = mutableListOf<LabelDesData>()

    init {
        itemLayoutId = R.layout.item_engraving_layout
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        //雕刻状态
        val stateParser: QueryStateParser? = laserPeckerModel.deviceStateData.value

        //恢复的雕刻状态
        val isRestore = engraveModel.isRestore()

        if (stateParser?.mode == WORK_MODE_ENGRAVE) {
            //打印模式
        }

        //打印进度
        stateParser?.rate

        itemHolder.tv(R.id.pause_button)?.text = if (stateParser?.isEngravePause() == true) {
            //打印暂停中
            _string(R.string.print_v2_package_Laser_continue)
        } else {
            _string(R.string.print_v2_package_Laser_pause)
        }

        //可见性
        val isEngraving =
            stateParser?.isEngraving() == true || stateParser?.isEngravePause() == true
        itemHolder.visible(R.id.pause_button, isEngraving)
        itemHolder.visible(R.id.stop_button, isEngraving)
        itemHolder.visible(R.id.again_button, !isEngraving)
        itemHolder.visible(R.id.lib_tip_view, isEngraving)

        //镭雕提示
        itemHolder.tv(R.id.lib_tip_view)?.text = span {
            if (isEngraving) {
                append(_string(R.string.v3_print_state_tips))
            }
        }

        //提示2
        labelDesList.clear()
        if (!isRestore) {
            //不是恢复的数据
            if (laserPeckerModel.isZOpen()) {
                labelDesList.add(
                    LabelDesData(
                        _string(R.string.device_setting_tips_fourteen_11),
                        QuerySettingParser.Z_MODEL.toZModeString()
                    )
                )
            }

            if (laserPeckerModel.productInfoData.value?.isLIIIMax() == true) {
                labelDesList.add(
                    LabelDesData(
                        _string(R.string.laser_type),
                        engraveModel.engraveOptionInfoData.value?.type.toLaserTypeString()
                    )
                )
            }

            //分辨率: 1k
            labelDesList.add(
                LabelDesData(
                    _string(R.string.tv_01),
                    engraveModel.engraveReadyInfoData.value?.engraveData?.px?.toPxDes()
                )
            )
            //材质:
            labelDesList.add(
                LabelDesData(
                    _string(R.string.custom_material),
                    engraveModel.engraveOptionInfoData.value?.material.or()
                )
            )

            //功率:
            labelDesList.add(
                LabelDesData(
                    _string(R.string.custom_power),
                    "${engraveModel.engraveOptionInfoData.value?.power ?: 0}%"
                )
            )

            //深度:
            labelDesList.add(
                LabelDesData(
                    _string(R.string.custom_speed),
                    "${engraveModel.engraveOptionInfoData.value?.depth ?: 0}%"
                )
            )

            //雕刻次数
            val times = engraveModel.engraveOptionInfoData.value?.time?.toHexInt() ?: 1
            val printTimes = engraveModel.engraveReadyInfoData.value?.printTimes ?: 1
            labelDesList.add(
                LabelDesData(_string(R.string.print_times), "${printTimes}/${times}")
            )

            //加工时间
            val startEngraveTime =
                engraveModel.engraveReadyInfoData.value?.startEngraveTime ?: -1
            if (startEngraveTime > 0) {
                var engraveTime = (nowTime() - startEngraveTime).toEngraveTime()
                if (isEngraving) {
                    labelDesList.add(
                        LabelDesData(_string(R.string.tips_fourteen_12), engraveTime)
                    )
                } else {
                    val stopEngraveTime =
                        engraveModel.engraveReadyInfoData.value?.stopEngraveTime ?: nowTime()
                    engraveTime = (stopEngraveTime - startEngraveTime).toEngraveTime()
                    labelDesList.add(
                        LabelDesData(_string(R.string.work_time), engraveTime)
                    )
                }
            }
        }

        itemHolder.flow(R.id.lib_flow_layout)
            ?.resetChild(labelDesList, R.layout.dsl_solid_tag_item) { itemView, item, itemIndex ->
                itemView.dslViewHolder().tv(R.id.lib_label_view)?.text = item.label
                itemView.dslViewHolder().tv(R.id.lib_des_view)?.text = item.des
            }

        //继续/暂停雕刻
        itemHolder.click(R.id.pause_button) {
            if (laserPeckerModel.deviceStateData.value?.isEngravePause() == true) {
                //打印暂停中, 继续雕刻
                EngraveCmd.continueEngrave().enqueue()
                laserPeckerModel.queryDeviceState()
            } else {
                //暂停雕刻
                EngraveCmd.pauseEngrave().enqueue()
                laserPeckerModel.queryDeviceState()
            }
        }

        //结束雕刻
        itemHolder.click(R.id.stop_button) {
            it.context.messageDialog {
                dialogMessage = _string(R.string.print_stop)
                negativeButtonText = _string(R.string.dialog_negative)

                positiveButton { dialog, dialogViewHolder ->
                    dialog.dismiss()
                    //print_stop
                    EngraveCmd.stopEngrave().enqueue()
                    laserPeckerModel.queryDeviceState()
                }
            }
        }

        //再次雕刻
        itemHolder.click(R.id.again_button) {
            againAction?.invoke(it)
        }
    }
}