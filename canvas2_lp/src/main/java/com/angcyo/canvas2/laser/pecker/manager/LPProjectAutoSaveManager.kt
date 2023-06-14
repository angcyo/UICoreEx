package com.angcyo.canvas2.laser.pecker.manager

import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.library.annotation.CallPoint
import com.angcyo.library.component._removeMainRunnable
import com.angcyo.library.component._runMainRunnableDelay
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2023/05/07
 */
object LPProjectAutoSaveManager {

    /**是否使用文件夹的方式存储临时工程*/
    var useFolderSave = true

    /**是否正在存储工程
     * [com.angcyo.canvas2.laser.pecker.manager.LPProjectManager.saveProjectV2]*/
    val isSaveBoolean = AtomicBoolean(false)

    private var autoSaveRunnable: AutoSaveRunnable? = null

    /**自动保存工程*/
    @CallPoint
    fun autoSave(renderDelegate: CanvasRenderDelegate, async: Boolean) {
        if (!HawkEngraveKeys.enableProjectAutoSave) {
            //未激活自动保存
            return
        }
        if (isSaveBoolean.get()) {
            //正在保存
            return
        }
        if (useFolderSave) {
            LPProjectManager().saveProjectV2Folder(renderDelegate)
        } else {
            removeAutoSave()
            autoSaveRunnable = AutoSaveRunnable(renderDelegate, async)
            if (async) {
                _runMainRunnableDelay(HawkEngraveKeys.autoSaveProjectDelay, autoSaveRunnable!!)
            } else {
                //立即保存
                autoSaveRunnable?.run()
            }
        }
    }

    /**自动恢复*/
    @CallPoint
    fun autoRestore(renderDelegate: CanvasRenderDelegate) {
        try {
            if (useFolderSave) {
                LPProjectManager().restoreProjectV2Folder(renderDelegate)
            } else {
                LPProjectManager().restoreProjectV2(renderDelegate)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**移除自动保存动作*/
    private fun removeAutoSave() {
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
                    //保存完之后, 刷新界面
                    renderDelegate.refresh()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}