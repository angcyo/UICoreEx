package com.angcyo.quickjs

import android.os.Handler
import android.os.HandlerThread
import com.angcyo.library.ex.uuid
import com.angcyo.quickjs.api.core.LoopJsApi
import com.quickjs.JSContext
import com.quickjs.QuickJS
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/06/19
 */
class EngineExecuteThread(val readyAction: (thread: EngineExecuteThread, handler: Handler) -> Unit) :
    HandlerThread("QuickJSEngine-${executeIndex++}") {

    companion object {
        internal var executeIndex = 0
        internal val engineMap = ConcurrentHashMap<String, EngineExecuteThread>()

        /**获取一个执行器线程*/
        fun get(uuid: String): EngineExecuteThread? = engineMap[uuid]
    }

    /**处理器*/
    var executeHandler: Handler? = null

    /**运行结束后的回调*/
    var onExecuteFinish: (thread: EngineExecuteThread) -> Unit = {}

    //--
    var quickJs: QuickJS? = null
    var context: JSContext? = null
    val uuid: String = uuid()
    //--

    /**是否要一直等待直到主动调用了退出?*/
    var waitForQuit: AtomicBoolean = AtomicBoolean(false)

    init {
        start()
    }

    override fun onLooperPrepared() {
        super.onLooperPrepared()
        executeHandler = Handler(looper)
        readyAction(this, executeHandler!!)
    }

    override fun run() {
        super.run()
        LoopJsApi.clearLoop(uuid)
        onExecuteFinish(this)
    }

    /**直接终止*/
    override fun quit(): Boolean {
        return super.quit()
    }

    /**消息处理完后, 终止*/
    override fun quitSafely(): Boolean {
        return super.quitSafely()
    }

    /**保存对象*/
    fun inject(quickJs: QuickJS, context: JSContext) {
        this.quickJs = quickJs
        this.context = context

        context.set("id", uuid)//注入id engineId
        engineMap[uuid] = this
    }

    /**释放对象, 并退出*/
    fun release() {
        engineMap.remove(uuid)
        try {
            context?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            quickJs?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        quit()
    }
}