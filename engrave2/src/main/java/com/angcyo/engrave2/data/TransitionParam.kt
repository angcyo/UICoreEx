package com.angcyo.engrave2.data

import android.graphics.Matrix
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.core.vmApp
import com.angcyo.library.annotation.MM
import com.angcyo.library.annotation.Pixel
import com.angcyo.library.component.hawk.LibHawkKeys
import com.angcyo.library.unit.toMm
import com.angcyo.library.unit.toPixel

/**转换需要的一些额外参数
 *
 * [com.angcyo.engrave2.transition.EngraveTransitionHelper]
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/10/10
 */
data class TransitionParam(

    //---抖动算法需要的参数---

    /**进行图片转抖动时, 图片是否已经反色了*/
    val isBitmapInvert: Boolean = true,

    /**是否反色, 用来决定进入抖动算法前, 图片透明颜色应该使用什么颜色填充*/
    val invert: Boolean = false,

    /**对比度 [-1~1]*/
    val contrast: Float = 0f,

    /**亮度 [-1~1]*/
    val brightness: Float = 0f,

    /**是否使用新的抖动算法*/
    val useNewDithering: Boolean = HawkEngraveKeys.useNewDithering,

    //---GCode算法需要的参数---

    /**是否仅使用图片转GCode的方式处理, 这样会强制忽略[android.graphics.Path]的转换
     * 否则会自动优先先使用[android.graphics.Path]转GCode,
     * 然后在使用[android.graphics.Bitmap]转GCode*/
    val onlyUseBitmapToGCode: Boolean = false,

    /**在处理图片转GCode时, 是否使用OpenCV的算法 */
    val useOpenCvHandleGCode: Boolean = true,

    /**转GCode时, 是否要自动开关激光*/
    val isAutoCnc: Boolean = vmApp<LaserPeckerModel>().isCSeries(),

    /**图片转GCode时, 是否是简单的线生成的图片.
     * 如果是线条生成的图片, 则开启此开关, 会有优化处理. 尤其是虚线
     * 只在[useOpenCvHandleGCode=false]的情况下有效
     * */
    val isSingleLine: Boolean = false,

    /**使用图片像素转GCode时, 扫描像素的步长*/
    @Pixel
    val pixelGCodeGapValue: Float = LibHawkKeys.pathPixelGapValue.toPixel(),

    /**是否激活压缩输出GCode
     * [com.angcyo.engrave2.transition.EngraveTransitionHelper.transitionToGCode]*/
    val enableGCodeShrink: Boolean = HawkEngraveKeys.enableGCodeShrink,

    /**是否使用GCode切割数据*/
    val enableGCodeCutData: Boolean = false,

    /**GCode数据额外需要偏移的距离*/
    @Pixel
    val gcodeOffsetLeft: Float = 0f,

    @Pixel
    val gcodeOffsetTop: Float = 0f,
) {

    /**需要平移的矩阵信息*/
    @MM
    val translateMatrix: Matrix?
        get() = if (gcodeOffsetLeft != 0f || gcodeOffsetTop != 0f) {
            Matrix().apply {
                postTranslate(gcodeOffsetLeft.toMm(), gcodeOffsetTop.toMm())
            }
        } else {
            null
        }

    /**需要平移的矩阵信息*/
    @Pixel
    val translatePixelMatrix: Matrix?
        get() = if (gcodeOffsetLeft != 0f || gcodeOffsetTop != 0f) {
            Matrix().apply {
                postTranslate(gcodeOffsetLeft, gcodeOffsetTop)
            }
        } else {
            null
        }
}
