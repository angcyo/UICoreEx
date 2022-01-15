package com.angcyo.github.finger

/**
 * Created by Zhangwh on 2018/9/7.
 * 指纹识别结束返回信息
 */
class IdentificationInfo {

    var exception: FPerException? = null

    var isSuccessful: Boolean

    constructor(isSuccessful: Boolean) {
        this.isSuccessful = isSuccessful
    }

    constructor(exceptionCode: Int) {
        exception = FPerException(exceptionCode)
        isSuccessful = false
    }
}