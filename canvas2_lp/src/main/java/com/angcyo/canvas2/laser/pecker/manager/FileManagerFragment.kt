package com.angcyo.canvas2.laser.pecker.manager

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.ViewGroup
import com.angcyo.bluetooth.fsc.WaitReceivePacket
import com.angcyo.bluetooth.fsc.enqueue
import com.angcyo.bluetooth.fsc.laserpacker.DeviceStateModel
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.bean.FileIndexBean
import com.angcyo.bluetooth.fsc.laserpacker.command.FileModeCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.QueryCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.QueryCmd.Companion.TYPE_SD
import com.angcyo.bluetooth.fsc.laserpacker.command.QueryCmd.Companion.TYPE_USB
import com.angcyo.bluetooth.fsc.laserpacker.parse.FileTransferParser
import com.angcyo.bluetooth.fsc.laserpacker.parse.NoDeviceException
import com.angcyo.bluetooth.fsc.laserpacker.parse.listenerFileList
import com.angcyo.bluetooth.fsc.parse
import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas2.laser.pecker.BuildConfig
import com.angcyo.canvas2.laser.pecker.IEngraveRenderFragment
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.engrave.BaseFlowLayoutHelper
import com.angcyo.canvas2.laser.pecker.engrave.SingleFlowInfo
import com.angcyo.canvas2.laser.pecker.engrave.SingleFlowLayoutHelper
import com.angcyo.canvas2.laser.pecker.manager.dslitem.LpbFileItem
import com.angcyo.core.fragment.BaseDslFragment
import com.angcyo.core.showIn
import com.angcyo.core.vmApp
import com.angcyo.dialog.itemsDialog
import com.angcyo.dialog.normalDialog
import com.angcyo.dsladapter.data.updateAdapterState
import com.angcyo.dsladapter.toEmpty
import com.angcyo.dsladapter.toError
import com.angcyo.fragment.AbsLifecycleFragment
import com.angcyo.item.component.initSearchAdapterFilter
import com.angcyo.laserpacker.device.engraveLoadingAsyncTimeout
import com.angcyo.library.ex.BooleanAction
import com.angcyo.library.ex._string
import com.angcyo.library.ex.getColor
import com.angcyo.library.ex.gone
import com.angcyo.library.ex.isDebugType
import com.angcyo.library.ex.size
import com.angcyo.library.ex.syncSingle
import com.angcyo.library.toast

/**
 * 文件管理
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/08/03
 */
class FileManagerFragment : BaseDslFragment(), IEngraveRenderFragment {

    var currentFileType: Int = TYPE_SD

    init {
        fragmentTitle = _string(R.string.file_manager_title)
        fragmentConfig.isLightStyle = true
        fragmentConfig.showTitleLineView = true
        fragmentConfig.fragmentBackgroundDrawable =
            ColorDrawable(getColor(R.color.lib_theme_white_color))

        enableRefresh = true

        titleLayoutId = R.layout.file_manager_title_layout
    }

    override fun initBaseView(savedInstanceState: Bundle?) {
        super.initBaseView(savedInstanceState)
        initSearchAdapterFilter(_string(R.string.ui_search))

        _vh.click(R.id.filter_button) {
            it.context.itemsDialog {
                addDialogItem {
                    itemText = _string(R.string.sd_card_file_title)
                    itemClick = {
                        if (currentFileType != TYPE_SD) {
                            currentFileType = TYPE_SD
                            loadSdFileList()
                        }
                    }
                }
                addDialogItem {
                    itemText = _string(R.string.usb_file_title)
                    itemClick = {
                        if (currentFileType != TYPE_USB) {
                            currentFileType = TYPE_USB
                            loadUsbFileList()
                        }
                    }
                }
            }
        }

        //
        appendRightItem(ico = R.drawable.canvas_delete_ico, action = {
            gone()
        }) {
            fContext().normalDialog {
                dialogTitle = _string(R.string.engrave_warn)
                dialogMessage = _string(R.string.canvas_delete_project_tip)
                positiveButton { dialog, dialogViewHolder ->
                    dialog.dismiss()
                    deleteAllHistory()
                }
            }
        }
    }

