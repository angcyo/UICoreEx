package com.angcyo.usb.storage

import android.view.View
import android.widget.HorizontalScrollView
import com.angcyo.core.R
import com.angcyo.dsladapter.DslAdapter
import com.angcyo.dsladapter.DslAdapterStatusItem
import com.angcyo.dsladapter.data.loadSingleData2
import com.angcyo.dsladapter.select
import com.angcyo.dsladapter.selector
import com.angcyo.dsladapter.singleModel
import com.angcyo.library.annotation.CallPoint
import com.angcyo.library.ex.Action
import com.angcyo.library.ex.Anim
import com.angcyo.library.ex.doAnimate
import com.angcyo.library.ex.drawWidth
import com.angcyo.library.ex.toString
import com.angcyo.library.ex.withMinValue
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget._rv
import com.angcyo.widget.progress.HSProgressView
import com.angcyo.widget.recycler.initDslAdapter
import me.jahnen.libaums.core.fs.UsbFile

/**
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2023/09/23
 */
class UsbStorageFolderSelectorHelper {

    /**配置*/
    var usbSelectorConfig = UsbSelectorConfig()

    /**需要关闭当前的界面*/
    var removeThisAction: Action? = null

    /**选中的文件item*/
    private var selectorUsbFile: UsbFile? = null

    /**当前的文件*/
    private var currentUsbFile: UsbFile? = null

    /**获取上一层路径*/
    private fun getPrePath(): UsbFile? = currentUsbFile?.parent

    private var scrollView: HorizontalScrollView? = null

    private lateinit var _adapter: DslAdapter
    private lateinit var _vh: DslViewHolder

    @CallPoint
    fun init(vh: DslViewHolder) {
        _vh = vh
        _vh.click(R.id.lib_touch_back_layout) {
            send(null)
        }
        _vh.tv(R.id.current_file_path_view)?.text = usbSelectorConfig.rootDirectory?.absolutePath
        _vh.view(R.id.file_selector_button)?.isEnabled = false

        scrollView = _vh.v(R.id.current_file_path_layout)

        /*上一个路径*/
        _vh.click(R.id.current_file_path_layout) {
            resetPath(getPrePath())
        }
        /*回到app根目录*/
        _vh.click(R.id.file_go_home_view) {
            resetPath(usbSelectorConfig.rootDirectory)
        }
        //选择按钮
        _vh.click(R.id.file_selector_button) {
            //T_.show(selectorFilePath)
            send(selectorUsbFile)
        }

        _vh.rv(R.id.lib_recycler_view)?.apply {
            _adapter = initDslAdapter()
            _adapter.setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_LOADING)
            _adapter.singleModel()

            _adapter.selector().observer {
                onItemChange = { selectorItems, selectorIndexList, _, _ ->
                    selectorUsbFile =
                        (selectorItems.firstOrNull() as? UsbFileSelectorItem)?.itemUsbFile
                    _vh.enable(R.id.file_selector_button, selectorIndexList.isNotEmpty())
                }
            }
        }

        _adapter.onDispatchUpdatesOnce {
            _vh._rv(R.id.lib_recycler_view)?.scrollHelper?.lockScrollToFirst {
                scrollAnim = false
            }
        }

        doShowAnimator()
    }

    /**是否要拦截back操作*/
    fun onBackPressed(): Boolean {
        return if (currentUsbFile == usbSelectorConfig.rootDirectory) {
            //已经是根目录了, 再次返回就是关闭界面
            send(null)
            false
        } else {
            //否则返回上一级
            resetPath(getPrePath())
            true
        }
    }

    fun firstLoad() {
        _vh.post {
            if (usbSelectorConfig.isSelectFolder) {
                selectorUsbFile = usbSelectorConfig.rootDirectory
            }
            loadPath(usbSelectorConfig.rootDirectory, 360)
        }
    }

    //---


    private fun checkEnableSelectorButton() {
        if (usbSelectorConfig.isSelectFolder) {
            _vh.enable(R.id.file_selector_button, selectorUsbFile != null)
        }
    }

    private fun resetPath(path: UsbFile?, force: Boolean = false) {
        path ?: return
        if (!force && _vh.tv(R.id.current_file_path_view)?.text.toString() == path.absolutePath) {
            return
        }
        loadPath(path)
    }

    private fun loadPath(path: UsbFile?, delay: Long = 0L) {
        path ?: return
        _vh.tv(R.id.current_file_path_view)?.text = path.absolutePath

        scrollView?.let {
            it.post {
                val x = it.getChildAt(0).measuredWidth - it.drawWidth
                it.scrollTo(x.withMinValue(0), 0)
            }
        }

        loadFileList(path, delay)
    }

    private fun loadFileList(path: UsbFile, delay: Long = 0L) {
        currentUsbFile = path
        checkEnableSelectorButton()

        _vh.v<HSProgressView>(R.id.lib_progress_view)?.apply {
            visibility = View.VISIBLE
            startAnimator()
        }

        _vh.postDelay(delay) {
            UsbStorageHelper.loadUsbFile(path) {
                renderUsbFileList(it)
            }
        }
    }

    private fun doShowAnimator() {
        _vh.view(R.id.lib_touch_back_layout)?.run {
            doAnimate {
                translationY = this.measuredHeight.toFloat()
                animate().translationY(0f).setDuration(Anim.ANIM_DURATION).start()
            }
        }
    }

    private fun doHideAnimator(onEnd: () -> Unit) {
        _vh.view(R.id.lib_touch_back_layout)?.run {
            if (hasTransientState()) {
                return
            }
            animate()
                .translationY(this.measuredHeight.toFloat())
                .setDuration(Anim.ANIM_DURATION)
                .withEndAction(onEnd)
                .start()
        }
    }

    /**发送返回结果*/
    private fun send(fileItem: UsbFile? = null) {
        doHideAnimator {
            removeThisAction?.invoke()
            usbSelectorConfig.onUsbFileSelector?.invoke(fileItem)
            usbSelectorConfig = UsbSelectorConfig()
        }
    }

    private fun renderUsbFileList(usbFileList: List<UsbFile>?) {
        //文件列表加载返回
        val filterList = usbFileList?.filter {
            if (usbSelectorConfig.isSelectFolder) {
                if (usbSelectorConfig.isSelectFile) {
                    true
                } else {
                    it.isDirectory
                }
            } else {
                true
            }
        }

        _vh.gone(R.id.lib_progress_view)
        _adapter.loadSingleData2<UsbFileSelectorItem>(filterList, 1, Int.MAX_VALUE) { data ->
            itemIsSelected = selectorUsbFile == data

            itemClick = {
                itemUsbFile?.apply {
                    if (itemIsFile(null)) {
                        if (usbSelectorConfig.isSelectFile) {
                            _adapter.select {
                                it == this@loadSingleData2
                            }
                        }
                    } else if (itemIsFolder(null)) {
                        if (usbSelectorConfig.isSelectFolder) {
                            selectorUsbFile = itemUsbFile
                        }
                        resetPath(itemUsbFile)
                    }
                }
            }
        }
    }
}


open class UsbSelectorConfig {

    /**最根的目录*/
    var rootDirectory: UsbFile? = null

    /**是否需要选择文件*/
    var isSelectFile: Boolean = false

    /**是否需要选择文件夹*/
    var isSelectFolder: Boolean = true

    /**选择回调*/
    var onUsbFileSelector: ((UsbFile?) -> Unit)? = null
}