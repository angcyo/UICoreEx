package com.angcyo.laserpacker.device

import android.graphics.Bitmap
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.core.component.file.appFilePath
import com.angcyo.core.component.file.writeTo
import com.angcyo.core.vmApp
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.library.ex.ensureExtName
import com.angcyo.library.ex.file
import com.angcyo.library.unit.IValueUnit
import com.angcyo.library.utils.FileTextData
import com.angcyo.library.utils.filePath
import com.angcyo.library.utils.writeToFile
import kotlin.math.absoluteValue

/**
 * 图层助手
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/30
 */
object EngraveHelper {

    private var engraveIndex: Int = 1
    private var lastEngraveIndex: Int = 1

    //region ---雕刻---

    fun String.toTransferData() = toByteArray(Charsets.ISO_8859_1)

    fun ByteArray.toTransferData() = toString(Charsets.ISO_8859_1)

    /**将字节数据写入到文件*/
    fun ByteArray.writeTransferDataPath(fileName: String) =
        writeToFile(getTransferDataPath(fileName).file())

    /**获取一个传输数据的文件路径*/
    fun getTransferDataPath(fileName: String) =
        filePath(LPDataConstant.ENGRAVE_TRANSFER_FILE_FOLDER, fileName)

    /**生成一个雕刻需要用到的文件索引
     * 4个字节 最大 4_294_967_295
     * */
    fun generateEngraveIndex(): Int {
        /*System.currentTimeMillis()//13位毫秒 1682135311648
        var nano = System.nanoTime() //15位毫秒 //269520350402700
        *//*val s = millis / 1000 //10位秒
        val m = millis % 1000 //毫秒
        val r = nextInt(0, m.toInt()) //随机数
        return (s + m + r).toInt()*//*
        //nano = (nano shl 16) or Random.nextLong(1, 0b1111111111111111) + engraveIndex++
        val randomBits = 16
        nano = (nano shl randomBits) or Random.nextBits(randomBits) + engraveIndex++
        //8位随机数255
        //16位随机数65535 碰撞概率:7 9 8 11 14 14 10 11
        return (nano and 0xfff_ffff).toInt()*/
        var index = (System.nanoTime() shr 4).toInt().absoluteValue
        if (index == lastEngraveIndex) {
            index += engraveIndex++
        }
        lastEngraveIndex = index
        return index
    }

    /**生成一个雕刻的文件名*/
    fun generateEngraveName(): String {
        return "filename-${HawkEngraveKeys.lastEngraveCount + 1}"
    }

    /**生成百分比数值列表*/
    fun percentList(max: Int = 100): List<Int> {
        return (1..max).toList()
    }

    fun findOptionIndex(list: List<Any>?, value: Int?): Int {
        return list?.indexOfFirst { it.toString().toInt() == value } ?: -1
    }

    /**获取物理尺寸的值*/
    fun getDiameter(): Int {
        val laserPeckerModel = vmApp<LaserPeckerModel>()
        //物理尺寸
        val diameter = if (!laserPeckerModel.haveExDevice()) {
            0
        } else {
            val mm = IValueUnit.MM_UNIT.convertPixelToValue(HawkEngraveKeys.lastDiameterPixel)
            (mm * 100).toInt()
        }
        return diameter
    }

    /**保存雕刻数据到文件
     * [index] 需要保存的文件名(雕刻索引), 无扩展
     * [suffix] 文件后缀, 扩展名
     * [data]
     *   [String]
     *   [ByteArray]
     *   [Bitmap]
     *   [File]
     * ]*/
    fun saveEngraveData(
        index: Any?,
        data: FileTextData?,
        suffix: String = "engrave",
        recycle: Boolean = false,
    ): String? {
        //将雕刻数据写入文件
        return data.writeTo(
            LPDataConstant.ENGRAVE_FILE_FOLDER,
            "${index}${suffix.ensureExtName()}",
            false,
            recycle
        )
    }

    /** 单独返回文件路径
     * [saveEngraveData]*/
    fun getSaveEngraveDataFilePath(index: Any?, suffix: String = "engrave"): String {
        return appFilePath("${index}${suffix.ensureExtName()}", LPDataConstant.ENGRAVE_FILE_FOLDER)
    }

    //endregion ---雕刻---

}