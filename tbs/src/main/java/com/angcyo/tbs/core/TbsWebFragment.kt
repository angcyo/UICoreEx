package com.angcyo.tbs.core

import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import com.angcyo.base.dslAHelper
import com.angcyo.library.L
import com.angcyo.library.component.DslNotify
import com.angcyo.library.ex.*
import com.angcyo.library.model.WebConfig
import com.angcyo.tbs.DslTbs
import com.angcyo.tbs.R
import com.angcyo.tbs.TbsImagePager
import com.angcyo.tbs.core.inner.TbsWebView
import com.angcyo.web.core.BaseWebFragment
import com.angcyo.web.core.DslBaseWebMenuItem
import com.angcyo.widget.span.span
import com.tencent.smtt.sdk.QbSdk
import com.tencent.smtt.sdk.TbsReaderView

/**
 * file:///android_asset/webpage/fileChooser.html
 *
 * TBS内核调试界面
 * https://debugtbs.qq.com/
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/03/01
 */
open class TbsWebFragment : BaseWebFragment() {

    init {
        _configMenuDialogAdapter = { dialog ->
            if (isDebug()) {
                DslBaseWebMenuItem()() {
                    menuText = "X5内核测试"
                    menuIcon = R.drawable.tbs_ic_x5
                    itemClick = {
                        dialog?.dismiss()
                        _tbsWebView?.loadUrl(WebConfig.DEBUG_TBS_URL)
                    }
                }
            }
        }
    }

    //<editor-fold desc="根据不同的类型, 填充不同的布局">

    var _tbsWebView: TbsWebView? = null

    /**追加[TbsWebView], 用于打开网页*/
    override fun attachWebView(url: String?, data: String?, parent: ViewGroup?) {
        super.attachWebView(url, data, parent)

        val webView = TbsWebView(fContext())
        webView.apply {
            id = R.id.tbs_web_view
            _tbsWebView = this
            onInitTbsWebView(this)

            //标题回调
            receivedTitleAction = {
                receivedTitle(it)
                updateHost(_tbsWebView?.originalUrl)
            }

            //进度回调
            progressChangedAction = { url, progress ->
                progressChanged(url, progress)
            }

            //下载回调
            downloadAction = { url, userAgent, contentDisposition, mime, length ->
                download(url, userAgent, contentDisposition, mime, length)
            }

            //打开其他应用回调
            openAppAction = { url, activityInfo, appBean ->
                openApp(url, activityInfo, appBean)
            }

            //选择文件回调
            fileChooseAction = {
                fileChoose(it)
            }

            //注入图片预览回调
            TbsImagePager.register(this@TbsWebFragment, this)

            //加载url
            if (data.isNullOrEmpty()) {
                loadUrl(url)
            } else {
                loadDataWithBaseURL2(data)
            }
        }

        parent?.addView(webView, -1, -1)
    }

    /**[attachTbsWebView]*/
    open fun onInitTbsWebView(webView: TbsWebView) {

    }

    override fun updateHost(url: String?) {
        val host = url?.toUri()?.host

        _vh.tv(R.id.lib_host_tip_view)?.text = span {
            if (!host.isNullOrEmpty()) {
                append("网页由 $host 提供")

                if (DslTbs.isX5Core) {
                    appendln()
                    drawable {
                        backgroundDrawable =
                            _drawable(DslNotify.DEFAULT_NOTIFY_ICON)?.setBounds(18 * dpi, 18 * dpi)
                    }
                    append("腾讯x5内核支持")
                }
            }
        }
    }

    override fun attachFileReaderView(path: String, parent: ViewGroup?): Boolean {
        val fileExt = path.ext()
        val readerView = TbsReaderView(fContext()) { actionType, args, result ->
            hideLoadingView()
            L.d("Tbs type:$actionType args:$args result:$result")
        }

        //如果tbs支持打开文件, 一般是文档格式
        if (DslTbs.canOpenFileTbs(readerView, fileExt)) {
            attachTbsReaderView(readerView, path)
            return true
        }
        return false
    }

    override fun attachVideoView(uri: Uri, parent: ViewGroup?) {
        if (DslTbs.canUseTbsPlayer()) {
            DslTbs.openVideo(fContext(), uri.loadUrl()!!)
            dslAHelper {
                finish()
            }
        } else {
            super.attachVideoView(uri, parent)
        }
    }

    var _tbsReaderView: TbsReaderView? = null

    /**追加[TbsReaderView], 用于打开文档格式*/
    open fun attachTbsReaderView(
        readerView: TbsReaderView,
        path: String,
        parent: ViewGroup? = wrapLayout
    ) {
        val extName = path.ext()
        val param = Bundle()

        //param.putString("fileExt", extName)
        param.putString(TbsReaderView.KEY_FILE_PATH, path)
        param.putString(TbsReaderView.KEY_TEMP_PATH, context.externalCacheDir?.absolutePath)

        //默认不设置，是全屏dialog显示文件内容,
        //param.putInt("windowType",2);
        //设置windowType = 2，进入view显示文件内容, 文件内容会挂到设置的layout上。
        //FILE_READER_WINDOW_TYPE_DEFAULT 全屏样式, 自己的标题栏会无法显示.
        //param.putInt("windowType", TbsFileInterfaceImpl.FILE_READER_WINDOW_TYPE_DEFAULT)

        readerView.apply {
            _tbsReaderView = this
            if (preOpen(extName, false)) {
                openFile(param)
            }
        }

        parent?.addView(readerView, -1, -1)

//        //新接口方式, 都会带有默认的标题栏
//        TbsFileInterfaceImpl.getInstance()
//            .openFileReader(fContext(), param, { actionType, args, result ->
//                hideLoadingView()
//
//                L.d("Tbs type:$actionType args:$args result:$result")
//
//                if (ITbsReader.OPEN_FILEREADER_PLUGIN_SUCCESS == actionType) {
//                    L.w("Tbs plugin success")
//                } else if (ITbsReader.OPEN_FILEREADER_PLUGIN_FAILED == actionType) {
//                    L.w("Tbs plugin failed")
//                }
//            }, parent as FrameLayout)
    }


    //</editor-fold desc="根据不同的类型, 填充不同的布局">

    //<editor-fold desc="生命周期操作">

    override fun onFragmentShow(bundle: Bundle?) {
        super.onFragmentShow(bundle)
        _tbsWebView?.resumeTimers()
    }

    override fun onFragmentHide() {
        super.onFragmentHide()
        _tbsWebView?.pauseTimers()
    }

    override fun onDestroy() {
        super.onDestroy()
        _tbsWebView?.destroy()
        _tbsReaderView?.onStop()

        //TbsFileInterfaceImpl.getInstance().closeFileReader()
        QbSdk.closeFileReader(fContext())
        QbSdk.clear(fContext())
    }

    //</editor-fold desc="生命周期操作">

    override fun canGoBack(): Boolean = _tbsWebView?.canGoBack() == true

    override fun goBack() {
        _tbsWebView?.goBack()
    }

    override fun getWebTitle(): CharSequence? = _tbsWebView?.title
    override fun getLoadUrl(): String? = _tbsWebView?.url

    override fun getUserAgentString(): String? = _tbsWebView?.settings?.userAgentString

    override fun loadUrl(url: String?) {
        _tbsWebView?.loadUrl(url)
    }

    override fun fileChooseResult(files: Array<Uri?>?) {
        _tbsWebView?.onReceiveValue(files)
    }

}