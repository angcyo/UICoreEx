package com.angcyo.bluetooth.fsc.laserpacker.command

/**
 * 指令异常
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/12/18
 */
class CommandException : Exception {
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
    constructor(cause: Throwable?) : super(cause)
}