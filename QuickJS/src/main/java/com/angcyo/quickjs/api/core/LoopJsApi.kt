package com.angcyo.quickjs.api.core

import android.webkit.JavascriptInterface
import androidx.annotation.Keep
import com.angcyo.http.rx.doBack
import com.angcyo.library.ex.sleep
import com.angcyo.library.ex.uuid
import com.angcyo.library.ex.yield
import com.angcyo.quickjs.EngineExecuteThread
import com.angcyo.quickjs.api.BaseJSInterface
import com.quickjs.JSArray
import com.quickjs.JSFunction
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 支持循环调用的api, 在子线程中调用
 *
 * java.lang.Error: All QuickJS methods must be called on the same thread.
 * Invalid QuickJS thread access: current thread is Thread[RxCachedThreadScheduler-2,5,main]
 * while the locker has thread Thread[RxCachedThreadScheduler-1,5,main]
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/06/19
 */
@Keep
class LoopJsApi : BaseJSInterface() {

    companion object {
        /**循环的状态控制*/
        internal val loopStateMap = ConcurrentHashMap<String, AtomicBoolean>()

        /**清理所有循环
         * [engineId] js殷勤的id*/
        fun clearLoop(engineId: String) {
            loopStateMap.keys.forEach {
                if (it.startsWith("${engineId}_")) {
                    loopStateMap[it]?.set(false)
                }
            }
        }
    }

    override val interfaceName: String = "loop"

    private fun key(uuid: String) = "${engineId}_${uuid}"

    /**退出循环
     * [uuid] 循环的标识*/
    @JavascriptInterface
    fun quitLoop(uuid: String): Boolean {
        try {
            val atomicBoolean = loopStateMap[key(uuid)]
            if (atomicBoolean != null) {
                atomicBoolean.set(false)
                return true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    /**启动一个循环, 在子线程中执行
     * 需要保证主线程不被关闭
     * [quitLoop]
     * [com.angcyo.quickjs.api.core.AppJsApi.waitForQuit]
     * [com.angcyo.quickjs.api.core.AppJsApi.waitTimeout
     * @return 返回循环的唯一标识, 可以用来控制退出循环*/
    @JavascriptInterface
    fun loop(action: JSFunction): String? {
        EngineExecuteThread.get(engineId)?.let { engineThread ->
            val uuid = uuid()
            val key = key(uuid)
            loopStateMap[key] = AtomicBoolean(true)
            doBack {
                try {
                    while (loopStateMap[key]?.get() == true) {
                        engineThread.executeHandler?.post {
                            action.call(action, JSArray(action.context))
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    quitLoop(uuid)
                }
            }
            return uuid
        }
        return null
    }

    /**循环, 每隔多久执行一次
     * [delay] 循环间隔, 毫秒
     *
     * ```
     * let count = 0;
     * AppJs.loop.loopDelay(1000, ()=>{
     *     AppJs.L.d(`${count++}`)
     *     if(count>=10){
     *         AppJs.quit()
     *     }
     * })
     *
     * AppJs.waitForQuit();
     * ```
     *
     * [quitLoop]
     */
    @JavascriptInterface
    fun loopDelay(delay: Int, action: JSFunction): String? {
        EngineExecuteThread.get(engineId)?.let { engineThread ->
            val uuid = uuid()
            val key = key(uuid)
            loopStateMap[key] = AtomicBoolean(true)
            doBack {
                try {
                    var time = 0L
                    while (loopStateMap[key]?.get() == true) {
                        val nowTime = System.currentTimeMillis()
                        if (time == 0L || nowTime - time >= delay) {
                            //循环
                            engineThread.executeHandler?.post {
                                action.call(action, JSArray(action.context))
                            }
                            time = nowTime
                        } else {
                            //等待
                            try {
                                yield()
                                sleep(delay.toLong())
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    quitLoop(uuid)
                }
            }
            return uuid
        }
        return null
    }
}