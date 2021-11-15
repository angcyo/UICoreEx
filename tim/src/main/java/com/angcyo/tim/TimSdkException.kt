package com.angcyo.tim

/**
 * TIM sdk 异常类
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/09
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class TimSdkException(val code: Int, val desc: String?) : RuntimeException(desc) {
    override fun toString(): String {
        return "Tim异常:$code:$desc"
    }
}