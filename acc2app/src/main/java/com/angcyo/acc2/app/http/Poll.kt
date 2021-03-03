package com.angcyo.acc2.app.http

import com.angcyo.acc2.app.app
import com.angcyo.http.rx.BaseFlowableSubscriber
import com.angcyo.http.rx.observer
import com.angcyo.library.L
import io.reactivex.Flowable
import io.reactivex.disposables.Disposable
import java.util.concurrent.TimeUnit


/**
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/01/19
 */
object Poll {

    var _disposable: Disposable? = null

    fun init() {
        poll()

        if (!L.debug) {

        }
    }

    /**开始轮询*/
    fun poll() {
        _disposable?.dispose()
        _disposable = Flowable.interval(app().memoryConfigBean.pollTime, TimeUnit.SECONDS)
            .doOnNext {
                //L.i("poll...$it")
                Message.fetchMessage()
            }
            .observer(BaseFlowableSubscriber())
    }

    fun release() {
        _disposable?.dispose()
    }
}