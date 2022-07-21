package com.angcyo.engrave

import android.graphics.Bitmap
import android.graphics.Paint
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.command.EngravePreviewCmd
import com.angcyo.canvas.LinePath
import com.angcyo.canvas.core.renderer.SelectGroupGCodeItem
import com.angcyo.canvas.items.PictureShapeItem
import com.angcyo.canvas.items.getHoldData
import com.angcyo.canvas.items.renderer.BaseItemRenderer
import com.angcyo.canvas.utils.*
import com.angcyo.core.component.file.writeTo
import com.angcyo.core.vmApp
import com.angcyo.engrave.data.EngraveDataInfo
import com.angcyo.engrave.data.EngraveReadyDataInfo
import com.angcyo.engrave.model.EngraveModel
import com.angcyo.gcode.GCodeHelper
import com.angcyo.library.component.HawkPropertyValue
import com.angcyo.library.ex.*
import com.angcyo.objectbox.laser.pecker.entity.MaterialEntity
import java.io.File

/**
 * 雕刻助手
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/21
 */
object EngraveHelper {

    /**最后一次预览光功率设置 [0~1f]*/
    var lastPwrProgress: Float by HawkPropertyValue<Any, Float>(0.5f)

    /**最后一次功率*/
    var lastPower: Int by HawkPropertyValue<Any, Int>(100)

    /**最后一次深度*/
    var lastDepth: Int by HawkPropertyValue<Any, Int>(10)

    /**最后一次的物理尺寸, 像素*/
    var lastDiameter: Float by HawkPropertyValue<Any, Float>(300f)

    /**生成一个雕刻需要用到的文件索引*/
    fun generateEngraveIndex(): Int {
        return (System.currentTimeMillis() / 1000).toInt()
    }

    /**创建一个雕刻数据, 不处理雕刻数据, 加快响应速度*/
    fun generateEngraveReadyDataInfo(renderer: BaseItemRenderer<*>?): EngraveReadyDataInfo? {
        var result: EngraveReadyDataInfo? = null
        val item = renderer?.getRendererItem() ?: return result
        result = EngraveReadyDataInfo()
        val dataInfo = EngraveDataInfo()
        result.engraveData = dataInfo

        //打印的文件索引
        dataInfo.index = item.engraveIndex ?: generateEngraveIndex()
        item.engraveIndex = dataInfo.index

        dataInfo.name = item.itemName?.toString()
        result.rendererItemUuid = item.uuid

        val gCodeText = renderer.getGCodeText()
        if (!gCodeText.isNullOrEmpty()) {
            //GCode数据
            dataInfo.dataType = EngraveDataInfo.TYPE_GCODE
            result.optionMode = CanvasConstant.BITMAP_MODE_GCODE //GCode数据使用GCode模式
            return result
        }

        val svgPathList = renderer.getPathList()
        if (!svgPathList.isNullOrEmpty()) {
            //path路径
            dataInfo.dataType = EngraveDataInfo.TYPE_GCODE
            result.optionMode = CanvasConstant.BITMAP_MODE_GCODE //GCode数据使用GCode模式
            return result
        }

        if (item is SelectGroupGCodeItem) {
            dataInfo.dataType = EngraveDataInfo.TYPE_GCODE
            result.optionMode = CanvasConstant.BITMAP_MODE_GCODE //GCode数据使用GCode模式
            return result
        }

        if (item is PictureShapeItem) {
            dataInfo.dataType = EngraveDataInfo.TYPE_GCODE
            result.optionMode = CanvasConstant.BITMAP_MODE_GCODE //GCode数据使用GCode模式
            if (item.paint.style == Paint.Style.STROKE && item.shapePath !is LinePath) {
                //
            } else {
                result.optionSupportModeList = listOf(
                    CanvasConstant.BITMAP_MODE_GREY,
                    CanvasConstant.BITMAP_MODE_BLACK_WHITE,
                    CanvasConstant.BITMAP_MODE_GCODE
                )
            }
            return result
        } else {
            dataInfo.px = LaserPeckerHelper.DEFAULT_PX
            dataInfo.dataType = EngraveDataInfo.TYPE_BITMAP

            result.optionMode = item.getHoldData(CanvasDataHandleOperate.KEY_DATA_MODE)
                ?: CanvasConstant.BITMAP_MODE_GREY

            //px list
            result.optionSupportPxList = LaserPeckerHelper.findProductSupportPxList()
        }
        return result
    }

