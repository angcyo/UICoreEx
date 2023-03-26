package com.angcyo.acc2.app

import com.angcyo.library.component.HawkPropertyValue
import com.angcyo.library.ex._string
import com.angcyo.library.ex.fromBase64
import com.angcyo.library.ex.nowTime
import com.angcyo.library.ex.toBase64
import com.angcyo.library.utils.Device
import kotlin.random.Random.Default.nextInt

/**
 * 安全加密
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2023/03/26
 * Copyright (c) 2020 angcyo. All rights reserved.
 */
object AccSafety {

    /**输入的卡密*/
    var safetyCard: String? by HawkPropertyValue<Any, String?>(null)

    /**通过[key]变量, 产生一个加密字符串, 里面包含密钥过期时间
     * 包括 [time][key][angcyo]
     * [endTime] 密钥到期时间, 13位时间戳毫秒
     * */
    fun generateCode(key: String = Device.androidId, endTime: Long): String {
        val code = buildString {
            val s1 = "$endTime"
            val s2 = key
            val len = kotlin.math.max(s1.length, s2.length)
            for (i in 0 until len) {
                s1.getOrNull(i)?.let {
                    append(it)
                }
                s2.getOrNull(i)?.let {
                    append(it)
                }
            }
            val random = _string(R.string.lib_en_digits)
            //随机字符串
            for (i in 0 until nextInt(0, 10)) {//10个字符随机
                val index = nextInt(0, random.lastIndex)
                append(random[index])//随机字符
            }
        }
        val encode = code.toBase64()
        return encode
    }

    /**从[code]中反解[generateCode]生成的密钥过期时间
     * @return null : key匹配失败
     *         long : 密钥过期时间
     * */
    fun parseCode(code: String?, key: String = Device.androidId): Long? {
        val s1 = StringBuilder() //time
        val s2 = StringBuilder() //key
        try {
            val decode = code!!.fromBase64()
            var index = 0
            var isS1End = false
            var isS2End = false
            while (true) {
                if (s1.length < 13) {
                    s1.append(decode[index++])
                } else {
                    isS1End = true
                }
                if (s2.length < key.length) {
                    s2.append(decode[index++])
                } else {
                    isS2End = true
                }
                if (index >= decode.length || (isS1End && isS2End)) {
                    break
                }
            }
            return if (s2.toString() == key) {
                s1.toString().toLongOrNull() ?: 0L
            } else {
                null
            }
        } catch (e: Exception) {
            return null
        }
    }

    /**卡密是否过期*/
    fun isStaleCard(): Boolean {
        val card = safetyCard
        return (parseCode(card) ?: 0L) <= nowTime()
    }

    /**软件到期时间, 13位毫秒时间*/
    fun staleTime(): Long? = parseCode(safetyCard)
}