package com.angcyo.game.core

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/03/09
 */
data class UpdateParams(
    var updateFirstTime: Long = -1,
    var updatePrevTime: Long = -1,
    var updateCurrentTime: Long = -1
)