    /**处理需要打印的雕刻数据, 保存对应的数据等*/
    fun handleEngraveData(
        renderer: BaseItemRenderer<*>?,
        engraveReadyDataInfo: EngraveReadyDataInfo
    ): EngraveReadyDataInfo {
        val item = renderer?.getRendererItem() ?: return engraveReadyDataInfo

        //GCode
        val gCodeText = renderer.getGCodeText()
        if (!gCodeText.isNullOrEmpty()) {
            //GCode数据
            val gCodeFile = CanvasDataHandleOperate.gCodeAdjust(
                gCodeText,
                renderer.getBounds(),
                renderer.rotate
            )
            _handleGCodeEngraveDataInfo(engraveReadyDataInfo, gCodeFile)
            return engraveReadyDataInfo
        }

        //SVG
        val svgPathList = renderer.getPathList()
        if (!svgPathList.isNullOrEmpty()) {
            //path路径
            val gCodeFile = CanvasDataHandleOperate.pathStrokeToGCode(
                svgPathList,
                renderer.getBounds(),
                renderer.rotate
            )
            _handleGCodeEngraveDataInfo(engraveReadyDataInfo, gCodeFile)
            return engraveReadyDataInfo
        }

        //group
        if (item is SelectGroupGCodeItem) {
            //使用bitmap转gcode
            val bitmap = renderer.preview()?.toBitmap() ?: return engraveReadyDataInfo
            var gCodeFile = CanvasDataHandleOperate.bitmapToGCode(bitmap)
            val gCodeString = gCodeFile.readText()
            gCodeFile.deleteSafe()
            if (!gCodeString.isNullOrEmpty()) {
                //GCode数据
                gCodeFile = CanvasDataHandleOperate.gCodeTranslation(
                    gCodeString,
                    renderer.getRotateBounds()
                )
                _handleGCodeEngraveDataInfo(engraveReadyDataInfo, gCodeFile)
                return engraveReadyDataInfo
            }
        }

        //其他
        if (item is PictureShapeItem) {
            if (item.paint.style == Paint.Style.STROKE && item.shapePath !is LinePath) {
                //描边时, 才处理成GCode. 并且不是线段
                val path = item.shapePath
                if (path != null) {
                    val gCodeFile = CanvasDataHandleOperate.pathStrokeToGCode(
                        path,
                        renderer.getRotateBounds(),
                        renderer.rotate
                    )
                    _handleGCodeEngraveDataInfo(engraveReadyDataInfo, gCodeFile)
                    return engraveReadyDataInfo
                }
            } else {
                //填充情况下, 使用bitmap转gcode
                val bitmap = renderer.preview()?.toBitmap() ?: return engraveReadyDataInfo
                //OpenCV.bitmapToGCode(app(), bitmap)
                var gCodeFile = CanvasDataHandleOperate.bitmapToGCode(bitmap)
                val rotate = 0f//renderer.rotate
                val gCodeString = gCodeFile.readText()
                gCodeFile.deleteSafe()
                if (!gCodeString.isNullOrEmpty()) {
                    //GCode数据
                    gCodeFile = CanvasDataHandleOperate.gCodeTranslation(
                        gCodeString,
                        renderer.getRotateBounds()
                    )
                    /*CanvasDataHandleHelper.gCodeAdjust(
                        gCodeString,
                        renderer.getBounds(),
                        rotate
                    )*/ //这里只需要平移GCode即可
                    _handleGCodeEngraveDataInfo(engraveReadyDataInfo, gCodeFile)
                    return engraveReadyDataInfo
                }
            }
        } else {
            //其他方式, 使用图片雕刻
            val bounds = renderer.getRotateBounds()
            val bitmap = renderer.preview()?.toBitmap() ?: return engraveReadyDataInfo

            val x = bounds.left.toInt()
            val y = bounds.top.toInt()

            engraveReadyDataInfo.engraveData?.dataType = EngraveDataInfo.TYPE_BITMAP
            engraveReadyDataInfo.optionBitmap = bitmap
            engraveReadyDataInfo.optionX = x
            engraveReadyDataInfo.optionY = y

            updateBitmapPx(
                engraveReadyDataInfo,
                engraveReadyDataInfo.engraveData?.px ?: LaserPeckerHelper.DEFAULT_PX
            )
        }

        return engraveReadyDataInfo
    }

