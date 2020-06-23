package com.angcyo.jpush

import android.content.Context
import cn.jpush.android.api.JPushInterface

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/06/23
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
object JPush {

    /**初始化入口*/
    fun init(context: Context, debug: Boolean = BuildConfig.DEBUG) {
        JPushInterface.setDebugMode(debug)
        JPushInterface.init(context.applicationContext)
    }

    /**
     * http://docs.jiguang.cn/jpush/client/Android/android_api/#api_3
     * */
    fun getAlias(context: Context, sequence: Int) {
        JPushInterface.getAlias(context, sequence)
    }

    /**
     * [sequence] 用户自定义的操作序列号，同操作结果一起返回，用来标识一次操作的唯一性。
     * [alias] 每次调用设置有效的别名，覆盖之前的设置。
     * 有效的别名组成：字母（区分大小写）、数字、下划线、汉字、特殊字符@!#$&*+=.|。
     * 限制：alias 命名长度限制为 40 字节。（判断长度需采用 UTF-8 编码）
     * */
    fun setAlias(context: Context, sequence: Int, alias: String?) {
        JPushInterface.setAlias(context, sequence, alias)
    }

    fun deleteAlias(context: Context, sequence: Int) {
        JPushInterface.deleteAlias(context, sequence)
    }

    /**
     * [context] 应用的ApplicationContext。
     * [sequence] 用户自定义的操作序列号，同操作结果一起返回，用来标识一次操作的唯一性。
     * [mobileNumber] 手机号码。如果传 null 或空串则为解除号码绑定操作。
     * 限制：只能以 “+” 或者 数字开头；后面的内容只能包含 “-” 和数字。
     * */
    fun setMobileNumber(
        context: Context,
        sequence: Int,
        mobileNumber: String?
    ) {
        JPushInterface.setMobileNumber(context, sequence, mobileNumber)
    }

    fun setTags(
        context: Context,
        sequence: Int,
        tags: Set<String?>?
    ) {
        JPushInterface.setTags(context, sequence, tags)
    }

    fun addTags(
        context: Context,
        sequence: Int,
        tags: Set<String?>?
    ) {
        JPushInterface.addTags(context, sequence, tags)
    }

    fun deleteTags(
        context: Context,
        sequence: Int,
        tags: Set<String?>?
    ) {
        JPushInterface.deleteTags(context, sequence, tags)
    }

    fun cleanTags(context: Context, sequence: Int) {
        JPushInterface.cleanTags(context, sequence)
    }

    fun getAllTags(context: Context, sequence: Int) {
        JPushInterface.getAllTags(context, sequence)
    }

}