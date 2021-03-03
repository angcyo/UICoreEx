package com.angcyo.acc2.app.http.bean

import com.angcyo.http.base.type
import java.lang.reflect.Type

/**
 * 网络请求数据结构
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/07/07
 * Copyright (c) 2020 angcyo. All rights reserved.
 */
open class HttpBean<T> {

    //200..299 表示成功
    var code: Int = -1

    var msg: String? = null

    //数据的版本, 高版本的数据会覆盖低版本
    var version: Long = -1

    //任务集合
    var data: T? = null
}

class PageBean<T> {
    /**数据总量*/
    var total: Long = 0

    /**当前返回的数量*/
    var size: Long = 0

    /**当前页*/
    var current: Long = 0

    /**总页数*/
    var pages: Long = 0

    /**数据集合*/
    var records: List<T>? = null
}

class HttpPageBean<T> : HttpBean<PageBean<T>>()

/**
 * HttpBean<Bean>
 * */
fun beanType(typeClass: Class<*>): Type = type(HttpBean::class.java, typeClass)

/**
 * HttpPageBean<Bean>
 * */
fun pageBeanType(typeClass: Class<*>): Type = type(HttpPageBean::class.java, typeClass)
//type(HttpBean::class.java, type(PageBean::class.java, typeClass))

/**
 * HttpBean<List<Bean>>
 * */
fun listBeanType(typeClass: Class<*>): Type =
    type(HttpBean::class.java, type(List::class.java, typeClass))