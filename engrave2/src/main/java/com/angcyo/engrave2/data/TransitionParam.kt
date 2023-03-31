package com.angcyo.engrave2.data

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.core.vmApp

/**转换需要的一些额外参数
 *
 * [com.angcyo.engrave2.transition.EngraveTransitionHelper]
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/10/10
 */
data class TransitionParam(

    //---抖动算法需要的参数---

    /**是否反色, 用来决定进入抖动算法前, 图片透明颜色应该使用什么颜色填充*/
    val invert: Boolean = false,

    //---GCode算法需要的参数---

    /**是否仅使用图片转GCode的方式处理,
     * 否则会自动优先先使用[android.graphics.Path]转GCode,
     * 然后在使用[android.graphics.Bitmap]转GCode*/
    val onlyUseBitmapToGCode: Boolean = false,

    /**在处理图片转GCode时, 是否使用OpenCV的算法 */
    val useOpenCvHandleGCode: Boolean = true,

    /**转GCode时, 是否要自动开关激光*/
    val isAutoCnc: Boolean = vmApp<LaserPeckerModel>().isC1(),

    /**图片转GCode时, 是否是简单的线生成的图片.
     * 如果是线条生成的图片, 则开启此开关, 会有优化处理. 尤其是虚线
     * 只在[useOpenCvHandleGCode=false]的情况下有效
     * */
    val isSingleLine: Boolean = false
)
