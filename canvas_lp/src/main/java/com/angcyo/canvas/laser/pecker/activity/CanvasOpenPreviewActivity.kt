package com.angcyo.canvas.laser.pecker.activity

import android.graphics.Color
import android.os.Bundle
import androidx.core.graphics.drawable.toDrawable
import com.angcyo.activity.BaseAppCompatActivity
import com.angcyo.base.dslFHelper
import com.angcyo.canvas.graphics.toGCodeItemData
import com.angcyo.canvas.graphics.toSvgItemData
import com.angcyo.canvas.laser.pecker.R
import com.angcyo.canvas.laser.pecker.loadingAsync
import com.angcyo.canvas.laser.pecker.mode.CanvasOpenModel
import com.angcyo.canvas.laser.pecker.toBlackWhiteBitmapItemData
import com.angcyo.canvas.laser.pecker.toCanvasDataBean
import com.angcyo.canvas.utils.*
import com.angcyo.core.vmApp
import com.angcyo.dsladapter.DslAdapter
import com.angcyo.dsladapter.DslAdapterStatusItem
import com.angcyo.dsladapter.updateAdapterState
import com.angcyo.engrave.firmware.FirmwareUpdateFragment
import com.angcyo.gcode.GCodeHelper
import com.angcyo.getData
import com.angcyo.http.rx.doBack
import com.angcyo.http.rx.doMain
import com.angcyo.library.component.FontManager
import com.angcyo.library.component.FontManager.toTypeface
import com.angcyo.library.component._delay
import com.angcyo.library.ex.*
import com.angcyo.library.getAppIcon
import com.angcyo.putData
import com.angcyo.widget.recycler.renderDslAdapter

/**
 * 文件打开的预览界面
 * [com.angcyo.uicore.activity.CanvasOpenActivity]
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/10/12
 */
class CanvasOpenPreviewActivity : BaseAppCompatActivity() {

    init {
        activityLayoutId = R.layout.activity_open_preview_layout
    }

    /**需要打开的文件路径*/
    var openFilePath: String? = null

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

        doBack {
            val path: String? = getData()
            openFilePath = path
            if (path != null && path.isFileExist()) {
                if (!handleFilePath(adapter, path)) {
                    //不支持
                    openFilePath = null
                }
            }

            //support
            if (openFilePath == null) {
                adapter?.updateAdapterState(IllegalStateException("not support!"))
            }
        }
    }

    /**处理文件路径*/
    fun handleFilePath(adapter: DslAdapter?, path: String): Boolean {
        val canvasOpenModel = vmApp<CanvasOpenModel>()
        if (path.endsWith(CanvasConstant.PROJECT_EXT)) {
            //工程文件
            val text = path.file().readText()
            val canvasBean = text?.toCanvasDataBean()

            if (canvasBean != null) {
                adapter?.render {
                    clearAllItems()
                    CanvasOpenPreviewItem()() {
                        itemFilePath = path
                        itemShowName = canvasBean.file_name
                        itemDrawable =
                            canvasBean.preview_img?.toBitmapOfBase64()?.toDrawable(resources)
                                ?: getAppIcon()

                        openAction = {
                            canvasOpenModel.open(this@CanvasOpenPreviewActivity, canvasBean)
                            finish()
                        }

                        cancelAction = {
                            finish()
                        }
                    }
                }
                return true
            }
        } else if (path.endsWith(CanvasConstant.GCODE_EXT) ||
            (path.endsWith(CanvasConstant.TXT_EXT) && path.file().readText()
                ?.isGCodeContent() == true)
        ) {
            //gcode
            adapter?.render {
                clearAllItems()
                CanvasOpenPreviewItem()() {
                    itemFilePath = path
                    val text = path.file().readText()
                    itemDrawable = GCodeHelper.parseGCode(text)

                    openAction = {
                        val itemData = text.toGCodeItemData()
                        canvasOpenModel.open(this@CanvasOpenPreviewActivity, itemData)
                        finish()
                    }

                    cancelAction = {
                        finish()
                    }
                }
            }
            return true
        } else if (path.endsWith(CanvasConstant.SVG_EXT) ||
            (path.endsWith(CanvasConstant.TXT_EXT) && path.file().readText()
                ?.isSvgContent() == true)
        ) {
            //svg
            adapter?.render {
                clearAllItems()
                CanvasOpenPreviewItem()() {
                    itemFilePath = path
                    val text = path.file().readText()
                    itemDrawable = parseSvg(text)

                    openAction = {
                        val itemData = text.toSvgItemData()
                        canvasOpenModel.open(this@CanvasOpenPreviewActivity, itemData)
                        finish()
                    }

                    cancelAction = {
                        finish()
                    }
                }
            }
            return true
        } else if (path.endsWith(FirmwareUpdateFragment.FIRMWARE_EXT)) {
            //固件升级
            doMain {
                dslFHelper {
                    restore(FirmwareUpdateFragment::class) {
                        putData(path)
                    }
                }
            }
            return true
        } else if (path.isFontType()) {
            //字体
            adapter?.render {
                clearAllItems()
                CanvasOpenPreviewItem()() {
                    itemFilePath = path
                    itemTypeface = path.toTypeface()

                    //导入字体
                    openAction = {
                        val typefaceInfo = FontManager.importCustomFont(path)
                        if (typefaceInfo == null) {
                            //导入失败
                            updateAdapterState(IllegalStateException(_string(R.string.font_import_fail)))
                        } else {
                            dslAdapterStatusItem.onBindStateLayout = { itemHolder, state ->
                                if (state == DslAdapterStatusItem.ADAPTER_STATUS_ERROR) {
                                    itemHolder.img(R.id.lib_image_view)?.apply {
                                        setImageResource(R.drawable.lib_ic_succeed)
                                        setWidthHeight(80 * dpi, 80 * dpi)
                                    }
                                }
                            }
                            //导入成功
                            updateAdapterState(IllegalStateException(_string(R.string.font_import_success)))
                            _delay(600) {
                                finish()
                            }
                        }
                    }

                    cancelAction = {
                        finish()
                    }
                }
            }
            return true
        } else if (path.isImageType()) {
            //图片
            adapter?.render {
                clearAllItems()
                CanvasOpenPreviewItem()() {
                    val bitmap = path.toBitmap()
                    itemFilePath = path
                    itemDrawable = bitmap?.toDrawable(resources)

                    openAction = {
                        this@CanvasOpenPreviewActivity.loadingAsync({
                            bitmap.toBlackWhiteBitmapItemData()
                        }) {
                            canvasOpenModel.open(this@CanvasOpenPreviewActivity, it)
                            finish()
                        }
                    }

                    cancelAction = {
                        finish()
                    }
                }
            }
            return true
        }
        return false
    }

}