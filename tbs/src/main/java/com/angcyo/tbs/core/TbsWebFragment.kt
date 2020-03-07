package com.angcyo.tbs.core

import android.content.ComponentName
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.angcyo.base.dslAHelper
import com.angcyo.base.dslFHelper
import com.angcyo.core.component.fileSelector
import com.angcyo.core.fragment.BaseTitleFragment
import com.angcyo.dialog.configBottomDialog
import com.angcyo.dialog.dslDialog
import com.angcyo.download.download
import com.angcyo.download.downloadNotify
import com.angcyo.image.dslitem.DslSubSamplingImageItem
import com.angcyo.library.L
import com.angcyo.library.ex.*
import com.angcyo.library.toastQQ
import com.angcyo.media.dslitem.DslTextureVideoItem
import com.angcyo.tablayout.screenWidth
import com.angcyo.tbs.DslTbs
import com.angcyo.tbs.R
import com.angcyo.tbs.core.inner.TbsWeb
import com.angcyo.tbs.core.inner.TbsWebView
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.bar
import com.angcyo.widget.base.*
import com.angcyo.widget.span.span
import com.tencent.tbs.reader.ITbsReader
import com.tencent.tbs.reader.TbsFileInterfaceImpl
import com.tencent.tbs.reader.TbsReaderView

/**
 * file:///android_asset/webpage/fileChooser.html
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/03/01
 */
open class TbsWebFragment : BaseTitleFragment() {
    companion object {
        const val KEY_CONFIG = "key_config"
    }

    init {
        contentLayoutId = R.layout.layout_tbs_web_content
        contentOverlayLayoutId = R.layout.layout_tbs_web_content_overlay
        fragmentTitle = null
    }

    override fun onCreateBackItem(): View? {
        return super.onCreateBackItem()?.apply {
            find<TextView>(R.id.lib_text_view)?.run {
                text = span {
                    drawable {
                        backgroundDrawable =
                            getDrawable(com.angcyo.core.R.drawable.lib_back)
                                .colorFilter(fragmentConfig.titleItemIconColor)
                    }
                }
            }
        }
    }

    var webConfig = TbsWebConfig()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.getParcelable<TbsWebConfig>(TbsWebFragment.KEY_CONFIG)?.run {
            webConfig = this
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _vh.tv(R.id.lib_title_text_view)?.run {
            setWidth(width = screenWidth - 160 * dpi)
            setSingleLineMode()
        }

        //有些网页, 无法回退. 添加强制关闭按钮
        appendLeftItem(ico = R.drawable.tbs_ic_close, action = {
            id = R.id.lib_close_view
            visibility = View.GONE
        }) {
            close()
        }

        val wrapLayout = _vh.group(R.id.tbs_wrap_layout)
        val uri = webConfig.uri

        if (uri == null) {
            toastQQ("数据异常", R.drawable.lib_ic_error)
        } else if (wrapLayout == null) {
            toastQQ("布局异常", R.drawable.lib_ic_error)
        } else {
            val url = uri.toString()
            val path = uri.path
            val mimeType = path.mimeType()

            L.d("TBS:$uri $path $mimeType")

            if (uri.isHttpScheme() || mimeType.isHttpMimeType()) {
                //打开网页
                attachTbsWebView(wrapLayout, url)
            } else if (uri.isFileScheme()) {
                val fileExt = path!!.ext()

                fragmentTitle = path.file().name

                when {
                    //如果tbs支持打开文件, 一般是文档格式
                    DslTbs.canOpenFileTbs(fileExt) -> attachTbsReaderView(wrapLayout, path)
                    mimeType.isVideoMimeType() -> attachVideoView(wrapLayout, uri)
                    mimeType.isImageMimeType() -> attachImageView(wrapLayout, uri)

                    else -> showLoadingView("无法打开\n$uri")
                }
            } else {
                //其他类型
                showLoadingView("无法打开\n$uri")
            }
        }
    }

    //<editor-fold desc="根据不同的类型, 填充不同的布局">

    var _tbsWebView: TbsWebView? = null

