package com.angcyo.engrave

import com.angcyo.library.ex.toElapsedTime

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/20
 */

/**分:秒 的时间格式*/
fun Long?.toEngraveTime() = this?.toElapsedTime(
    pattern = intArrayOf(-1, 1, 1),
    units = arrayOf("", "", ":", ":", ":")
)