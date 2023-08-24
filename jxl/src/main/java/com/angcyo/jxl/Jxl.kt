package com.angcyo.jxl

import android.net.Uri
import com.angcyo.library.ex.inputStream
import com.angcyo.library.ex.toStr
import jxl.Workbook
import jxl.WorkbookSettings
import jxl.write.Label
import jxl.write.WritableWorkbook
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * Jxl 读取/写入Excel
 * ```
 * Unable to recognize OLE stream
 * ```
 * 不支出读取 excel 2007 文件(*.xlsx)。只支持 excel 2003 (*.xls)
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/08/24
 */
object Jxl {

    //region ---读---

    /**读取Excel文件*/
    fun readExcel(
        file: File?,
        ws: WorkbookSettings = WorkbookSettings().apply {
            encoding = "UTF-8"
        }
    ): Map<String, List<List<Any?>>>? = file?.inputStream()?.use {
        return readExcel(it, ws)
    }

    fun readExcel(
        uri: Uri?,
        ws: WorkbookSettings = WorkbookSettings().apply {
            encoding = "UTF-8"
        }
    ): Map<String, List<List<Any?>>>? = uri?.inputStream()?.use {
        return readExcel(it, ws)
    }

    fun readExcel(
        input: InputStream?,
        ws: WorkbookSettings = WorkbookSettings().apply {
            encoding = "UTF-8"
        }
    ): Map<String, List<List<Any?>>>? {
        input ?: return null
        var result: HashMap<String, List<List<Any?>>>? = null
        var workbook: Workbook? = null
        try {
            workbook = Workbook.getWorkbook(input, ws)
            val sheetNames = workbook.sheetNames
            for (sheetName in sheetNames) {
                workbook.getSheet(sheetName)?.let { sheet ->
                    val rows = sheet.rows
                    val columns = sheet.columns

                    var sheetDataList: MutableList<List<Any?>>? = null
                    for (row in 0 until rows) {
                        val lineList = mutableListOf<Any?>()
                        for (column in 0 until columns) {
                            //lineList.add(sheet.getCell(column, row).type)
                            lineList.add(sheet.getCell(column, row).contents)
                        }
                        if (sheetDataList == null) {
                            sheetDataList = mutableListOf()
                        }
                        sheetDataList.add(lineList)
                    }

                    if (result == null) {
                        result = hashMapOf()
                    }
                    result!![sheetName] = sheetDataList!!
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            workbook?.close()
        }
        return result
    }

    //endregion ---读---

    //region ---写---

    /**写入Excel文件*/
    fun writeExcel(
        file: File,
        list: List<List<Any?>>,
        ws: WorkbookSettings = WorkbookSettings().apply {
            encoding = "UTF-8"
        }
    ) {
        file.outputStream().use {
            writeExcel(it, list, ws)
        }
    }

    /**写入Excel文件*/
    fun writeExcel(
        out: OutputStream,
        list: List<List<Any?>>,
        ws: WorkbookSettings = WorkbookSettings().apply {
            encoding = "UTF-8"
        }
    ) {
        var workbook: WritableWorkbook? = null
        try {
            workbook = Workbook.createWorkbook(out, ws)
            val sheet = workbook.createSheet("Sheet1", 0)
            list.forEachIndexed { lineIndex, lineList ->
                lineList.forEachIndexed { columnIndex, data ->
                    sheet.addCell(Label(columnIndex, lineIndex, data?.toStr()))
                }
            }
            workbook.write()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            workbook?.close()
        }
    }

    //endregion ---写---
}