    /**更新雕刻图片的分辨率
     * [px] 图片需要调整到的分辨率*/
    fun updateBitmapPx(engraveReadyDataInfo: EngraveReadyDataInfo, px: Byte) {
        var bitmap = engraveReadyDataInfo.optionBitmap ?: return
        val width = bitmap.width
        val height = bitmap.height

        val info = engraveReadyDataInfo.engraveData

        //scale
        bitmap = LaserPeckerHelper.bitmapScale(bitmap, px)

        //雕刻的数据
        val data = bitmap.engraveColorBytes()
        //保存一份byte数据
        engraveReadyDataInfo.dataPath = saveEngraveData(info?.index, data)//数据路径
        //保存一份用来历史文档预览的数据
        engraveReadyDataInfo.optionBitmap = bitmap
        engraveReadyDataInfo.previewDataPath = saveEngraveData("${info?.index}.p", bitmap, "png")

        //保存一份可视化的数据
        val channelBitmap = data.toEngraveBitmap(bitmap.width, bitmap.height)
        saveEngraveData(info?.index, channelBitmap, "png")

        //根据px, 修正坐标
        val x = engraveReadyDataInfo.optionX
        val y = engraveReadyDataInfo.optionY
        val rect = EngravePreviewCmd.adjustBitmapRange(x, y, width, height, px)

        //雕刻的宽高使用图片本身的宽高, 否则如果宽高和数据不一致,会导致图片打印出来是倾斜的效果
        val engraveWidth = bitmap.width
        val engraveHeight = bitmap.height

        info?.data = data
        info?.x = rect.left
        info?.y = rect.top
        info?.width = engraveWidth
        info?.height = engraveHeight
        info?.px = px
    }

    /**更新雕刻时的数据模式, 比如图片可以转GCode, 灰度, 黑白数据等*/
    fun updateDataMode(info: EngraveDataInfo) {

    }

    /**保存雕刻数据到文件
     * [fileName] 需要保存的文件名, 无扩展
     * [suffix] 文件后缀, 扩展名
     * [data]
     *   [String]
     *   [ByteArray]
     *   [Bitmap]
     * ]*/
    fun saveEngraveData(fileName: Any?, data: Any?, suffix: String = "engrave"): String? {
        //将雕刻数据写入文件
        return data.writeTo(
            CanvasDataHandleOperate.ENGRAVE_CACHE_FILE_FOLDER,
            "${fileName}.${suffix}",
            false
        )
    }

    fun _handleGCodeEngraveDataInfo(engraveReadyDataInfo: EngraveReadyDataInfo, gCodeFile: File) {
        val pathGCodeText = gCodeFile.readText()
        val gCodeLines = gCodeFile.lines()
        gCodeFile.deleteSafe()

        if (!pathGCodeText.isNullOrEmpty()) {
            //GCode数据

            engraveReadyDataInfo.engraveData?.dataType = EngraveDataInfo.TYPE_GCODE
            engraveReadyDataInfo.engraveData?.lines = gCodeLines
            val data = pathGCodeText.toByteArray()
            engraveReadyDataInfo.engraveData?.data = data

            //保存一份byte数据
            engraveReadyDataInfo.dataPath =
                saveEngraveData(engraveReadyDataInfo.engraveData?.index, data)//数据路径

            saveEngraveData(engraveReadyDataInfo.engraveData?.index, pathGCodeText, "gcode")
            val gCodeDrawable = GCodeHelper.parseGCode(pathGCodeText)
            val bitmap = gCodeDrawable?.toBitmap()
            engraveReadyDataInfo.engraveData?.width =
                gCodeDrawable?.gCodeBound?.width()?.toInt() ?: 0
            engraveReadyDataInfo.engraveData?.height =
                gCodeDrawable?.gCodeBound?.height()?.toInt() ?: 0
            engraveReadyDataInfo.optionBitmap = bitmap
            engraveReadyDataInfo.previewDataPath =
                saveEngraveData("${engraveReadyDataInfo.engraveData?.index}.p", bitmap, "png")
        }
    }

    fun findOptionIndex(list: List<Any>?, value: Byte?): Int {
        return list?.indexOfFirst { it.toString().toInt() == value?.toHexInt() } ?: -1
    }

