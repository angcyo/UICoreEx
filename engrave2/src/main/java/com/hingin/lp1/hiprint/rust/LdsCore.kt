package com.hingin.lp1.hiprint.rust


/**
 * lds 相关算法
 */
object LdsCore {
    init {
        System.loadLibrary("lds_android_core")
    }

    /**
     * gcode 路径优化
     * @param gcode 原始 gcode 代码，只包含 G1 G0， 需要完整 X Y 坐标数据，不能省略
     * @param begin gcode 启动前指令，主要用于设置坐标系统 单位 速度 等
     * @param end gcode 结束指令，主要用于 gcode 主体工作之后的指令，例如 G0 X0Y0 M2 等
     * @param toolOn 工具启动指令 例如 M3S255
     * @param toolOff 工具关闭指令 例如 M5S0
     * @return 优化后的 gcode 包含启动，终止 工具开关指令
     */
    external fun gcodeOptimiser(
        gcode: String?,
        begin: String?,
        end: String?,
        toolOn: String?,
        toolOff: String?
    ): String?

    /**
     * 0x30 点坐标路径优化
     * @param points 点坐标 [[x1,y1,x2,y2...xn,yn],[x1,y1,x2,y2...xn,yn]]
     * @return 优化后的点坐标
     */
    external fun pointsOptimiser(points: ArrayList<ArrayList<Double>>): ArrayList<ArrayList<Double>>

}
