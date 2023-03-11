package com.angcyo.engrave.ble

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import com.angcyo.base.dslFHelper
import com.angcyo.bluetooth.fsc.enqueue
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.parse.QuerySettingParser
import com.angcyo.bluetooth.fsc.laserpacker.syncQueryDeviceState
import com.angcyo.core.component.fileSelector
import com.angcyo.core.dslitem.DslLastDeviceInfoItem
import com.angcyo.core.fragment.BaseDslFragment
import com.angcyo.core.vmApp
import com.angcyo.dsladapter.DslAdapter
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.drawBottom
import com.angcyo.engrave.R
import com.angcyo.engrave.data.HawkEngraveKeys
import com.angcyo.engrave.data.ZModel
import com.angcyo.item.DslPropertySwitchItem
import com.angcyo.item.DslSegmentTabItem
import com.angcyo.item.DslTextInfoItem
import com.angcyo.item.style.*
import com.angcyo.library.ex._dimen
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

        /**z轴的3中模式*/
        fun getZDirSegmentList() = if (vmApp<LaserPeckerModel>().isC1()) {
            //C1只有圆柱模式
            listOf(ZModel(QuerySettingParser.Z_MODEL_CYLINDER))
        } else if (vmApp<LaserPeckerModel>().isL3()) {
            //L3没有小车 2023-1-4
            listOf(
                ZModel(QuerySettingParser.Z_MODEL_FLAT),
                ZModel(QuerySettingParser.Z_MODEL_CYLINDER)
            )
        } else {
            listOf(
                ZModel(QuerySettingParser.Z_MODEL_FLAT),
                ZModel(QuerySettingParser.Z_MODEL_CAR),
                ZModel(QuerySettingParser.Z_MODEL_CYLINDER)
            )
        }

        /**上传日志的item*/
        var createUploadLoadItemAction: ((fragment: DeviceSettingFragment, adapter: DslAdapter) -> DslAdapterItem?)? =
            null

        /**创建固件升级的item*/
        var createFirmwareUpdateItemAction: ((fragment: DeviceSettingFragment, adapter: DslAdapter) -> DslAdapterItem?)? =
            null

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
        val isL2 = productInfo?.isLII() == true
        val isL3 = productInfo?.isLIII() == true
        val isL4 = productInfo?.isLIV() == true
        val isC1CarFlag = isC1 && settingParser?.carFlag == 1

        //是否需要z轴开关
        var zEx = true
        //是否需要r轴开关, 旋转轴
        var rEx = isL4
        //是否需要s轴开关, 滑台
        var sEx = isL4
        if (isC1CarFlag) {
            //自动进入了移动平台模式
            zEx = false
            rEx = false
            sEx = false
        } else if (isC1) {
            zEx = true
            rEx = true
        }
        if (isC1) {
            sEx = false
        }

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
            //雕刻方向
            if (isL2 && productInfo?.version in 374..399) {
                DslPropertySwitchItem()() {
                    itemLabel = _string(R.string.device_setting_engrave_dir)
                    itemDes = _string(R.string.device_setting_engrave_dir_des)
                    initItem()

                    itemSwitchChecked = settingParser?.printDir == 1
                    itemSwitchChangedAction = {
                        settingParser?.printDir = if (it) 1 else 0
                        settingParser?.updateSetting()
                    }
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

                    itemSwitchChecked = settingParser?.gcodeView == QuerySettingParser.GCODE_PREVIEW
                    itemSwitchChangedAction = {
                        settingParser?.gcodeView =
                            if (it) QuerySettingParser.GCODE_PREVIEW else QuerySettingParser.GCODE_RECT_PREVIEW
                        settingParser?.updateSetting()
                    }
                }
            }
            if (isL4 || isC1) {
                DslPropertySwitchItem()() {
                    itemLabel = _string(R.string.device_g_code_pwr_label)
                    itemDes = _string(R.string.device_g_code_pwr_des)
                    initItem()

                    itemSwitchChecked = settingParser?.gcodePower == 1
                    itemSwitchChangedAction = {
                        settingParser?.gcodePower = if (it) 1 else 0
                        settingParser?.updateSetting()
                    }
                }
            }
            //第三轴
            if (zEx) {
                DslPropertySwitchItem()() {
                    itemLabel = _string(R.string.device_ex_z_label)
                    itemDes = _string(R.string.device_ex_z_des)

                    itemSwitchChecked = settingParser?.zFlag == 1
                    itemSwitchChangedAction = {
                        settingParser?.clearFlag()
                        settingParser?.zFlag = if (it) 1 else 0
                        settingParser?.updateSetting()
                        renderData()
                    }
                }
                if (!isC1) {
                    //C1只有一种模式, 干脆就不显示了
                    DslSegmentTabItem()() {
                        itemLayoutId = R.layout.device_z_dir_segment_tab_item
                        initItem()

                        //平板 //小车 //圆柱
                        val zModelList = getZDirSegmentList()
                        itemSegmentList = zModelList

                        //zDir 0为打直板，1为打印圆柱。
                        val zModel = if (productInfo?.version in 373..399) {
                            if (settingParser?.zDir == 0) QuerySettingParser.Z_MODEL_CYLINDER else QuerySettingParser.Z_MODEL_STR
                        } else {
                            if (settingParser?.zDir == 1) QuerySettingParser.Z_MODEL_CYLINDER else QuerySettingParser.Z_MODEL_STR
                        }
                        itemCurrentIndex = max(zModelList.indexOfFirst { it.resKey == zModel }, 0)
                        /* val zDirIndex = max(if (settingParser?.zDir == 1) 2 else 0, maxIndex)
                             if (zDirIndex == 0 && (QuerySettingParser.Z_MODEL == 0 || QuerySettingParser.Z_MODEL == 1)) {
                                 //平板和小车都对应的 0
                                 QuerySettingParser.Z_MODEL
                             } else {
                                 zDirIndex
                             }*/
                        itemSelectIndexChangeAction =
                            { fromIndex: Int, selectIndexList: List<Int>, reselect: Boolean, fromUser: Boolean ->
                                val index = selectIndexList.first()
                                //QuerySettingParser.Z_MODEL = index //确切的模式
                                QuerySettingParser.Z_MODEL_STR = zModelList.getOrNull(index)?.resKey
                                    ?: QuerySettingParser.Z_MODEL_FLAT //确切的模式
                                if (productInfo?.version in 373..399) {
                                    //2023-3-10 373的版本小车模式和圆柱模式逻辑统一
                                    settingParser?.zDir =
                                        if (QuerySettingParser.Z_MODEL_STR == QuerySettingParser.Z_MODEL_FLAT) 0 else 1
                                } else {
                                    settingParser?.zDir =
                                        if (QuerySettingParser.Z_MODEL_STR == QuerySettingParser.Z_MODEL_CYLINDER) 1 else 0
                                }
                                settingParser?.updateSetting()
                            }
                    }
                }
            }
            //旋转轴
            if (rEx) {
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
            }
            //滑台
            if (sEx) {
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
            if (sEx) {
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
            if (zEx && settingParser != null) {
                /*if (settingParser.zFlag == 1) {
                    //Z轴的时候, 才有正转/反转
                    DslPropertySwitchItem()() {
                        itemLabel = _string(R.string.device_ex_direction_label)
                        itemDes = _string(R.string.device_ex_direction_des)
                        initItem()

                        itemSwitchChecked = settingParser.dir == 1
                        itemSwitchChangedAction = {
                            settingParser.dir = if (it) 1 else 0
                            settingParser.updateSetting()
                        }
                    }
                }*/
            }
            //C1 移动平台雕刻模式
            if (isC1) {
                //2022-11-9 不需要显示, 设备自动设置, 不允许关闭
                /*DslPropertySwitchItem()() {
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
                }*/
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

            //实验性功能
            if (HawkEngraveKeys.enableExperimental) {
                DslTextInfoItem()() {
                    itemInfoText = _string(R.string.engrave_experimental)
                    itemDarkIcon = R.drawable.lib_next
                    initItem()
                    configInfoTextStyle {
                        textSize = _dimen(R.dimen.text_sub_size).toFloat()
                    }
                    itemClick = {
                        dslFHelper {
                            show(EngraveExperimentalFragment::class)
                        }
                    }
                }
            }

            //上传日志
            createUploadLoadItemAction?.invoke(this@DeviceSettingFragment, this)?.let {
                it.initItem()
                this + it
            }

            //固件升级
            createFirmwareUpdateItemAction?.invoke(this@DeviceSettingFragment, this)?.let {
                it.initItem()
                this + it
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
        syncQueryDeviceState { bean, error ->
            laserPeckerModel.updateSettingOnceData.postValue(true)
        }
        //LaserPeckerHelper.initDeviceSetting()
    }
}