    /**追加[TbsWebView], 用于打开网页*/
    open fun attachTbsWebView(parent: ViewGroup, url: String) {
        val webView = TbsWebView(fContext())
        webView.apply {

            id = R.id.tbs_web_view

            _tbsWebView = this

            //标题
            onReceivedTitle = {
                fragmentTitle = it
            }

            //进度
            onProgressChanged = { url, progress ->
                // L.d("$url $progress")
                _vh.bar(R.id.lib_progress_bar)?.setProgress(progress)
                //加载框

                if (progress == 0) {
                    if (fragmentTitle.isNullOrEmpty()) {
                        fragmentTitle = "加载中..."
                    }
                }

                checkCloseView()

                if (progress >= 80) {
                    hideLoadingView()
                }
            }

            //下载
            onDownloadListener = { url, userAgent, contentDisposition, mime, length ->
                fContext().dslDialog {
                    configBottomDialog()
                    dialogLayoutId = R.layout.dialog_tbs_file_download
                    onDialogInitListener = { dialog, dialogViewHolder ->
                        val fileName = TbsWeb.getFileName(url, contentDisposition)
                        dialogViewHolder.tv(R.id.target_url_view)?.text = url
                        dialogViewHolder.tv(R.id.file_name_view)?.text = fileName
                        dialogViewHolder.tv(R.id.file_size_view)?.text =
                            if (length > 0) length.fileSizeString() else "未知大小"
                        dialogViewHolder.tv(R.id.file_type_view)?.text = mime

                        dialogViewHolder.longClick(R.id.target_url_view) {
                            url.copy()
                            toastQQ("下载地址已复制")
                        }

                        dialogViewHolder.click(R.id.download_button) {
                            dialog.dismiss()
                            url.downloadNotify()
                            url.download {
                                onConfigTask = {
                                    it.setFilename(fileName)
                                }
                            }
                        }
                    }
                }
            }

            //打开其他应用
            onOpenAppListener = { url, activityInfo, appBean ->
                fContext().dslDialog {
                    configBottomDialog()
                    dialogLayoutId = R.layout.dialog_tbs_open_app
                    onDialogInitListener = { dialog, dialogViewHolder ->
                        dialogViewHolder.tv(R.id.lib_text_view)?.text = appBean.appName
                        dialogViewHolder.tv(R.id.lib_sub_text_view)?.text = url
                        dialogViewHolder.img(R.id.lib_image_view)?.setImageDrawable(appBean.appIcon)
                        dialogViewHolder.click(R.id.lib_reject_button) {
                            dialog.dismiss()
                        }
                        dialogViewHolder.click(R.id.lib_open_button) {
                            dialog.dismiss()
                            dslAHelper {
                                start(
                                    url.urlIntent(
                                        ComponentName(
                                            activityInfo.packageName,
                                            activityInfo.name
                                        )
                                    )
                                )
                            }
                        }
                    }
                }
            }

            //选择文件
            onFileChooseListener = {
                dslFHelper {
                    fileSelector {
                        it?.run {
                            onFileChooseResult(arrayOf(fileUri))
                        } ?: onFileChooseResult(null)
                    }
                }
            }

            loadUrl(url)
        }

        parent.addView(webView, -1, -1)
    }

    var _tbsReaderView: TbsReaderView? = null