    /**是否显示右边删除按钮*/
    fun showRightDeleteIcoView(visible: Boolean = true) {
        rightControl()?.goneIndex(0, !visible)
    }

    /**删除设备所有记录*/
    private fun deleteAllHistory() {
        engraveLoadingAsyncTimeout({
            syncSingle { countDownLatch ->
                FileModeCmd.deleteAllHistory(currentFileType.toByte()).enqueue { bean, error ->
                    countDownLatch.countDown()
                    if (bean?.parse<FileTransferParser>()?.isFileDeleteSuccess() == true) {
                        toast(_string(R.string.delete_history_succeed))
                        showRightDeleteIcoView(false)
                        if (currentFileType == TYPE_SD) {
                            sdList = null
                        } else {
                            usbList = null
                        }
                        startRefresh()
                    }
                    error?.let { toast(it.message) }
                }
            }
        })
    }

    /**删除设备所有记录*/
    private fun deleteHistory(fileBean: FileIndexBean, action: BooleanAction) {
        engraveLoadingAsyncTimeout({
            syncSingle { countDownLatch ->
                FileModeCmd.deleteHistory(fileBean.name!!, currentFileType.toByte())
                    .enqueue { bean, error ->
                        countDownLatch.countDown()
                        if (bean?.parse<FileTransferParser>()?.isFileDeleteSuccess() == true) {
                            toast(_string(R.string.delete_history_succeed))
                            if (currentFileType == TYPE_SD) {
                                sdList?.remove(fileBean)
                                if (sdList.isNullOrEmpty()) {
                                    showRightDeleteIcoView(false)
                                    _adapter.toEmpty()
                                }
                            } else {
                                usbList?.remove(fileBean)
                                if (usbList.isNullOrEmpty()) {
                                    showRightDeleteIcoView(false)
                                    _adapter.toEmpty()
                                }
                            }
                            action(true)
                        } else {
                            action(false)
                        }
                        error?.let { toast(it.message) }
                    }
            }
        })
    }

    override fun onLoadData() {
        super.onLoadData()

        if (vmApp<DeviceStateModel>().isDeviceConnect()) {
            if (vmApp<LaserPeckerModel>().isHaveUsbProduct()) {
                if (currentFileType == TYPE_SD) {
                    loadSdFileList()
                } else if (currentFileType == TYPE_USB) {
                    loadUsbFileList()
                } else {
                    finishRefresh()
                    _adapter.updateAdapterState()
                }
            } else {
                finishRefresh()
                _adapter.updateAdapterState()
            }
        } else {
            finishRefresh()
            _adapter.updateAdapterState(error = NoDeviceException())
        }
    }

    private var sdReceive: WaitReceivePacket? = null
    private var usbReceive: WaitReceivePacket? = null

    private var sdList: MutableList<FileIndexBean>? = null
    private var usbList: MutableList<FileIndexBean>? = null

    private fun loadSdFileList() {
        if (!sdList.isNullOrEmpty()) {
            finishRefresh()
            renderFileListLayout(TYPE_SD, sdList)
            return
        }
        sdReceive?.cancel()
        usbReceive?.cancel()
        sdReceive = listenerFileList(this) { parser, error ->
            parser?.apply {
                val list = mutableListOf<FileIndexBean>()
                nameList?.forEachIndexed { index, name ->
                    list.add(FileIndexBean(indexList?.getOrNull(index) ?: 0, name, TYPE_SD))
                }
                sdList = list
            }
            if (currentFileType == TYPE_SD) {
                finishRefresh()
                if (error != null) {
                    _adapter.toError(error)
                } else {
                    renderFileListLayout(TYPE_SD, sdList)
                }
            }
        }
        QueryCmd.fileSdNameList.enqueue { bean, error ->
            if (error != null) {
                _adapter.toError(error)
            }
        }
    }

