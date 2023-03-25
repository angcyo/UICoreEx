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
import com.angcyo.core.vmApp
import com.angcyo.dsladapter.DslAdapter
import com.angcyo.dsladapter.DslAdapterStatusItem
import com.angcyo.dsladapter.updateItem
import com.angcyo.engrave.ble.BluetoothSearchHelper
import com.angcyo.engrave.model.AutoEngraveModel
import com.angcyo.engrave.model.EngraveModel
import com.angcyo.library.component._delay
import com.angcyo.library.ex.toBitmapOfBase64
import com.angcyo.library.ex.uuid
import com.angcyo.library.getAppIcon
import com.angcyo.library.toastQQ
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
                data != null
            }
        }

        //监听
        autoEngraveModel.autoEngraveTaskOnceData.observe(
            this,
            allowBackward = false
        ) { autoEngraveTask ->
            autoEngraveTask?.let { task ->
                adapter?.updateItem { item ->
                    if (item is AutoEngraveItem) {
                        item.itemAutoEngraveTask = autoEngraveTask
                        true
                    } else {
                        false
                    }
                }
                if (task.isFinish) {
                    if (task.error == null) {
                        toastQQ("雕刻完成")
                    } else {
                        toastQQ(task.error?.message)
                    }
                    finish()
                }
            }
        }
    }

    /**检查开始雕刻, 比如蓝牙的连接状态*/
    fun checkStartEngrave() {
        if (fscBleApiModel.haveDeviceConnected()) {
            startEngrave()
        } else {
            //无设备连接,先连接设备
            BluetoothSearchHelper.checkAndSearchDevice(fragmentActivity = this) {
                onDismissListener = {
                    _delay {
                        checkStartEngrave()
                    }
                }
            }
        }
    }

    /**开始雕刻, 创建数据/发送数据/雕刻*/
    fun startEngrave() {
        autoEngraveModel.startEngrave(taskId, _engraveData)
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
                    bitmap = GraphicsHelper.parseRenderItemFrom(data, null)?.getEngraveBitmap()
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