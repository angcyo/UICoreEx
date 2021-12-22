package com.angcyo.script

import android.content.Context
import com.angcyo.library.L
import com.angcyo.library.app
import com.angcyo.script.annotation.ScriptInject
import com.eclipsesource.v8.V8
import com.eclipsesource.v8.V8Array
import com.eclipsesource.v8.V8Object
import com.eclipsesource.v8.V8Value


/**
 * Java JavaScript 互操作, Java 解析 js脚本并执行
 *
 * https://github.com/cashapp/zipline
 *
 * https://eclipsesource.com/blogs/tutorials/getting-started-with-j2v8/
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/12/22
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class DslScript {

    lateinit var v8: V8

    //对象保持, 用来释放
    val v8ObjectHoldSet = mutableSetOf<V8Value>()

    /**初始化引擎*/
    fun initEngine(context: Context) {
        val dir = context.getExternalFilesDir("v8")
        v8 = V8.createV8Runtime("v8", dir?.absolutePath)
    }

    /**释放引擎资源*/
    fun releaseEngine() {
        v8ObjectHoldSet.forEach {
            it.release()
        }
        v8ObjectHoldSet.clear()
        v8.release(true)
    }

    /**运行脚本, 默认是主线程*/
    fun runScript(script: String) {
        val result = v8.executeScript(script)
        L.i("脚本返回:$result")
    }

    /**注入对象*/
    fun injectObj(obj: Any) {
        val clsInject = obj.javaClass.getAnnotation(ScriptInject::class.java)

        if (clsInject == null) {
            L.w("${obj.javaClass.name} 未使用 ${ScriptInject::class.java.name} 标注!")
            return
        }

        //注入对象
        var objKey: String = clsInject.key
        if (objKey.isEmpty()) {
            objKey = obj.javaClass.simpleName
        }

        v8.add(objKey, convertToV8Value(obj))
    }

    fun convertToV8Value(obj: Any?): V8Value? {
        val result: V8Value? = when (obj) {
            is Collection<*> -> {
                V8Array(v8).apply {
                    obj.forEach {
                        push(convertToV8Obj(it))
                    }
                }
            }
            is Array<*> -> {
                V8Array(v8).apply {
                    obj.forEach {
                        push(convertToV8Obj(it))
                    }
                }
            }
            is Map<*, *> -> {
                V8Object(v8).apply {
                    obj.forEach { entry ->
                        val key = entry.key
                        if (key is String) {
                            add(key, convertToV8Value(entry.value))
                        }
                    }
                }
            }
            else -> {
                convertToV8Obj(obj)
            }
        }
        result?.let { v8ObjectHoldSet.add(it) }
        return result
    }

    fun convertToV8Obj(obj: Any?): V8Object? {
        if (obj == null) {
            return null
        }

        val v8Obj = V8Object(v8)
        v8ObjectHoldSet.add(v8Obj)

        obj.javaClass.apply {
            for (f in declaredFields) {
                f.isAccessible = true
                val fInject = f.getAnnotation(ScriptInject::class.java)
                if (fInject != null) {
                    //注入对象的属性
                    var key: String = fInject.key
                    if (key.isEmpty()) {
                        key = f.name
                    }

                    val value = f.get(obj)

                    when {
                        value == null -> v8Obj.addNull(key)
                        Boolean::class.java.isAssignableFrom(f.type) -> v8Obj.add(
                            key,
                            value as Boolean
                        )
                        Int::class.java.isAssignableFrom(f.type) -> v8Obj.add(key, value as Int)
                        Long::class.java.isAssignableFrom(f.type) -> v8Obj.add(
                            key,
                            (value as Long).toInt()
                        )
                        Float::class.java.isAssignableFrom(f.type) -> v8Obj.add(
                            key,
                            (value as Float).toDouble()
                        )
                        Double::class.java.isAssignableFrom(f.type) -> v8Obj.add(
                            key,
                            value as Double
                        )
                        Number::class.java.isAssignableFrom(f.type) -> v8Obj.add(
                            key,
                            (value as Number).toInt()
                        )
                        String::class.java.isAssignableFrom(f.type) -> v8Obj.add(
                            key,
                            value as String
                        )
                        else -> v8Obj.add(key, convertToV8Value(value))
                    }
                }
            }
            for (m in declaredMethods) {
                m.isAccessible = true
                val mInject = m.getAnnotation(ScriptInject::class.java)
                if (mInject != null) {
                    var key: String = mInject.key
                    if (key.isEmpty()) {
                        key = m.name
                    }

                    val v8Callback = v8Obj.registerJavaMethod(
                        obj,
                        m.name,
                        key,
                        m.parameterTypes,
                        mInject.includeReceiver
                    )

                    v8ObjectHoldSet.add(v8Callback)
                }
            }
            //v8Obj.add()
        }

        return v8Obj
    }

    /**初始化引擎*/
    fun wrapEngine() {
        val v8 = V8.createV8Runtime()

        /*val runtime = V8.createV8Runtime()


        runtime.add("", V8Value)
        runtime.registerJavaMethod()

        val result = runtime.executeIntegerScript(
            """
              var hello = 'hello, ';
              var world = 'world!';
              hello.concat(world).length;
              """.trimIndent()
        )

        //runtime.executeScript()

        runtime.getObject()//.release()

        println(result)

        runtime.release()*/
    }
}

/**脚本*/
fun script(context: Context = app(), action: DslScript.() -> Unit): DslScript {
    return DslScript().apply {
        initEngine(context)
        action(this)
        releaseEngine()
    }
}

