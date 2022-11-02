package com.angcyo.engrave.ble

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import com.angcyo.base.dslFHelper
import com.angcyo.bluetooth.fsc.enqueue
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.asyncQueryDeviceState
import com.angcyo.bluetooth.fsc.laserpacker.parse.QuerySettingParser
import com.angcyo.core.component.fileSelector
import com.angcyo.core.dslitem.DslLastDeviceInfoItem
import com.angcyo.core.fragment.BaseDslFragment
import com.angcyo.core.vmApp
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.drawBottom
import com.angcyo.engrave.R
import com.angcyo.engrave.data.HawkEngraveKeys
import com.angcyo.item.DslPropertySwitchItem
import com.angcyo.item.DslSegmentTabItem
import com.angcyo.item.style.*
import com.angcyo.library.ex._string
import com.angcyo.library.ex.isDebug
import com.angcyo.library.utils.FileUtils
import kotlin.math.max


/**
 * 设备设置界面
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/08
 */
class DeviceSettingFragment : BaseDslFragment() {

    companion object {

        fun getZDirSegmentList() = if (vmApp<LaserPeckerModel>().isC1()) {
            //C1只有圆柱模式
            listOf(
                _string(R.string.device_setting_tips_fourteen_10)
            )
        } else {
            listOf(
                _string(R.string.device_setting_tips_fourteen_8),
                _string(R.string.device_setting_tips_fourteen_9),
                _string(R.string.device_setting_tips_fourteen_10)
            )
        }

    }

    val laserPeckerModel = vmApp<LaserPeckerModel>()

    init {
        fragmentTitle = _string(R.string.ui_slip_menu_model)
        fragmentConfig.isLightStyle = true
        fragmentConfig.showTitleLineView = true
    }

    override fun onInitFragment(savedInstanceState: Bundle?) {
        fragmentConfig.fragmentBackgroundDrawable = ColorDrawable(Color.WHITE)
        super.onInitFragment(savedInstanceState)
    }

    override fun initBaseView(savedInstanceState: Bundle?) {
        super.initBaseView(savedInstanceState)

        laserPeckerModel.deviceSettingData.observe {
            it?.let {
                renderData()
            }
        }

        //读取设置
        LaserPeckerHelper.initDeviceSetting()
    }

