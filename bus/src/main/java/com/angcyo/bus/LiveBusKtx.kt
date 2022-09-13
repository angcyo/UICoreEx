package com.angcyo.bus

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.angcyo.library.L
import com.jeremyliao.liveeventbus.LiveEventBus
import com.jeremyliao.liveeventbus.core.LiveEvent
import com.jeremyliao.liveeventbus.core.Observable
import kotlin.reflect.KClass

/**
 * https://github.com/JeremyLiao/LiveEventBus
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/23
 */

/***
 *
 * LiveEventBus
 *   .get("some_key", String.class)
 *   .observe(this, new Observer<String>() {
 *     @Override
 *     public void onChanged(@Nullable String s) {
 *     }
 *   });
 *
 * LiveEventBus
 *   .get("some_key", String.class)
 *   .observeForever(observer);
 *
 * LiveEventBus
 *   .get("some_key")
 *   .post(some_value);
 *
 * LiveEventBus
 *   .get(DemoEvent.class)
 *   .post(new DemoEvent("Hello world"));
 *
 * */

/**https://github.com/JeremyLiao/LiveEventBus/blob/master/docs/config.md*/
fun initLiveBusConfig() {
    LiveEventBus.config().apply {
        enableLogger(L.debug)
        autoClear(false)
        lifecycleObserverAlwaysActive(true)
        //setContext()
    }
}

//region ------liveBus------

/**获取一个可以被观察的消息总线对象*/
fun <T> String.liveBus(type: Class<T>): Observable<T> = LiveEventBus.get(this, type)

fun <T : Any> String.liveBus(type: KClass<T>): Observable<T> = liveBus(type.java)

fun <T : LiveEvent> Class<T>.liveBus(): Observable<T> = LiveEventBus.get(this)

fun <T : LiveEvent> KClass<T>.liveBus(): Observable<T> = java.liveBus()

/**直接发布自己*/
inline fun <reified T : LiveEvent> T.busPost() {
    T::class.busPost(this)
}

//endregion

//region ------post------

/**发送[value]到指定的key*/
fun String.busPostAny(value: Any?) {
    liveBus(Any::class).post(value)
}

inline fun <reified T> String.busPost(value: T?) {
    liveBus(T::class.java).post(value)
}

/**发送[value]到指定的类型对应的key*/
fun <T : LiveEvent> Class<T>.busPost(value: T?) {
    liveBus().post(value)
}

fun <T : LiveEvent> KClass<T>.busPost(value: T?) {
    liveBus().post(value)
}

//endregion

//region ------observe------

//observer
fun <T> Observable<T>.doObserver(
    owner: LifecycleOwner? = null,
    sticky: Boolean = false,
    observer: Observer<T?>
) {
    if (owner == null) {
        if (sticky) {
            observeStickyForever(observer)
        } else {
            observeForever(observer)
        }
    } else {
        if (sticky) {
            observeSticky(owner, observer)
        } else {
            observe(owner, observer)
        }
    }
}

/**观察一个事件总线的消息事件
 * [owner] 声明周期提供者, 如果不传, 表示全局监听. 可能会有内存泄漏风险*/
fun String.busObserveAny(
    owner: LifecycleOwner? = null,
    sticky: Boolean = false,
    observer: Observer<Any?>
): Observer<Any?> {
    liveBus(Any::class).doObserver(owner, sticky, observer)
    //removeObserver(observer)
    return observer
}

fun <T> String.busObserve(
    type: Class<T>,
    owner: LifecycleOwner? = null,
    sticky: Boolean = false,
    observer: Observer<T?>
): Observer<T?> {
    liveBus(type).doObserver(owner, sticky, observer)
    //removeObserver(observer)
    return observer
}

fun <T : Any> String.busObserve(
    type: KClass<T>,
    owner: LifecycleOwner? = null,
    sticky: Boolean = false,
    observer: Observer<T?>
): Observer<T?> {
    liveBus(type).doObserver(owner, sticky, observer)
    //removeObserver(observer)
    return observer
}

inline fun <reified T> String.busObserve(
    owner: LifecycleOwner? = null,
    sticky: Boolean = false,
    observer: Observer<T?>
): Observer<T?> {
    liveBus(T::class.java).doObserver(owner, sticky, observer)
    //removeObserver(observer)
    return observer
}

fun <T : LiveEvent> Class<T>.busObserve(
    owner: LifecycleOwner? = null,
    sticky: Boolean = false,
    observer: Observer<T?>
): Observer<T?> {
    liveBus().doObserver(owner, sticky, observer)
    //removeObserver(observer)
    return observer
}

fun <T : LiveEvent> KClass<T>.busObserve(
    owner: LifecycleOwner? = null,
    sticky: Boolean = false,
    observer: Observer<T?>
): Observer<T?> {
    liveBus().doObserver(owner, sticky, observer)
    //removeObserver(observer)
    return observer
}

//endregion
