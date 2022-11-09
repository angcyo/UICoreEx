package com.angcyo.engrave.auto

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import androidx.core.graphics.drawable.toDrawable
import com.angcyo.activity.BaseAppCompatActivity
import com.angcyo.bluetooth.fsc.FscBleApiModel
import com.angcyo.bluetooth.fsc.enqueue
import com.angcyo.bluetooth.fsc.laserpacker.command.ExitCmd
import com.angcyo.canvas.data.CanvasOpenDataType
import com.angcyo.canvas.data.CanvasProjectBean
import com.angcyo.canvas.data.CanvasProjectItemBean
import com.angcyo.canvas.data.toTypeNameString
import com.angcyo.canvas.graphics.GraphicsHelper
import com.angcyo.core.component.dslPermissions
import com.angcyo.core.vmApp
import com.angcyo.dsladapter.DslAdapter
import com.angcyo.dsladapter.DslAdapterStatusItem
import com.angcyo.dsladapter.updateItem
import com.angcyo.engrave.ble.bluetoothSearchListDialog
import com.angcyo.engrave.model.AutoEngraveModel
import com.angcyo.engrave.model.EngraveModel
import com.angcyo.library.component._delay
import com.angcyo.library.ex.toBitmapOfBase64
import com.angcyo.library.ex.uuid
import com.angcyo.library.getAppIcon
import com.angcyo.library.toast
import com.angcyo.viewmodel.observe
import com.angcyo.widget.recycler.renderDslAdapter

/**
 * 自动雕刻界面显示
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/10/22
 */
class AutoEngraveActivity : BaseAppCompatActivity() {

    /**自动雕刻模式*/
    val autoEngraveModel = vmApp<AutoEngraveModel>()

    val engraveModel = vmApp<EngraveModel>()

    val fscBleApiModel = vmApp<FscBleApiModel>()

    //---

    /**需要雕刻的数据*/
    var _engraveData: CanvasOpenDataType? = null

    /**已经创建的雕刻任务*/
    var _autoEngraveTask: AutoEngraveModel.AutoEngraveTask? = null

    var taskId = uuid()

    init {
        activityLayoutId = R.layout.activity_open_preview_layout
    }

    override fun onCreateAfter(savedInstanceState: Bundle?) {
        super.onCreateAfter(savedInstanceState)

        var adapter: DslAdapter? = null

        _vh.rv(R.id.lib_recycler_view)?.apply {
            setBackgroundColor(Color.WHITE)
            renderDslAdapter {
                adapter = this
                dslAdapterStatusItem.itemEnableRetry = false
                setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_LOADING)
            }
        }

        if (autoEngraveModel.engravePendingData.value == null) {
            finish()
        } else {
            autoEngraveModel.engravePendingData.observeOnce { data ->
                data?.let {
                    _engraveData = it
                    renderLayout(adapter, data)

                    if (needAutoStart()) {
                        checkStartEngrave()
                    }
                }
            }
        }

        //监听
        autoEngraveModel.autoEngraveTaskOnceData.observe(
            this,
            allowBackward = false
        ) { autoEngraveTask ->
            autoEngraveTask?.let {
                adapter?.updateItem {
                    if (it is AutoEngraveItem) {
                        it.itemAutoEngraveTask = autoEngraveTask
                        true
                    } else {
                        false
                    }
                }
                if (it.isFinish) {
                    if (it.error == null) {
                        toast("雕刻完成")
                    } else {
                        toast(it.error?.message)
                    }
                    finish()
                }
            }
        }
    }

    /**检查开始雕刻, 比如蓝牙的连接状态*/
    fun checkStartEngrave() {
        if (fscBleApiModel.haveDeviceConnected()) {
            //进入空闲模式
            ExitCmd().enqueue()
            startEngrave()
        } else {
            //无设备连接,先连接设备
            dslPermissions(FscBleApiModel.bluetoothPermissionList()) { allGranted, foreverDenied ->
                if (allGranted) {
                    bluetoothSearchListDialog {
                        connectedDismiss = true
                        onDismissListener = {
                            _delay {
                                checkStartEngrave()
                            }
                        }
                    }
                } else {
                    toast("蓝牙权限被禁用!")
                }
            }
        }
    }

    /**开始雕刻, 创建数据/发送数据/雕刻*/
    fun startEngrave() {
        val data = _engraveData
        if (data is CanvasProjectBean) {
            _autoEngraveTask = autoEngraveModel.startAutoEngrave(taskId, data)
        } else if (data is CanvasProjectItemBean) {
            _autoEngraveTask = autoEngraveModel.startAutoEngrave(taskId, listOf(data))
        }
    }

    /**是否需要自动开始*/
    fun needAutoStart(): Boolean {
        val data = _engraveData
        if (data is CanvasProjectBean) {
            if (data._debug == true) {
                //调试模式下, 不自动开始
                return false
            }
        } else if (data is CanvasProjectItemBean) {
            if (data._debug == true) {
                //调试模式下, 不自动开始
                return false
            }
        }
        return true
    }

    /**渲染界面*/
    fun renderLayout(adapter: DslAdapter?, data: CanvasOpenDataType) {
        adapter?.render {
            clearAllItems()
            AutoEngraveItem()() {
                var bitmap: Bitmap? = null
                if (data is CanvasProjectBean) {
                    itemShowName = data.file_name ?: "Untitled"
                    bitmap = data.preview_img?.toBitmapOfBase64()
                } else if (data is CanvasProjectItemBean) {
                    itemShowName = data.name ?: data.mtype.toTypeNameString()
                    bitmap = GraphicsHelper.parseRenderItemFrom(data)?.getEngraveBitmap()
                }
                itemDrawable = bitmap?.toDrawable(resources) ?: getAppIcon()

                itemPauseAction = { isPause ->
                    if (isPause) {
                        engraveModel.continueEngrave()
                    } else {
                        engraveModel.pauseEngrave()
                    }
                }
                itemStopAction = {
                    engraveModel.stopEngrave()
                    ExitCmd().enqueue()
                    finish()
                }
            }
        }
    }
}