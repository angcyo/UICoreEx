package com.angcyo.engrave.transition

import android.graphics.Bitmap
import com.angcyo.canvas.items.renderer.BaseItemRenderer
import com.angcyo.canvas.utils.CanvasDataHandleOperate
import com.angcyo.core.component.file.writeTo
import com.angcyo.engrave.data.EngraveDataInfo
import com.angcyo.engrave.data.EngraveReadyInfo

/**雕刻数据转换
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/08/22
 */
interface IEngraveTransition {

    /**开始转换需要准备的数据
     * 返回[null], 表示未处理
     * */
    fun doTransitionReadyData(renderer: BaseItemRenderer<*>): EngraveReadyInfo?

    /**开始转换需要雕刻的数据
     * 返回[false], 表示未处理
     * */
    fun doTransitionEngraveData(
        renderer: BaseItemRenderer<*>,
        engraveReadyInfo: EngraveReadyInfo
    ): Boolean

    /**初始化一个雕刻数据*/
    fun initReadyEngraveData(renderer: BaseItemRenderer<*>, engraveReadyInfo: EngraveReadyInfo) {
        //索引
        val item = renderer.getRendererRenderItem()
        var index = item?.engraveIndex
        if (index == null) {
            index = EngraveTransitionManager.generateEngraveIndex()
            item?.engraveIndex = index
        }
        //init
        if (engraveReadyInfo.engraveData == null) {
            engraveReadyInfo.engraveData = EngraveDataInfo()
        }
        //雕刻数据
        engraveReadyInfo.engraveData?.apply {
            this.dataType = engraveReadyInfo.dataType
            this.index = item?.engraveIndex
            this.name = item?.itemLayerName?.toString()
        }
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
}