    /**追加[TbsReaderView], 用于打开文档格式*/
    open fun attachTbsReaderView(parent: ViewGroup, path: String) {
        val extName = path.ext()
        val param = Bundle()
        param.putString("fileExt", extName)
        param.putString("filePath", path)

        //默认不设置，是全屏dialog显示文件内容,
        //param.putInt("windowType",2);
        //设置windowType = 2，进入view显示文件内容, 文件内容会挂到设置的layout上。
        //FILE_READER_WINDOW_TYPE_DEFAULT 全屏样式, 自己的标题栏会无法显示.
        param.putInt("windowType", TbsFileInterfaceImpl.FILE_READER_WINDOW_TYPE_DEFAULT)

        //老接口方式, 没有默认的标题栏
        val readerView =
            TbsReaderView(fContext(), TbsReaderView.ReaderCallback { actionType, args, result ->
                hideLoadingView()

                L.d("Tbs type:$actionType args:$args result:$result")

                if (ITbsReader.OPEN_FILEREADER_PLUGIN_SUCCESS == actionType) {
                    L.w("Tbs plugin success")
                } else if (ITbsReader.OPEN_FILEREADER_PLUGIN_FAILED == actionType) {
                    L.w("Tbs plugin failed")
                }
            })

        readerView.apply {
            _tbsReaderView = this
            if (preOpen(extName, false)) {
                openFile(param)
            }
        }

        parent.addView(readerView, -1, -1)

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

    var _dslVideoHolder: DslViewHolder? = null
    var _dslVideoItem: DslTextureVideoItem? = null

    /**加载视频*/
    open fun attachVideoView(parent: ViewGroup, uri: Uri) {
        parent.setBackgroundColor(Color.BLACK)
        hideLoadingView()

        val dslVideoItem = DslTextureVideoItem().apply {
            _dslVideoItem = this

            itemData = uri
            itemVideoUri = uri

            onItemDownloadStart = { itemHolder, task ->
                showLoadingView("下载中...")
            }

            onItemDownloadFinish = { itemHolder, task, cause, error ->
                hideLoadingView()
            }
        }
        _dslVideoHolder = parent.appendDslItem(dslVideoItem)
    }

    var _dslSubSamplingItem: DslSubSamplingImageItem? = null

    /**加载大图*/
    open fun attachImageView(parent: ViewGroup, uri: Uri) {
        parent.setBackgroundColor(Color.BLACK)
        hideLoadingView()

        val dslSubSamplingItem = DslSubSamplingImageItem().apply {
            _dslSubSamplingItem = this

            itemData = uri
            itemLoadUri = uri

            onItemDownloadStart = { itemHolder, task ->
                showLoadingView("下载中...")
            }

            onItemDownloadFinish = { itemHolder, task, cause, error ->
                hideLoadingView()
            }
        }
        _dslVideoHolder = parent.appendDslItem(dslSubSamplingItem)
    }

    //</editor-fold desc="根据不同的类型, 填充不同的布局">

    //<editor-fold desc="生命周期操作">

    override fun onFragmentShow(bundle: Bundle?) {
        super.onFragmentShow(bundle)
    }

    override fun onFragmentHide() {
        super.onFragmentHide()
        _dslVideoItem?.itemViewDetachedToWindow?.invoke(_dslVideoHolder!!, 0)
        _dslSubSamplingItem?.itemViewDetachedToWindow?.invoke(_dslVideoHolder!!, 0)
    }

    override fun onDestroy() {
        super.onDestroy()
        _tbsWebView?.destroy()
        _tbsReaderView?.onStop()
        _dslVideoItem?.itemViewRecycled?.invoke(_dslVideoHolder!!, 0)
        _dslSubSamplingItem?.itemViewRecycled?.invoke(_dslVideoHolder!!, 0)

        TbsFileInterfaceImpl.getInstance().closeFileReader()
    }

    override fun onBackPressed(): Boolean {
        val webView = _tbsWebView
        if (webView != null && webView.canGoBack()) {
            webView.goBack()
            checkCloseView()
            return false
        }
        return true
    }

    //</editor-fold desc="生命周期操作">

    //<editor-fold desc="其他操作">

    /**动态判断是否要显示强制关闭按钮*/
    open fun checkCloseView() {
        val webView = _tbsWebView
        if (webView?.canGoBack() == true) {
            leftControl()?.run {
                visible(R.id.lib_close_view)
            }
        } else {
            leftControl()?.run {
                gone(R.id.lib_close_view)
            }
        }
    }

    open fun close() {
        dslFHelper {
            remove(this@TbsWebFragment)
        }
    }

    fun showLoadingView(tip: CharSequence? = null) {
        _vh.visible(R.id.lib_arc_loading_view)
        _vh.visible(R.id.lib_tip_view, tip != null)
        _vh.tv(R.id.lib_tip_view)?.text = tip
    }

    fun hideLoadingView() {
        _vh.gone(R.id.lib_arc_loading_view)
        _vh.gone(R.id.lib_tip_view)
    }

    //</editor-fold desc="其他操作">

}