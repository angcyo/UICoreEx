package com.angcyo.pine

import com.angcyo.library.annotation.CallPoint
import com.angcyo.library.ex.isDebug
import top.canyie.pine.Pine
import top.canyie.pine.PineConfig
import top.canyie.pine.callback.MethodHook
import java.lang.reflect.Method

/**
 *
 * ```
 *  RPine.init()
 *  hookBefore(Test::class.java.getDeclaredMethod("test")) {
 *      L.w("hook test↓")
 *      L.i(stackTraceString())
 *  }
 * ````
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2023/09/24
 */
object RPine {

    /**初始化入口点*/
    @CallPoint
    fun init(debug: Boolean = isDebug()) {
        // 是否debug，true会输出较详细log
        PineConfig.debug = debug
        // 该应用是否可调试，建议和配置文件中的值保持一致，否则会出现问题
        PineConfig.debuggable = false //debug //isAppDebug()//部分手机会出现问题
    }
}

/**拦截一个对象的指定方法
 * [top.canyie.pine.callback.MethodReplacement.DO_NOTHING]
 * */
fun hookBefore(method: Method, action: Pine.CallFrame.() -> Unit) {
    Pine.hook(method, object : MethodHook() {
        override fun beforeCall(callFrame: Pine.CallFrame) {
            //super.beforeCall(callFrame)
            callFrame.action()
            //callFrame.result
        }

        override fun afterCall(callFrame: Pine.CallFrame) {
            super.afterCall(callFrame)
        }
    })
}

fun Any.hookBefore(methodName: String, action: Pine.CallFrame.() -> Unit) {
    hookBefore(javaClass.getDeclaredMethod(methodName), action)
}