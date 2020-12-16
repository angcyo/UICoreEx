package com.angcyo.jpush

import android.content.Context
import cn.jpush.android.api.JPushInterface
import com.angcyo.library.app
import com.angcyo.library.ex.generateInt

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/06/23
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
object JPush {

    /**初始化入口*/
    fun init(context: Context = app(), debug: Boolean = BuildConfig.DEBUG) {
        JPushInterface.setDebugMode(debug)
        JPushInterface.init(context.applicationContext)
    }

    /**
     * http://docs.jiguang.cn/jpush/client/Android/android_api/#api_3
     * */
    fun getAlias(context: Context = app(), sequence: Int = generateInt()): Int {
        JPushInterface.getAlias(context, sequence)
        return sequence
    }

    /**
     * [sequence] 用户自定义的操作序列号，同操作结果一起返回，用来标识一次操作的唯一性。
     * [alias] 每次调用设置有效的别名，覆盖之前的设置。
     * 有效的别名组成：字母（区分大小写）、数字、下划线、汉字、特殊字符@!#$&*+=.|。
     * 限制：alias 命名长度限制为 40 字节。（判断长度需采用 UTF-8 编码）
     * */
    fun setAlias(context: Context = app(), sequence: Int = generateInt(), alias: String?): Int {
        JPushInterface.setAlias(context, sequence, alias)
        return sequence
    }

    fun deleteAlias(context: Context = app(), sequence: Int = generateInt()): Int {
        JPushInterface.deleteAlias(context, sequence)
        return sequence
    }

    /**
     * [context] 应用的ApplicationContext。
     * [sequence] 用户自定义的操作序列号，同操作结果一起返回，用来标识一次操作的唯一性。
     * [mobileNumber] 手机号码。如果传 null 或空串则为解除号码绑定操作。
     * 限制：只能以 “+” 或者 数字开头；后面的内容只能包含 “-” 和数字。
     * */
    fun setMobileNumber(
        context: Context = app(),
        sequence: Int = generateInt(),
        mobileNumber: String?
    ): Int {
        JPushInterface.setMobileNumber(context, sequence, mobileNumber)
        return sequence
    }

    /**有效的别名、标签组成：字母（区分大小写）、数字、下划线、汉字、特殊字符( 2.1.6 支持)@!#$&*+=.|
     * https://docs.jiguang.cn/jpush/client/Android/android_api/#_153
     * */
    fun setTags(context: Context = app(), sequence: Int = generateInt(), tags: Set<String?>?): Int {
        JPushInterface.setTags(context, sequence, tags)
        return sequence
    }

    fun addTags(context: Context = app(), sequence: Int = generateInt(), tags: Set<String?>?): Int {
        JPushInterface.addTags(context, sequence, tags)
        return sequence
    }

    fun deleteTags(
        context: Context = app(),
        sequence: Int = generateInt(),
        tags: Set<String?>?
    ): Int {
        JPushInterface.deleteTags(context, sequence, tags)
        return sequence
    }

    fun cleanTags(context: Context = app(), sequence: Int = generateInt()): Int {
        JPushInterface.cleanTags(context, sequence)
        return sequence
    }

    fun getAllTags(context: Context = app(), sequence: Int = generateInt()): Int {
        JPushInterface.getAllTags(context, sequence)
        return sequence
    }

}