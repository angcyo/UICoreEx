package com.angcyo.laserpacker.device.ble

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import com.angcyo.base.dslFHelper
import com.angcyo.bluetooth.fsc.IReceiveBeanAction
import com.angcyo.bluetooth.fsc.ReceivePacket
import com.angcyo.bluetooth.fsc.enqueue
import com.angcyo.bluetooth.fsc.laserpacker.DeviceStateModel
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerConfigHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.parse.QuerySettingParser
import com.angcyo.bluetooth.fsc.laserpacker.syncQueryDeviceState
import com.angcyo.core.dslitem.DslLastDeviceInfoItem
import com.angcyo.core.fragment.BaseDslFragment
import com.angcyo.core.vmApp
import com.angcyo.dsladapter.DslAdapter
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.drawBottom
import com.angcyo.item.DslPropertySwitchItem
import com.angcyo.item.DslSegmentTabItem
import com.angcyo.item.DslTextInfoItem
import com.angcyo.item.style.itemCurrentIndex
import com.angcyo.item.style.itemDes
import com.angcyo.item.style.itemInfoText
import com.angcyo.item.style.itemLabel
import com.angcyo.item.style.itemSelectIndexChangeAction
import com.angcyo.item.style.itemSwitchChangedAction
import com.angcyo.item.style.itemSwitchChecked
import com.angcyo.laserpacker.device.R
import com.angcyo.laserpacker.device.engraveLoadingAsyncTimeout
import com.angcyo.library.component.VersionMatcher
import com.angcyo.library.ex._dimen
import com.angcyo.library.ex._string
import com.angcyo.library.ex.isDebug
import com.angcyo.library.ex.size
import com.angcyo.library.ex.syncSingle
import com.angcyo.library.toastQQ
import kotlin.math.max


/**
 * 设备设置界面
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/08
 */
class DeviceSettingFragment : BaseDslFragment() {

    companion object {

        /**上传日志的item*/
        var createUploadLogItemAction: ((fragment: DeviceSettingFragment, adapter: DslAdapter) -> DslAdapterItem?)? =
            null

        /**创建固件升级的item*/
        var createFirmwareUpdateItemAction: ((fragment: DeviceSettingFragment, adapter: DslAdapter) -> DslAdapterItem?)? =
            null

    }

    val laserPeckerModel = vmApp<LaserPeckerModel>()
    val deviceStateModel = vmApp<DeviceStateModel>()

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
        val zModelList = productInfo?.zModeList
        val config = LaserPeckerConfigHelper.readDeviceSettingConfig()

        val isC1 = productInfo?.isCI() == true
        val isCSeries = productInfo?.isCSeries() == true
        val isL4 = productInfo?.isLIV() == true

        //强制隐藏Z/S/R开关
        var forceHideZSR = false
        forceHideZSR = deviceStateModel.isPenMode() || deviceStateModel.isCarMode()

        //强制隐藏批量雕刻按键
        var forceHideKeyPrint = false
        forceHideKeyPrint = isC1 && (settingParser?.zFlag == 1 || settingParser?.rFlag == 1)

