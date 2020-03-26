package com.angcyo.game.core

import android.graphics.Canvas
import com.angcyo.game.layer.BaseLayer
import com.angcyo.game.layer.BaseLayer.Companion.LAYER_STATUS_PAUSE_DRAW
import com.angcyo.game.layer.BaseLayer.Companion.LAYER_STATUS_PAUSE_UPDATE
import com.angcyo.library.ex.have
import java.util.concurrent.CopyOnWriteArrayList

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/03/09
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
class GameLayerManager(val engine: GameRenderEngine) {

    val layerList = CopyOnWriteArrayList<BaseLayer>()

    var _drawParams: DrawParams? = null
    var _updateParams: UpdateParams? = null

    /**引擎参数更新*/
    fun onEngineUpdate(engineParams: EngineParams) {
        val iterator = layerList.iterator()
        while (iterator.hasNext()) {
            iterator.next()?.also { layer ->
                layer.onLayerUpdate(engineParams.engineWidth, engineParams.engineHeight)
            }
        }
    }

    fun addLayer(layer: BaseLayer) {
        layerList.add(layer)
        layer.onLayerUpdate(engine._engineParams.engineWidth, engine._engineParams.engineHeight)
    }

    fun removeLayer(layer: BaseLayer) {
        layerList.remove(layer)
    }

    fun clearLayer() {
        layerList.clear()
    }

    /**引擎绘制回调*/
    fun draw(canvas: Canvas) {
        if (_drawParams == null) {
            _drawParams = DrawParams(GameRenderEngine.engineTime())
        }
        _drawParams?.drawCurrentTime = GameRenderEngine.engineTime()

        val iterator = layerList.iterator()
        while (iterator.hasNext()) {
            iterator.next()?.also { layer ->
                if (!layer.layerStatus.have(LAYER_STATUS_PAUSE_DRAW)) {
                    layer.draw(canvas, _drawParams!!)
                }
            }
        }
        _drawParams?.drawPrevTime = _drawParams!!.drawCurrentTime
    }

    /**引擎计算回调*/
    fun update() {
        if (_updateParams == null) {
            _updateParams = UpdateParams(GameRenderEngine.engineTime())
        }
        _updateParams?.updateCurrentTime = GameRenderEngine.engineTime()

        val iterator = layerList.iterator()
        while (iterator.hasNext()) {
            iterator.next()?.also { layer ->
                if (!layer.layerStatus.have(LAYER_STATUS_PAUSE_UPDATE)) {
                    layer.update(_updateParams!!)
                }
            }
        }
        _updateParams?.updatePrevTime = _updateParams!!.updateCurrentTime
    }
}