package com.angcyo.acc2.app.http.bean

import com.angcyo.http.base.type
import java.lang.reflect.Type

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2022/01/05
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class HttpListBean<DATA> : HttpBean<List<DATA>>()


/**
 * HttpListBean<<Bean>
 * */
fun httpListBeanType(typeClass: Class<*>): Type = type(HttpListBean::class.java, typeClass)