        renderDslAdapter(reset = true) {
            if (VersionMatcher.matches(
                    productInfo?.version,
                    config?.showBuzzerRange,
                    false,
                    true
                )
            ) {
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
            }

            if (VersionMatcher.matches(productInfo?.version, config?.showSafeRange, false, true)) {
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
            }

            if (VersionMatcher.matches(productInfo?.version, config?.showFreeRange, false, true)) {
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
            }

            //雕刻方向
            if (settingParser?.zFlag == 0 &&
                VersionMatcher.matches(productInfo?.version, config?.showPrintDirRange, false, true)
            ) {
                //第三轴打开的情况下, 不允许调整雕刻方向
                DslPropertySwitchItem()() {
                    itemLabel = _string(R.string.device_setting_engrave_dir)
                    itemDes = _string(R.string.device_setting_engrave_dir_des)
                    initItem()

                    itemSwitchChecked = settingParser.printDir == 1
                    itemSwitchChangedAction = {
                        settingParser.printDir = if (it) 1 else 0
                        settingParser.updateSetting()
                    }
                }
            }
            //GCode预览
            if (VersionMatcher.matches(
                    productInfo?.version,
                    config?.showGcodeViewRange,
                    false,
                    true
                )
            ) {
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

            if (VersionMatcher.matches(
                    productInfo?.version,
                    config?.showGcodePowerRange,
                    false,
                    true
                )
            ) {
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
            if (!forceHideZSR && VersionMatcher.matches(
                    productInfo?.version,
                    config?.showZFlagRange,
                    false,
                    true
                )
            ) {
                DslPropertySwitchItem()() {
                    itemLabel = _string(R.string.device_ex_z_label)
                    itemDes = _string(R.string.device_ex_z_des)

                    itemSwitchChecked = settingParser?.zFlag == 1
                    itemSwitchChangedAction = {
                        settingParser?.clearFlag()
                        settingParser?.zFlag = if (it) 1 else 0
                        updateSettingTimeout(settingParser, isCSeries)
                    }
                }
                if (!zModelList.isNullOrEmpty() && zModelList.size() > 1) {
                    //C1只有一种模式, 干脆就不显示了
                    DslSegmentTabItem()() {
                        itemLayoutId = R.layout.device_z_dir_segment_tab_item
                        initItem()

                        //平板 //小车 //圆柱
                        itemSegmentList = zModelList

                        //zDir 0为打直板，1为打印圆柱。
                        val zModel =
                            if (settingParser?.zDir == 1) QuerySettingParser.Z_MODEL_CYLINDER else QuerySettingParser.Z_MODEL_STR

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
                                settingParser?.zDir =
                                    if (QuerySettingParser.Z_MODEL_STR == QuerySettingParser.Z_MODEL_CYLINDER) 1 else 0
                                settingParser?.updateSetting()
                            }
                    }
                }
            }
            //旋转轴
            if (!forceHideZSR && VersionMatcher.matches(
                    productInfo?.version,
                    config?.showRFlagRange,
                    false,
                    true
                )
            ) {
                DslPropertySwitchItem()() {
                    itemLabel = _string(R.string.device_ex_r_label)
                    itemDes = _string(R.string.device_ex_r_des)
                    initItem()

                    itemSwitchChecked = settingParser?.rFlag == 1
                    itemSwitchChangedAction = {
                        settingParser?.clearFlag()
                        settingParser?.rFlag = if (it) 1 else 0
                        updateSettingTimeout(settingParser, isCSeries)
                    }
                }
            }
            //滑台
            if (!forceHideZSR && VersionMatcher.matches(
                    productInfo?.version,
                    config?.showSFlagRange,
                    false,
                    true
                )
            ) {
                DslPropertySwitchItem()() {
                    itemLabel = _string(R.string.device_ex_s_label)
                    itemDes = _string(R.string.device_ex_s_des)
                    initItem()

                    itemSwitchChecked = settingParser?.sFlag == 1
                    itemSwitchChangedAction = {
                        settingParser?.clearFlag()
                        settingParser?.sFlag = if (it) 1 else 0

                        //L4 调整滑台开关需要等待设备返回
                        updateSettingTimeout(settingParser, isL4)
                    }
                }
            }
            //滑台批量雕刻
            if (!forceHideZSR && VersionMatcher.matches(
                    productInfo?.version,
                    config?.showSRepRange,
                    false,
                    true
                )
            ) {
                DslPropertySwitchItem()() {
                    itemLabel = _string(R.string.device_s_batch_engrave_label)
                    itemDes = _string(R.string.device_s_batch_engrave_des)
                    initItem()

                    itemSwitchChecked = settingParser?.sRep == 1
                    itemSwitchChangedAction = {
                        settingParser?.clearFlag()
                        settingParser?.sRep = if (it) 1 else 0
                        if (isL4) {
                            //L4 调整滑台开关需要等待设备返回
                            this@DeviceSettingFragment.engraveLoadingAsyncTimeout({
                                syncSingle { countDownLatch ->
                                    settingParser?.receiveTimeout = 1 * 60 * 1000
                                    settingParser?.updateSetting { bean, error ->
                                        settingParser.receiveTimeout = null
                                        error?.let {
                                            toastQQ(it.message)
                                        }
                                        countDownLatch.countDown()
                                    }
                                }
                            }) {
                                renderData()
                            }
                        } else {
                            settingParser?.updateSetting()
                            renderData()
                        }
                    }
                }
            }
            //正转
            if (VersionMatcher.matches(productInfo?.version, config?.showDirRange, false, true)) {
                if (settingParser?.zFlag == 1 || settingParser?.rFlag == 1) {
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
                }
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

            //主机触摸按键
            if (VersionMatcher.matches(
                    productInfo?.version,
                    config?.showKeyViewRange,
                    false,
                    true
                )
            ) {
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
            }

            //批量雕刻按键
            if (!forceHideKeyPrint && VersionMatcher.matches(
                    productInfo?.version,
                    config?.showKeyPrintRange,
                    false,
                    true
                )
            ) {
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
            }

            //红光常亮
            if (VersionMatcher.matches(productInfo?.version, config?.showIrDstRange, false, true)) {
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
            }

            //自动连接蓝牙
            if (VersionMatcher.matches(
                    productInfo?.version,
                    config?.showAutoConnectRange,
                    false,
                    true
                )
            ) {
                DslPropertySwitchItem()() {
                    itemLabel = _string(R.string.device_setting_blue_connect_auto)
                    initItem()

                    itemSwitchChecked = HawkEngraveKeys.AUTO_CONNECT_DEVICE
                    itemSwitchChangedAction = {
                        HawkEngraveKeys.AUTO_CONNECT_DEVICE = it
                    }
                }
            }

            //实验性功能
            if (HawkEngraveKeys.enableExperimental || VersionMatcher.matches(
                    productInfo?.version,
                    config?.showExperimentalRange,
                    false,
                    true
                )
            ) {
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
            if (VersionMatcher.matches(
                    productInfo?.version,
                    config?.showUploadLogRange,
                    false,
                    true
                )
            ) {
                createUploadLogItemAction?.invoke(this@DeviceSettingFragment, this)?.let {
                    it.initItem()
                    this + it
                }
            }

            //固件升级
            if (VersionMatcher.matches(
                    productInfo?.version,
                    config?.showFirmwareUpdateRange,
                    false,
                    true
                )
            ) {
                createFirmwareUpdateItemAction?.invoke(this@DeviceSettingFragment, this)?.let {
                    it.initItem()
                    this + it
                }
            }

            //
            if (isDebug()) {
                DslLastDeviceInfoItem()()
            }
        }
    }

    fun DslAdapterItem.initItem() {
        drawBottom()
    }

    fun updateSettingTimeout(settingParser: QuerySettingParser?, timeout: Boolean) {
        if (timeout) {
            engraveLoadingAsyncTimeout({
                syncSingle { countDownLatch ->
                    settingParser?.receiveTimeout = 1 * 60 * 1000
                    settingParser?.updateSetting { bean, error ->
                        settingParser.receiveTimeout = null
                        error?.let {
                            toastQQ(it.message)
                        }
                        countDownLatch.countDown()
                    }
                }
            }) {
                renderData()
            }
        } else {
            settingParser?.updateSetting()
            renderData()
        }
    }

    fun QuerySettingParser.updateSetting(
        action: IReceiveBeanAction = { bean: ReceivePacket?, error: Exception? ->
            error?.let {
                toastQQ(it.message)
            }
        }
    ) {
        //sendCommand()
        enqueue(action = action)
        syncQueryDeviceState { bean, error ->
            laserPeckerModel.updateSettingOnceData.postValue(true)
        }
        //LaserPeckerHelper.initDeviceSetting()
    }
}