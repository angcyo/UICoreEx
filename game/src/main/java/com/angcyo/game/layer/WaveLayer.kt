package com.angcyo.game.layer

import com.angcyo.game.core.UpdateParams
import com.angcyo.game.spirit.WaveSpirit

/**
 * 控制波纹的层
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/03/26
 */

open class WaveLayer : BaseLayer() {

    /**波纹添加间隔时间*/
    var waveInterval = 800

    var _waveLastAddTime: Long = 0

    override fun update(updateParams: UpdateParams) {
        super.update(updateParams)
        if (updateParams.updateCurrentTime - _waveLastAddTime >= waveInterval) {
            addSpirit(WaveSpirit())
            _waveLastAddTime = updateParams.updateCurrentTime
        }
    }
}