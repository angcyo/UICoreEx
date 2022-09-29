package com.angcyo.bluetooth.fsc

import androidx.annotation.WorkerThread
import com.angcyo.bluetooth.fsc.CommandQueueHelper.FLAG_NORMAL
import com.angcyo.bluetooth.fsc.laserpacker.command.ICommand
import com.angcyo.bluetooth.fsc.laserpacker.command.sendCommand
import com.angcyo.bluetooth.fsc.laserpacker.parse.MiniReceiveParser
import com.angcyo.bluetooth.fsc.laserpacker.writeBleLog
import com.angcyo.library.L
import com.angcyo.library.ex.className
import com.angcyo.library.ex.have
import com.angcyo.library.ex.nowTime
import com.angcyo.library.ex.size
import com.angcyo.library.toast
import java.util.*

/**
 * 蓝牙指令发送队列
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/17
 */

object CommandQueueHelper {

    /**默认标识*/
    const val FLAG_NORMAL = 0x00

    /**标识, 清除之前的所有指令*/
    const val FLAG_CLEAR_BEFORE = 0x00001

    /**标识, 清除之前的所有相同的指令*/
    const val FLAG_CLEAR_BEFORE_SAME = 0x00002

    /**标识, 异步执行指令*/
    const val FLAG_ASYNC = 0x00004

    /**当前指令, 不需要接收返回的数据*/
    const val FLAG_NO_RECEIVE = 0x00008

    /**待执行的指令列表*/
    private val linkedList = LinkedList<CommandInfo>()

    /**当前执行的指令*/
    @Volatile
    var _currentCommand: CommandInfo? = null

    /**添加一个指令到队列*/
    @Synchronized
    fun addCommand(info: CommandInfo) {
        info.startTime = nowTime()
        //flag
        if (info.flag.have(FLAG_CLEAR_BEFORE)) {
            //清理之前所有任务
            linkedList.forEach {
                it._receiveTask?.isCancel = true
            }
            linkedList.clear()
        }
        if (info.flag.have(FLAG_CLEAR_BEFORE_SAME)) {
            //清除之前相同类型的指令
            val list = mutableListOf<CommandInfo>()
            linkedList.forEach {
                if (it.command.className() == info.command.className()) {
                    list.add(it)
                    it._receiveTask?.isCancel = true
                }
            }
            linkedList.removeAll { list.contains(it) }
        }
        if (info.flag.have(FLAG_ASYNC) && linkedList.size() >= 1) {
            //异步任务
            _runCommand(info, false)
            return
        }

        //入队
        linkedList.add(info)
        L.i("加入指令:${info.command.toCommandLogString()} 共:${linkedList.size()}")
        start()
    }

    /**开始执行队列*/
    @Synchronized
    fun start() {
        if (_currentCommand == null) {
            val commandInfo = linkedList.poll()
            if (commandInfo == null) {
                //无指令需要执行
                L.w("队列所有指令执行完毕!")
            } else {
                //执行指令
                _currentCommand = commandInfo
                _runCommand(commandInfo, true)
            }
        } else {
            //已经在执行指令
            val command = _currentCommand?.command
            if (linkedList.size() > 1) {
                L.w("${command.hashCode()} ${command?.toCommandLogString()},指令正在运行,剩余:${linkedList.size()}")

                //检查超时指令, 自动取消
                _currentCommand?.apply {
                    if (startTime - nowTime() > (command?.getReceiveTimeout() ?: Long.MAX_VALUE)) {
                        //指令超时了
                        _currentCommand?._receiveTask?.isCancel = true
                    }
                }
            }
        }
    }

    /**执行下一个*/
    @Synchronized
    fun next() {
        _currentCommand = null
        start()
    }

    /**清空所有任务
     * 设备断开后, 清除所有指令*/
    @Synchronized
    fun clearCommand() {
        linkedList.forEach {
            try {
                it.listener?.onReceive(null, ReceiveCancelException("ReceiveCancel!"))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        linkedList.clear()
        try {
            _currentCommand?.listener?.onReceive(null, ReceiveCancelException("ReceiveCancel!"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun _runCommand(commandInfo: CommandInfo, next: Boolean) {
        val command = commandInfo.command
        val task = command.sendCommand(commandInfo.address, {
            try {
                commandInfo.listener?.onPacketProgress(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }) { bean, error ->
            L.i("指令返回:${command.hashCode()}->${bean?.parse<MiniReceiveParser>()} $error".writeBleLog())
            //
            if (next) {
                next()
            }
            try {
                commandInfo.listener?.onReceive(bean, error)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        commandInfo._receiveTask = task

        //不需要返回值
        if (commandInfo.flag.have(FLAG_NO_RECEIVE)) {
            //直接完成
            task?.listener?.onReceive(null, null)
            task?.end()
        }
    }

    /**需要入队列的信息*/
    data class CommandInfo(
        val command: ICommand,
        val flag: Int = FLAG_NORMAL,
        val address: String? = null,
        @WorkerThread
        val listener: IReceiveListener? = null,
        //任务
        var _receiveTask: WaitReceivePacket? = null,
        //入队的时间
        var startTime: Long = -1
    )
}

/**入队*/
fun List<ICommand>.enqueue(address: String? = null) {
    forEach {
        CommandQueueHelper.addCommand(
            CommandQueueHelper.CommandInfo(
                it,
                FLAG_NORMAL,
                address,
                null
            )
        )
    }
}

/**指令入队*/
fun ICommand.enqueue(
    address: String? = null,
    progress: ISendProgressAction = {},
    action: IReceiveBeanAction = { bean: ReceivePacket?, error: Exception? ->
        error?.let { toast(it.message) }
    }
) {
    enqueue(FLAG_NORMAL, address, progress, action)
}

/**指令入队*/
fun ICommand.enqueue(
    flag: Int = FLAG_NORMAL,
    address: String? = null,
    progress: ISendProgressAction = {},
    action: IReceiveBeanAction = { bean: ReceivePacket?, error: Exception? ->
        error?.let { toast(it.message) }
    }
) {
    CommandQueueHelper.addCommand(
        CommandQueueHelper.CommandInfo(
            this,
            flag,
            address,
            object : IReceiveListener {
                override fun onPacketProgress(bean: ReceivePacket) {
                    progress(bean)
                }

                override fun onReceive(bean: ReceivePacket?, error: Exception?) {
                    action(bean, error)
                }
            })
    )
}