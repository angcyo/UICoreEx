package com.angcyo.engrave2.transition

import com.angcyo.gcode.CollectPoint
import java.io.File

/**
 * 包裹一层[targetFile]
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/10/23
 */
class PathDataFile(val targetFile: File) {
    /**[com.angcyo.gcode.GCodeWriteHandler._collectPointList]*/
    var collectPointList: List<CollectPoint>? = null
}