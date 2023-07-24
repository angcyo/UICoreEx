package com.angcyo.objectbox.annotation

/**
 * 标识当前的操作进行数据的`增删改`操作
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/07/24
 */
@Target(AnnotationTarget.FUNCTION)
annotation class OperateEntity(val des: String = "增删改")
