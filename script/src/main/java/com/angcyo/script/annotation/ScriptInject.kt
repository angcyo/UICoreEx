package com.angcyo.script.annotation

import androidx.annotation.Keep

/**
 * 声明当前对象需要注入到 v8 中
 *
 * 非 public 声明的方法, 无法注入
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/12/22
 * Copyright (c) 2020 angcyo. All rights reserved.
 */

@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION
)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Keep
annotation class ScriptInject(

    /**注入的key, 默认就是属性名*/
    val key: String = "",

    /**注入方法时, 有效*/
    val includeReceiver: Boolean = false,

    /**是否忽略注入*/
    val ignore: Boolean = false,
)
