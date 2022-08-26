package com.angcyo.engrave.transition

import com.angcyo.canvas.items.renderer.BaseItemRenderer
import com.angcyo.canvas.utils.toDataModeStr
import com.angcyo.canvas.utils.toDataTypeStr
import com.angcyo.engrave.data.EngraveDataInfo
import com.angcyo.engrave.data.EngraveReadyInfo
import com.angcyo.engrave.data.toEngraveTypeStr
import com.angcyo.library.L
import com.angcyo.library.annotation.CallPoint

/**
 * 雕刻数据相关处理
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/08/22
 */
class EngraveTransitionManager {

    companion object {
        /**生成一个雕刻需要用到的文件索引*/
        fun generateEngraveIndex(): Int {
            return (System.currentTimeMillis() / 1000).toInt()
        }
    }

    /**数据转换器*/
    private val transitionList = mutableListOf<IEngraveTransition>()

    init {
        transitionList.add(GCodeTransition())
        transitionList.add(BitmapTransition())
    }

    /**将[renderer]转换成雕刻预备的数据 */
    @CallPoint
    fun transitionReadyData(renderer: BaseItemRenderer<*>?): EngraveReadyInfo? {
        val itemRenderer = renderer ?: return null
        var result: EngraveReadyInfo? = null

        for (transition in transitionList) {
            result = transition.doTransitionReadyData(itemRenderer)
            if (result != null) {
                break
            }
        }

        if (result?.engraveData == null) {
            result?.engraveData = EngraveDataInfo()
        }
        if (result == null) {
            L.w("无法处理的Item:${renderer}")
        } else {
            L.i(
                "预处理Item数据->",
                result.dataType.toDataTypeStr(),
                result.dataMode.toDataModeStr()
            )
        }
        return result
    }

    /**真正的雕刻数据处理*/
    @CallPoint
    fun transitionEngraveData(
        renderer: BaseItemRenderer<*>?,
        engraveReadyInfo: EngraveReadyInfo
    ) {
        val itemRenderer = renderer ?: return

        for (transition in transitionList) {
            val result = transition.doTransitionEngraveData(itemRenderer, engraveReadyInfo)
            if (result) {
                break
            }
        }

        engraveReadyInfo.engraveData?.let {
            L.i(
                "处理Item数据->",
                engraveReadyInfo.dataType.toDataTypeStr(),
                engraveReadyInfo.dataMode.toDataModeStr(),
                engraveReadyInfo.engraveData?.engraveDataType?.toEngraveTypeStr()
            )
        }
    }

}