    //<editor-fold desc="material">

    /**材质列表*/
    val materialList = mutableListOf<MaterialEntity>()
    const val MATERIAL_SPLIT = ","

    /**L1设备推荐参数*/
    fun initL1MaterialList(materialList: MutableList<MaterialEntity> = EngraveHelper.materialList) {
        materialList.clear()
        val L1 = buildString {
            append(LaserPeckerHelper.LI)
            append(MATERIAL_SPLIT)
            append(LaserPeckerHelper.LI_Z)
            append(MATERIAL_SPLIT)
            append(LaserPeckerHelper.LI_PRO)
            append(MATERIAL_SPLIT)
            append(LaserPeckerHelper.LI_Z_PRO)
        }
        //环保纸
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_hbz
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 50
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_hbz
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 30
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_hbz
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 15
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_hbz
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 80
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_hbz
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 65
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_hbz
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 30
        })

        //瓦楞纸
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_wlz
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 50
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_wlz
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 30
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_wlz
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 15
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_wlz
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 90
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_wlz
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 75
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_wlz
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 45
        })

        //皮革
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_pg
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 70
            depth = 70
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_pg
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 70
            depth = 50
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_pg
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 70
            depth = 30
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_pg
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 90
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_pg
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 60
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_pg
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 30
        })

        //竹质
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_zz
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 80
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_zz
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 60
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_zz
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 30
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_zz
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 70
            depth = 80
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_zz
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 70
            depth = 50
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_zz
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 60
            depth = 40
        })

        //木质
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_mz
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 80
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_mz
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 60
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_mz
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 30
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_mz
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 90
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_mz
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 60
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_mz
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 40
        })

        //软木
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_rm
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 40
            depth = 40
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_rm
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 40
            depth = 30
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_rm
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 40
            depth = 20
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_rm
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 50
            depth = 30
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_rm
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 50
            depth = 20
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_rm
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 50
            depth = 15
        })

        //塑料
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_sl
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 50
            depth = 60
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_sl
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 50
            depth = 50
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_sl
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 50
            depth = 40
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_sl
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 60
            depth = 70
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_sl
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 60
            depth = 50
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_sl
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 60
            depth = 30
        })

        //布料
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_bl
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 90
            depth = 70
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_bl
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 90
            depth = 50
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_bl
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 90
            depth = 30
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_bl
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 100
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_bl
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 100
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_bl
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 100
        })

        //毛毡布
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_mzb
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 80
            depth = 20
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_mzb
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 80
            depth = 15
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_mzb
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 70
            depth = 10
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_mzb
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 80
            depth = 45
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_mzb
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 80
            depth = 35
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_mzb
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 80
            depth = 20
        })

        //不透明亚克力
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_btmykl
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 80
            depth = 80
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_btmykl
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 80
            depth = 50
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_btmykl
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 80
            depth = 40
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_btmykl
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 100
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_btmykl
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 90
            depth = 90
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_btmykl
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 90
            depth = 60
        })

        //光敏印章
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gmyz
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 50
            depth = 10
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gmyz
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 60
            depth = 5
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gmyz
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 60
            depth = 3
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gmyz
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 60
            depth = 20
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gmyz
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 60
            depth = 15
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gmyz
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 60
            depth = 10
        })

        //果皮
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gp
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 90
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gp
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 80
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gp
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 60
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gp
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 90
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gp
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 90
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gp
            product = L1
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 80
        })

    }

    /**L2设备推荐参数*/
    fun initL2MaterialList(materialList: MutableList<MaterialEntity> = EngraveHelper.materialList) {
        materialList.clear()
        val L2 = buildString {
            append(LaserPeckerHelper.LII)
        }
        //环保纸
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_hbz
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 3
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_hbz
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 90
            depth = 2
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_hbz
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 1
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_hbz
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 30
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_hbz
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 15
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_hbz
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 5
        })

        //瓦楞纸
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_wlz
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 3
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_wlz
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 90
            depth = 2
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_wlz
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 1
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_wlz
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 30
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_wlz
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 15
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_wlz
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 5
        })

        //皮革
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_pg
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 3
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_pg
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 90
            depth = 2
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_pg
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 1
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_pg
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 30
            depth = 5
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_pg
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 30
            depth = 5
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_pg
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 30
            depth = 5
        })

        //竹质
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_zz
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 10
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_zz
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 5
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_zz
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 3
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_zz
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 40
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_zz
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 30
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_zz
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 20
        })

        //木质
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_mz
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 10
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_mz
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 5
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_mz
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 3
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_mz
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 30
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_mz
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 20
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_mz
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 10
        })

        //软木
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_rm
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 1
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_rm
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 80
            depth = 1
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_rm
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 50
            depth = 1
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_rm
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 30
            depth = 1
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_rm
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 30
            depth = 1
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_rm
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 30
            depth = 1
        })

        //塑料
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_sl
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 2
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_sl
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 2
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_sl
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 50
            depth = 2
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_sl
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 5
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_sl
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 5
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_sl
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 5
        })

        //布料
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_bl
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 70
            depth = 2
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_bl
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 45
            depth = 2
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_bl
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 25
            depth = 2
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_bl
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 15
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_bl
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 10
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_bl
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 80
            depth = 1
        })

        //毛毡布
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_mzb
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 1
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_mzb
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 80
            depth = 1
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_mzb
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 40
            depth = 1
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_mzb
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 1
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_mzb
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 1
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_mzb
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 1
        })

        //不透明亚克力
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_btmykl
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 2
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_btmykl
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 80
            depth = 2
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_btmykl
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 40
            depth = 2
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_btmykl
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 90
            depth = 20
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_btmykl
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 70
            depth = 15
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_btmykl
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 70
            depth = 10
        })

        //光敏印章
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gmyz
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 50
            depth = 1
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gmyz
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 40
            depth = 1
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gmyz
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 20
            depth = 1
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gmyz
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 1
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gmyz
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 1
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gmyz
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 1
        })

        //果皮
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gp
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 5
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gp
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 5
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gp
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 3
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gp
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 80
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gp
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 80
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gp
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 80
        })

        //可乐罐
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_klg
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 10
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_klg
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 5
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_klg
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 3
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_klg
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 100
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_klg
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 80
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_klg
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 50
        })

        //石头
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_st
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 100
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_st
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 100
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_st
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 50
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_st
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 100
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_st
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 100
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_st
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 100
        })

        //氧化金属
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_yhjs
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 10
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_yhjs
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 10
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_yhjs
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 5
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_yhjs
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 100
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_yhjs
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 60
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_yhjs
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 10
        })

        //陶制品
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_tzp
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 10
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_tzp
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 10
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_tzp
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 5
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_tzp
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 50
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_tzp
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 50
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_tzp
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 50
        })

        //漆涂层
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_qtc
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 70
            depth = 1
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_qtc
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 70
            depth = 1
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_qtc
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 50
            depth = 1
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_qtc
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 10
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_qtc
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 10
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_qtc
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 10
        })

        //不锈钢
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_bxg
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 100
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_bxg
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 100
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_bxg
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 100
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_bxg
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 100
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_bxg
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 100
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_bxg
            product = L2
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 100
        })
    }

    /**L3设备推荐参数*/
    fun initL3MaterialList(materialList: MutableList<MaterialEntity> = EngraveHelper.materialList) {
        materialList.clear()
        val L3 = buildString {
            append(LaserPeckerHelper.LIII)
            append(MATERIAL_SPLIT)
            append(LaserPeckerHelper.LIII_YT)
        }
        //光敏印章
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gmyz
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 60
            depth = 20
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gmyz
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 60
            depth = 10
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gmyz
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_4K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 60
            depth = 3
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gmyz
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 60
            depth = 30
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gmyz
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 60
            depth = 10
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gmyz
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_4K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 60
            depth = 5
        })

        //硅胶
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gj
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 5
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gj
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 1
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gj
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_4K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 40
            depth = 1
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gj
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 60
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gj
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 30
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gj
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_4K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 10
        })

        //塑料
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_sl
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 5
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_sl
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 1
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_sl
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_4K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 40
            depth = 1
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_sl
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 30
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_sl
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 80
            depth = 15
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_sl
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_4K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 80
            depth = 5
        })

        //漆面纸板
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_qmzb
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 5
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_qmzb
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 3
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_qmzb
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_4K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 1
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_qmzb
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 30
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_qmzb
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 80
            depth = 10
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_qmzb
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_4K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 80
            depth = 5
        })

        //氧化金属
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_yhjs
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 20
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_yhjs
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 5
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_yhjs
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_4K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 1
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_yhjs
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 60
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_yhjs
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 30
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_yhjs
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_4K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 6
        })

        //拉丝不锈钢
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_lsbxg
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 50
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_lsbxg
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 20
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_lsbxg
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_4K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 5
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_lsbxg
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 100
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_lsbxg
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 100
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_lsbxg
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_4K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 60
        })

        //拉丝不锈钢
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_lsbxg
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 50
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_lsbxg
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 20
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_lsbxg
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_4K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 5
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_lsbxg
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 100
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_lsbxg
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 100
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_lsbxg
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_4K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 60
        })

        //铜
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_tong
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 30
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_tong
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 10
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_tong
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_4K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 1
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_tong
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 60
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_tong
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 30
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_tong
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_4K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 10
        })

        //铝合金
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_lhj
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 60
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_lhj
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 20
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_lhj
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_4K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 3
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_lhj
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 100
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_lhj
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 60
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_lhj
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_4K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 20
        })

        //亚克力
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_ykl
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 40
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_ykl
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 20
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_ykl
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_4K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 5
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_ykl
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 80
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_ykl
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 40
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_ykl
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_4K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 15
        })

        //漆涂层
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_qtc
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 70
            depth = 25
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_qtc
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 70
            depth = 5
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_qtc
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_4K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 60
            depth = 1
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_qtc
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 70
            depth = 50
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_qtc
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 70
            depth = 20
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_qtc
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_4K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 70
            depth = 10
        })

        //皮革
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_pg
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 30
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_pg
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 15
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_pg
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_4K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 5
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_pg
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 80
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_pg
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 25
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_pg
            product = L3
            dataMode = CanvasConstant.BITMAP_MODE_GREY
            px = LaserPeckerHelper.PX_4K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 10
        })
    }

    /**默认参数*/
    fun getProductMaterialList(): List<MaterialEntity> {
        val productName: String? = vmApp<LaserPeckerModel>().productInfoData.value?.name
        val dataMode: Int = vmApp<EngraveModel>().engraveReadyInfoData.value?.optionMode
            ?: CanvasConstant.BITMAP_MODE_BLACK_WHITE //默认黑白
        val px: Byte = vmApp<EngraveModel>().engraveReadyInfoData.value?.engraveData?.px
            ?: LaserPeckerHelper.DEFAULT_PX
        val type: Byte = vmApp<EngraveModel>().engraveOptionInfoData.value?.type
            ?: LaserPeckerHelper.LASER_TYPE_BLUE
        return getProductMaterialList(productName, dataMode, px, type)
    }

    /**获取推荐的材质列表
     * [productName] 需要查询那个产品的推荐参数
     * [dataMode] 数据的处理模式, 对应不同的推荐参数 [CanvasConstant.BITMAP_MODE_BLACK_WHITE] [CanvasConstant.BITMAP_MODE_GREY]
     * [px] 数据的分辨率 [LaserPeckerHelper.DEFAULT_PX]
     * [type] 雕刻激光类型选择，0为1064nm激光 (白光-雕)，1为450nm激光 (蓝光-烧)。(L3max新增)
     * */
    fun getProductMaterialList(
        productName: String?,
        dataMode: Int, /*默认黑白*/
        px: Byte,
        type: Byte
    ): List<MaterialEntity> {
        val result = mutableListOf<MaterialEntity>()
        productName?.let {
            materialList.filterTo(result) { entity ->
                var match = entity.product == productName &&
                        entity.px.toByte() == px &&
                        entity.type.toByte() == type
                match =
                    if (dataMode == CanvasConstant.BITMAP_MODE_GREY || dataMode == CanvasConstant.BITMAP_MODE_DITHERING) {
                        //灰度 抖动
                        match && entity.dataMode == CanvasConstant.BITMAP_MODE_GREY
                    } else {
                        //其他模式下, 都用黑白参数
                        match && entity.dataMode == CanvasConstant.BITMAP_MODE_BLACK_WHITE
                    }
                match
            }
        }
        //自定义, 自动记住了上一次的值
        val custom = MaterialEntity()
        custom.resId = R.string.material_custom
        custom.power = lastPower
        custom.depth = lastDepth
        result.add(0, custom)
        return result
    }

    //</editor-fold desc="material">
}