    private fun loadUsbFileList() {
        if (!usbList.isNullOrEmpty()) {
            finishRefresh()
            renderFileListLayout(TYPE_USB, usbList)
            return
        }
        sdReceive?.cancel()
        usbReceive?.cancel()
        usbReceive = listenerFileList(this) { parser, error ->
            parser?.apply {
                val list = mutableListOf<FileIndexBean>()
                nameList?.forEachIndexed { index, name ->
                    list.add(FileIndexBean(indexList?.getOrNull(index) ?: 0, name, TYPE_USB))
                }
                usbList = list
            }

            if (currentFileType == TYPE_USB) {
                finishRefresh()
                if (error != null) {
                    _adapter.toError(error)
                } else {
                    renderFileListLayout(TYPE_USB, usbList)
                }
            }
        }
        QueryCmd.fileUsbNameList.enqueue { bean, error ->
            if (error != null) {
                _adapter.toError(error)
            }
        }
    }

    //---

    /**渲染sd卡文件列表*/
    private fun renderFileListLayout(mount: Int, beanList: List<FileIndexBean>?) {
        showRightDeleteIcoView(!beanList.isNullOrEmpty())
        val label =
            if (mount == TYPE_SD) _string(R.string.sd_card_file_title) else _string(R.string.usb_file_title)
        if (beanList.isNullOrEmpty()) {
            _adapter.toEmpty()
            _vh.tv(R.id.filter_text_view)?.text = label
        } else {
            _vh.tv(R.id.filter_text_view)?.text = "${label}(${beanList.size()})"
            renderDslAdapter(true) {
                beanList.forEach { bean ->
                    LpbFileItem()() {
                        itemFileName = bean.name
                        itemFileIndex = bean.index
                        itemPreviewAction = {
                            startPreview(bean)
                        }
                        itemEngraveAction = {
                            startEngrave(bean)
                        }
                        itemClick = {
                            startPreview(bean)
                        }
                        itemLongClick = {
                            it.context.itemsDialog {
                                addDialogItem {
                                    itemText = _string(R.string.delete_history)
                                    itemClick = {
                                        fContext().normalDialog {
                                            dialogTitle = _string(R.string.engrave_warn)
                                            dialogMessage =
                                                _string(R.string.canvas_delete_project_tip)
                                            positiveButton { dialog, dialogViewHolder ->
                                                dialog.dismiss()
                                                deleteHistory(bean) {
                                                    if (it) {
                                                        render {
                                                            removeAdapterItem()
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            true
                        }
                    }
                }
            }
        }
    }

    private fun startPreview(bean: FileIndexBean?) {
        bean ?: return
        flowLayoutHelper.singleFlowInfo = SingleFlowInfo("flowId-$bean", bean)
        if (BuildConfig.BUILD_TYPE.isDebugType()) {
            flowLayoutHelper._startPreview(this)
        } else {
            flowLayoutHelper.startPreview(this)
        }
    }

    private fun startEngrave(bean: FileIndexBean?) {
        bean ?: return
        flowLayoutHelper.singleFlowInfo = SingleFlowInfo("flowId-${bean.name}", bean)
        flowLayoutHelper.engraveFlow = BaseFlowLayoutHelper.ENGRAVE_FLOW_BEFORE_CONFIG
        flowLayoutHelper.showIn(this, flowLayoutContainer)
    }

    //<editor-fold desc="IEngraveCanvasFragment">

    private val _flowLayoutHelper: SingleFlowLayoutHelper by lazy {
        SingleFlowLayoutHelper()
    }

    override val fragment: AbsLifecycleFragment
        get() = this

    override val flowLayoutHelper: SingleFlowLayoutHelper
        get() = _flowLayoutHelper.apply {
            engraveCanvasFragment = this@FileManagerFragment
        }

    override val renderDelegate: CanvasRenderDelegate?
        get() = null

    override val flowLayoutContainer: ViewGroup?
        get() = _vh.group(R.id.lib_content_overlay_wrap_layout) ?: _vh.itemView as ViewGroup

    override val dangerLayoutContainer: ViewGroup?
        get() = flowLayoutContainer

    //</editor-fold desc="IEngraveCanvasFragment">
}