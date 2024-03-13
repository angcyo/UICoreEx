package com.angcyo.bluetooth.fsc.bean

import com.angcyo.http.base.type
import java.lang.reflect.Type

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2024/03/13
 */
data class DHttpBean<T>(
    //任务集合
    var result: T? = null
)

fun dBeanType(typeClass: Class<*>): Type = type(DHttpBean::class.java, typeClass)