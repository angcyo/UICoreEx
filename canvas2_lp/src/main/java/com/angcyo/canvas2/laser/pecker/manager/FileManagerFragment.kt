package com.angcyo.canvas2.laser.pecker.manager

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.ViewGroup
import com.angcyo.bluetooth.fsc.WaitReceivePacket
import com.angcyo.bluetooth.fsc.enqueue
import com.angcyo.bluetooth.fsc.laserpacker.command.FileModeCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.QueryCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.QueryCmd.Companion.TYPE_SD
import com.angcyo.bluetooth.fsc.laserpacker.command.QueryCmd.Companion.TYPE_USB
import com.angcyo.bluetooth.fsc.laserpacker.parse.FileTransferParser
import com.angcyo.bluetooth.fsc.laserpacker.parse.listenerFileList
import com.angcyo.bluetooth.fsc.parse
import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas2.laser.pecker.BuildConfig
import com.angcyo.canvas2.laser.pecker.IEngraveRenderFragment
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.engrave.BaseFlowLayoutHelper
import com.angcyo.canvas2.laser.pecker.engrave.SingleFlowLayoutHelper
import com.angcyo.canvas2.laser.pecker.manager.dslitem.LpbFileItem
import com.angcyo.core.fragment.BaseDslFragment
import com.angcyo.core.showIn
import com.angcyo.dialog.itemsDialog
import com.angcyo.dialog.normalDialog
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
                        startRefresh()
                    }
                    error?.let { toast(it.message) }
                }
            }
        })
    }

    /**删除设备所有记录*/
    private fun deleteHistory(name: String, action: BooleanAction) {
        engraveLoadingAsyncTimeout({
            syncSingle { countDownLatch ->
                FileModeCmd.deleteHistory(name, currentFileType.toByte())
                    .enqueue { bean, error ->
                        countDownLatch.countDown()
                        if (bean?.parse<FileTransferParser>()?.isFileDeleteSuccess() == true) {
                            toast(_string(R.string.delete_history_succeed))
                            if (currentFileType == TYPE_SD) {
                                sdNameList?.remove(name)
                                if (sdNameList.isNullOrEmpty()) {
                                    showRightDeleteIcoView(false)
                                    _adapter.toEmpty()
                                }
                            } else {
                                usbNameList?.remove(name)
                                if (usbNameList.isNullOrEmpty()) {
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
        if (currentFileType == TYPE_SD) {
            loadSdFileList()
        } else if (currentFileType == TYPE_USB) {
            loadUsbFileList()
        }
    }

    private var sdReceive: WaitReceivePacket? = null
    private var usbReceive: WaitReceivePacket? = null

    private var sdNameList: MutableList<String>? = null
    private var usbNameList: MutableList<String>? = null

    private fun loadSdFileList() {
        if (sdNameList != null) {
            renderFileListLayout(TYPE_SD, sdNameList)
            return
        }
        sdReceive?.cancel()
        usbReceive?.cancel()
        sdReceive = listenerFileList(this) { parser, error ->
            sdNameList = parser?.nameList?.toMutableList()
            if (currentFileType == TYPE_SD) {
                if (error != null) {
                    _adapter.toError(error)
                } else {
                    renderFileListLayout(TYPE_SD, parser?.nameList)
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
        if (usbNameList != null) {
            renderFileListLayout(TYPE_USB, usbNameList)
            return
        }
        sdReceive?.cancel()
        usbReceive?.cancel()
        usbReceive = listenerFileList(this) { parser, error ->
            usbNameList = parser?.nameList?.toMutableList()
            if (currentFileType == TYPE_USB) {
                if (error != null) {
                    _adapter.toError(error)
                } else {
                    renderFileListLayout(TYPE_USB, parser?.nameList)
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
    private fun renderFileListLayout(mount: Int, nameList: List<String>?) {
        showRightDeleteIcoView(!nameList.isNullOrEmpty())
        val label =
            if (mount == TYPE_SD) _string(R.string.sd_card_file_title) else _string(R.string.usb_file_title)
        if (nameList.isNullOrEmpty()) {
            _adapter.toEmpty()
            _vh.tv(R.id.filter_text_view)?.text = label
        } else {
            _vh.tv(R.id.filter_text_view)?.text = "${label}(${nameList.size()})"
            renderDslAdapter(true) {
                nameList.forEach { name ->
                    LpbFileItem()() {
                        itemFileName = name
                        itemPreviewAction = {
                            startPreview(itemFileName, mount)
                        }
                        itemEngraveAction = {
                            startEngrave(itemFileName, mount)
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
                                                deleteHistory(name) {
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

    private fun startPreview(fileName: String?, mount: Int) {
        fileName ?: return
        if (BuildConfig.BUILD_TYPE.isDebugType()) {
            flowLayoutHelper._startPreview(this)
        } else {
            flowLayoutHelper.startPreview(this)
        }
    }

    private fun startEngrave(fileName: String?, mount: Int) {
        fileName ?: return
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