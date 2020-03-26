package com.angcyo.game.core

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/03/09
 */
data class DrawParams(
    //第一帧的时间
    var drawFirstTime: Long = -1,
    //上一帧的时间
    var drawPrevTime: Long = -1,
    //当前帧的时间
    var drawCurrentTime: Long = -1
)