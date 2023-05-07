package com.angcyo.canvas2.laser.pecker.manager

import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.library.component._removeMainRunnable
import com.angcyo.library.component._runMainRunnableDelay
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2023/05/07
 */
object LPProjectAutoSaveManager {

    /**自动保存延迟时长*/
    var autoSaveDelay = 1_000L

    /**是否正在存储工程*/
    val isSaveBoolean = AtomicBoolean(false)

    private var autoSaveRunnable: AutoSaveRunnable? = null

    /**自动保存工程*/
    fun autoSave(renderDelegate: CanvasRenderDelegate, async: Boolean) {
        if (isSaveBoolean.get()) {
            //正在保存
            return
        }
        removeAutoSave()
        autoSaveRunnable = AutoSaveRunnable(renderDelegate, async)
        if (async) {
            _runMainRunnableDelay(autoSaveDelay, autoSaveRunnable!!)
        } else {
            //立即保存
            autoSaveRunnable?.run()
        }
    }

    /**移除自动保存动作*/
    fun removeAutoSave() {
        autoSaveRunnable?.let {
            _removeMainRunnable(it)
        }
        autoSaveRunnable = null
    }

    /**保存行为*/
    private class AutoSaveRunnable(val renderDelegate: CanvasRenderDelegate, val async: Boolean) :
        Runnable {
        override fun run() {
            try {
                LPProjectManager().saveProjectV2(
                    renderDelegate,
                    async = async
                ) { zipFilePath, exception ->
                    removeAutoSave()
                    if (exception != null) {
                        //保存失败
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}