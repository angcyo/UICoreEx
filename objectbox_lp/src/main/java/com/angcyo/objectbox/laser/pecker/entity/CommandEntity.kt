package com.angcyo.objectbox.laser.pecker.entity

import androidx.annotation.Keep
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

/**
 * 指令表, 记录发送的指令, 和指令的返回值
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/10/25
 */
@Keep
@Entity
data class CommandEntity(
    @Id var entityId: Long = 0L,

    /**指令的uuid, 唯一标识*/
    var uuid: String? = null,

    //---

    /**发送的指令, hex字符串*/
    var command: String? = null,

    /**[command]的功能码*/
    var func: Int? = null,

    /**[command]的功能码状态*/
    var state: Int? = null,

    /**指令描述*/
    var des: String? = null,

    //---

    /**指令返回, hex字符串*/
    var result: String? = null,

    /**返回值描述*/
    var resultDes: String? = null,

    /**指令发送时间, 13位毫秒*/
    var sendTime: Long = -1,

    /**指令返回时间, 13位毫秒*/
    var resultTime: Long = -1,
)
