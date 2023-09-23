package com.angcyo.usb.storage

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.angcyo.DslFHelper
import com.angcyo.base.dslFHelper
import com.angcyo.core.R
import com.angcyo.core.fragment.BaseFragment
import com.angcyo.library.annotation.DSL
import com.angcyo.widget.layout.touch.TouchBackLayout
import me.jahnen.libaums.core.fs.UsbFile

/**
 * USB文件夹选择
 *
 * [com.angcyo.core.component.FileSelectorFragment]
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/09/23
 */
class UsbStorageFolderSelectorFragment : BaseFragment() {

    val usbStorageFolderSelectorHelper = UsbStorageFolderSelectorHelper().apply {
        removeThisAction = {
            removeFragment()
        }
    }

    init {
        fragmentLayoutId = R.layout.lib_file_selector_fragment
    }

    override fun canSwipeBack(): Boolean {
        return false
    }

    override fun onBackPressed(): Boolean {
        usbStorageFolderSelectorHelper.onBackPressed()
        return false
    }

    override fun onFragmentFirstShow(bundle: Bundle?) {
        super.onFragmentFirstShow(bundle)
        usbStorageFolderSelectorHelper.firstLoad()
    }

    /**
     * 调用此方法, 配置参数
     * */
    fun usbSelectorConfig(config: UsbSelectorConfig.() -> Unit): UsbStorageFolderSelectorFragment {
        usbStorageFolderSelectorHelper.usbSelectorConfig.config()
        return this
    }

    override fun initBaseView(savedInstanceState: Bundle?) {
        super.initBaseView(savedInstanceState)

        usbStorageFolderSelectorHelper.init(_vh)

        //半屏效果
        _vh.v<TouchBackLayout>(R.id.lib_touch_back_layout)?.apply {
            enableTouchBack = true
            offsetScrollTop = (resources.displayMetrics.heightPixels) / 2

            onTouchBackListener = object : TouchBackLayout.OnTouchBackListener {
                override fun onTouchBackListener(
                    layout: TouchBackLayout,
                    oldScrollY: Int,
                    scrollY: Int,
                    maxScrollY: Int
                ) {
                    if (scrollY >= maxScrollY) {
                        removeFragment()
                    }
                }
            }
        }
    }

    /**移除界面*/
    private fun removeFragment() {
        dslFHelper {
            noAnim()
            remove(this@UsbStorageFolderSelectorFragment)
        }
    }
}

/**DSL
 * [com.angcyo.component.ResultKtx.getFile]
 * */
@DSL
fun Fragment.usbFolderSelector(
    root: UsbFile?,
    config: UsbSelectorConfig.() -> Unit = {},
    onResult: (UsbFile?) -> Unit = {}
) {
    dslFHelper {
        usbFolderSelector(root, config, onResult)
    }
}

/**文件选择
 * [com.angcyo.component.ResultKtx.getFile]
 * */
@DSL
fun DslFHelper.usbFolderSelector(
    root: UsbFile?,
    config: UsbSelectorConfig.() -> Unit = {},
    onResult: (UsbFile?) -> Unit = {}
) {
    noAnim()
    show(UsbStorageFolderSelectorFragment().apply {
        usbSelectorConfig {
            rootDirectory = root
            config()
            onUsbFileSelector = onResult
        }
    })
}