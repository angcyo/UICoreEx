package com.angcyo.engrave2.data

import android.graphics.RectF
import com.angcyo.engrave2.data.PreviewInfo.Companion.Z_STATE_CONTINUE
import com.angcyo.engrave2.data.PreviewInfo.Companion.Z_STATE_PAUSE
import com.angcyo.engrave2.data.PreviewInfo.Companion.Z_STATE_SCROLL
import com.angcyo.library.annotation.Implementation
import com.angcyo.library.annotation.Pixel

/**
 * 预览的一些信息
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/24
 */
data class PreviewInfo(

    /**未旋转时的矩形*/
    @Pixel
    var originBounds: RectF? = null,

    /**多元素预览时*/
    @Pixel
    var boundsList: List<RectF>? = null,

    /**是否是更新pwr指令*/
    var updatePwr: Boolean = false,

    /**所有元素的bounds*/
    @Pixel
    var elementBoundsList: List<RectF>? = null,

    //---

    /**是否处于/需要中心点预览, 需要额外适配C1*/
    var isCenterPreview: Boolean = false,

    /**第三轴是否处于暂停状态, 需要处于的状态
     * null 表示不处于第三轴预览状态
     * [Z_STATE_PAUSE]
     * [Z_STATE_CONTINUE]
     * [Z_STATE_SCROLL]
     * */
    var zState: Int? = null,

    /**是否开始了预览
     * [zState] 只有开始了预览, 才能直接发送z轴暂停/继续滚动指令*/
    var isStartPreview: Boolean = false,

    //---

    /**预览的数据, 如果有,用于实现路径预览的判断条件
     * [com.angcyo.engrave.dslitem.preview.PreviewControlItem]
     * [com.angcyo.canvas2.laser.pecker.engrave.dslitem.preview.PreviewControlItem]
     * */
    var rendererUuid: String? = null,

    //---

    /**预览时, 单元素的图层id, 用来实现GCode数据偏移预览
     * [com.angcyo.canvas2.laser.pecker.engrave.dslitem.preview.GCodeDataOffsetItem.offsetTop]
     * [com.angcyo.canvas2.laser.pecker.engrave.dslitem.preview.GCodeDataOffsetItem.offsetLeft]
     * */
    @Implementation
    var elementLayerId: String? = null
) {
    companion object {
        /**第三轴状态:暂停滚动*/
        const val Z_STATE_PAUSE = 1

        /**第三轴状态:继续滚动*/
        const val Z_STATE_CONTINUE = 2

        /**第三轴状态: C1滚动*/
        const val Z_STATE_SCROLL = 3
    }
}