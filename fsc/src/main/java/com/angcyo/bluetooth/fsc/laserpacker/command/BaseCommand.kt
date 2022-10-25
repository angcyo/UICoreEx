package com.angcyo.bluetooth.fsc.laserpacker.command

import com.angcyo.library.ex.uuid

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/10/25
 */
abstract class BaseCommand : ICommand {

    val _uuid: String by lazy { uuid() }

    override val uuid: String
        get() = _uuid
}