    fun renderData() {
        val settingParser = laserPeckerModel.deviceSettingData.value
        settingParser?.functionSetting()

        val productInfo = laserPeckerModel.productInfoData.value
        val isC1 = productInfo?.isCI() == true

        renderDslAdapter(reset = true) {
            DslPropertySwitchItem()() {
                itemLabel = _string(R.string.device_setting_act_model_warning_tone)
                itemDes = _string(R.string.device_setting_act_des_sound)
                initItem()

                itemSwitchChecked = settingParser?.buzzer == 1
                itemSwitchChangedAction = {
                    settingParser?.buzzer = if (it) 1 else 0
                    settingParser?.updateSetting()
                }
            }
            DslPropertySwitchItem()() {
                itemLabel = _string(R.string.device_setting_act_model_security)
                itemDes = _string(R.string.device_setting_act_des_security)
                initItem()

                itemSwitchChecked = settingParser?.safe == 1
                itemSwitchChangedAction = {
                    settingParser?.safe = if (it) 1 else 0
                    settingParser?.updateSetting()
                }
            }
            DslPropertySwitchItem()() {
                itemLabel = _string(R.string.device_setting_act_model_free)
                itemDes = _string(R.string.device_setting_act_des_free)
                initItem()

                itemSwitchChecked = settingParser?.free == 1
                itemSwitchChangedAction = {
                    settingParser?.free = if (it) 1 else 0
                    settingParser?.updateSetting()
                }
            }
            //GCode预览
            if (isC1) {
                //C1不支持矢量预览
            } else {
                DslPropertySwitchItem()() {
                    itemLabel = _string(R.string.device_setting_act_model_preview_g_code)
                    itemDes = _string(R.string.device_setting_act_des_preview_g_code)
                    initItem()

                    itemSwitchChecked = settingParser?.gcodeView == 1
                    itemSwitchChangedAction = {
                        settingParser?.gcodeView = if (it) 1 else 0
                        settingParser?.updateSetting()
                    }
                }
            }
            //第三轴
            DslPropertySwitchItem()() {
                itemLabel = _string(R.string.device_setting_tips_fourteen_2)
                itemDes = _string(R.string.device_setting_tips_fourteen_3)

                itemSwitchChecked = settingParser?.zFlag == 1
                itemSwitchChangedAction = {
                    settingParser?.clearFlag()
                    settingParser?.zFlag = if (it) 1 else 0
                    settingParser?.updateSetting()
                    renderData()
                }
            }
            DslSegmentTabItem()() {
                itemLayoutId = R.layout.device_z_dir_segment_tab_item
                initItem()

                //平板 //小车 //圆柱
                itemSegmentList = getZDirSegmentList()
                val maxIndex = itemSegmentList.lastIndex

                //zDir 0为打直板，1为打印圆柱。
                val zDirIndex = max(if (settingParser?.zDir == 1) 2 else 0, maxIndex)
                itemCurrentIndex =
                    if (zDirIndex == 0 && (QuerySettingParser.Z_MODEL == 0 || QuerySettingParser.Z_MODEL == 1)) {
                        //平板和小车都对应的 0
                        QuerySettingParser.Z_MODEL
                    } else {
                        zDirIndex
                    }
                itemSelectIndexChangeAction =
                    { fromIndex: Int, selectIndexList: List<Int>, reselect: Boolean, fromUser: Boolean ->
                        val index = selectIndexList.first()
                        QuerySettingParser.Z_MODEL = index //确切的模式
                        settingParser?.zDir = if (index == 2) 1 else 0
                        settingParser?.updateSetting()
                    }
            }
            //旋转轴
            DslPropertySwitchItem()() {
                itemLabel = _string(R.string.device_ex_r_label)
                itemDes = _string(R.string.device_ex_r_des)
                initItem()

                itemSwitchChecked = settingParser?.rFlag == 1
                itemSwitchChangedAction = {
                    settingParser?.clearFlag()
                    settingParser?.rFlag = if (it) 1 else 0
                    settingParser?.updateSetting()
                    renderData()
                }
            }
            //滑台
            if (isC1) {

            } else {
                DslPropertySwitchItem()() {
                    itemLabel = _string(R.string.device_ex_s_label)
                    itemDes = _string(R.string.device_ex_s_des)
                    initItem()

                    itemSwitchChecked = settingParser?.sFlag == 1
                    itemSwitchChangedAction = {
                        settingParser?.clearFlag()
                        settingParser?.sFlag = if (it) 1 else 0
                        settingParser?.updateSetting()
                        renderData()
                    }
                }
            }
            //滑台批量雕刻
            if (isC1) {

            } else {
                DslPropertySwitchItem()() {
                    itemLabel = _string(R.string.device_s_batch_engrave_label)
                    itemDes = _string(R.string.device_s_batch_engrave_des)
                    initItem()

                    itemSwitchChecked = settingParser?.sRep == 1
                    itemSwitchChangedAction = {
                        settingParser?.clearFlag()
                        settingParser?.sRep = if (it) 1 else 0
                        settingParser?.updateSetting()
                        renderData()
                    }
                }
            }
            //正转
            DslPropertySwitchItem()() {
                itemLabel = _string(R.string.device_ex_direction_label)
                itemDes = _string(R.string.device_ex_direction_des)
                initItem()

                itemSwitchChecked = settingParser?.dir == 1
                itemSwitchChangedAction = {
                    settingParser?.dir = if (it) 1 else 0
                    settingParser?.updateSetting()
                }
            }
            //C1 移动平台雕刻模式
            if (isC1) {
                DslPropertySwitchItem()() {
                    itemLabel = _string(R.string.device_ex_car_label)
                    itemDes = _string(R.string.device_ex_car_des)
                    initItem()

                    itemSwitchChecked = settingParser?.carFlag == 1
                    itemSwitchChangedAction = {
                        settingParser?.clearFlag()
                        settingParser?.carFlag = if (it) 1 else 0
                        settingParser?.updateSetting()
                        renderData()
                    }
                }
            }

            //---

            DslPropertySwitchItem()() {
                itemLabel = _string(R.string.device_setting_txt_3)
                itemDes = _string(R.string.device_setting_txt_4)
                initItem()

                itemSwitchChecked = settingParser?.keyView == 1
                itemSwitchChangedAction = {
                    settingParser?.keyView = if (it) 1 else 0
                    settingParser?.updateSetting()
                }
            }
            DslPropertySwitchItem()() {
                itemLabel = _string(R.string.device_setting_txt_5)
                itemDes = _string(R.string.device_setting_txt_6)
                initItem()

                itemSwitchChecked = settingParser?.keyPrint == 1
                itemSwitchChangedAction = {
                    settingParser?.keyPrint = if (it) 1 else 0
                    settingParser?.updateSetting()
                }
            }
            DslPropertySwitchItem()() {
                itemLabel = _string(R.string.button_infra_red_title)
                itemDes = _string(R.string.button_infra_red_content)
                initItem()

                itemSwitchChecked = settingParser?.irDst == 1
                itemSwitchChangedAction = {
                    settingParser?.irDst = if (it) 1 else 0
                    settingParser?.updateSetting()
                }
            }
            DslPropertySwitchItem()() {
                itemLabel = _string(R.string.device_setting_blue_connect_auto)
                initItem()

                itemSwitchChecked = HawkEngraveKeys.AUTO_CONNECT_DEVICE
                itemSwitchChangedAction = {
                    HawkEngraveKeys.AUTO_CONNECT_DEVICE = it
                }
            }
            //
            if (isDebug()) {
                DslLastDeviceInfoItem()() {
                    itemClick = {
                        dslFHelper {
                            fileSelector({
                                showFileMd5 = true
                                showFileMenu = true
                                showHideFile = true
                                targetPath =
                                    FileUtils.appRootExternalFolder().absolutePath
                                        ?: storageDirectory
                            })
                        }
                    }
                }
            }
        }
    }

    fun DslAdapterItem.initItem() {
        drawBottom()
    }

    fun QuerySettingParser.updateSetting() {
        //sendCommand()
        enqueue()
        asyncQueryDeviceState { bean, error ->
            laserPeckerModel.updateSettingOnceData.postValue(true)
        }
        //LaserPeckerHelper.initDeviceSetting()
    }

}

/**将z轴的模式, 转换成可视化*/
fun Int.toZModeString() = when (this) {
    0 -> _string(R.string.device_setting_tips_fourteen_8)
    1 -> _string(R.string.device_setting_tips_fourteen_9)
    2 -> _string(R.string.device_setting_tips_fourteen_10)
    else -> _string(R.string.device_setting_tips_fourteen_8)
}

/**将z轴模式, 转换成机器指令
 * Z_dir:  0为打直板，1为打印圆柱。
 * */
fun Int.toZModeDir() = if (this == 2) 1 else 0