package com.angcyo.laserpacker.device.ble

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerConfigHelper
import com.angcyo.bluetooth.fsc.laserpacker.bean._pathTolerance
import com.angcyo.core.fragment.lightStyle
import com.angcyo.dsladapter.renderEmptyItem
import com.angcyo.item.component.DebugAction
import com.angcyo.item.component.DebugFragment
import com.angcyo.laserpacker.device.R
import com.angcyo.laserpacker.device.ble.dslitem.DebugWifiConfigItem
import com.angcyo.laserpacker.device.ble.dslitem.DitherModeConfigItem
import com.angcyo.laserpacker.device.ble.dslitem.ExperimentalTopItem
import com.angcyo.library.component.hawk.LibHawkKeys
import com.angcyo.library.component.hawk.LibLpHawkKeys
import com.angcyo.library.ex._color
import com.angcyo.library.ex._string
import com.angcyo.library.ex.isDebug
import com.angcyo.library.ex.toDpi

/**
 * 实验性功能
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/12/09
 */
class EngraveExperimentalFragment : DebugFragment() {

    init {
        fragmentTitle = _string(R.string.engrave_experimental)

        fragmentConfig.isLightStyle = false
        fragmentConfig.showTitleLineView = false
        fragmentConfig.lightStyle()
        fragmentLayoutId = R.layout.fragment_engrave_experimental
    }

    override fun initBaseView(savedInstanceState: Bundle?) {
        super.initBaseView(savedInstanceState)
        //_vh.visible(R.id.background_view, !_isDarkMode)
    }

    override fun onInitFragment(savedInstanceState: Bundle?) {
        val themeWhite = _color(R.color.lib_theme_white_color)
        fragmentConfig.titleBarBackgroundDrawable = null
        fragmentConfig.fragmentBackgroundDrawable = ColorDrawable(themeWhite)
        super.onInitFragment(savedInstanceState)
        /*rootControl().view(R.id.lib_content_wrap_layout)?.background = dslGradientDrawable {
            gradientColors = intArrayOf(
                "#2d8dfb".toColor(),
                "#8ae7f6".toColor(),
                "#208ae7f6".toColor(),
                themeWhite,
                themeWhite,
                themeWhite
            )
            gradientOrientation = GradientDrawable.Orientation.TOP_BOTTOM
        }*/
    }

    override fun onCreateBehavior(child: View): CoordinatorLayout.Behavior<*>? {
        if (child.id == com.angcyo.core.R.id.lib_title_wrap_layout) {
            return null
        }
        return super.onCreateBehavior(child)
    }

    override fun renderActions() {
        renderDslAdapter {
            ExperimentalTopItem()()
            renderEmptyItem(30.toDpi())

            if (isDebug() || LaserPeckerConfigHelper.isOpenFun(HawkEngraveKeys::enableTransferIndexCheck.name)) {
                renderDebugAction(DebugAction().apply {
                    label = _string(R.string.transfer_index_check_label)
                    des = _string(R.string.transfer_index_check_des)
                    key = HawkEngraveKeys::enableTransferIndexCheck.name
                    type = Boolean::class.java
                    defValue = HawkEngraveKeys.enableTransferIndexCheck
                })
            }

            if (isDebug() || LaserPeckerConfigHelper.isOpenFun(HawkEngraveKeys::enableGCodeTransform.name)) {
                renderDebugAction(DebugAction().apply {
                    label = _string(R.string.gcode_transform_label)
                    des = _string(R.string.gcode_transform_des)
                    key = HawkEngraveKeys::enableGCodeTransform.name
                    type = Boolean::class.java
                    defValue = HawkEngraveKeys.enableGCodeTransform
                })
            }

            if (isDebug() || LaserPeckerConfigHelper.isOpenFun(LibLpHawkKeys::enableVectorArc.name)) {
                renderDebugAction(DebugAction().apply {
                    label = _string(R.string.vector_arc_label)
                    des = _string(R.string.vector_arc_des)
                    key = LibLpHawkKeys::enableVectorArc.name
                    type = Boolean::class.java
                    defValue = LibLpHawkKeys.enableVectorArc
                })
            }

            if (isDebug() || LaserPeckerConfigHelper.isOpenFun(LibLpHawkKeys::enableVectorRadiansSample.name)) {
                renderDebugAction(DebugAction().apply {
                    label = "激活矢量数据的弧度采样"
                    des = "激活后,弧形凸起变化达到(${_pathTolerance}mm)时,视为关键点"
                    key = LibLpHawkKeys::enableVectorRadiansSample.name
                    type = Boolean::class.java
                    defValue = LibLpHawkKeys.enableVectorRadiansSample
                })
            }

            if (isDebug() || LaserPeckerConfigHelper.isOpenFun(HawkEngraveKeys::enableItemTopOrder.name)) {
                renderDebugAction(DebugAction().apply {
                    label = _string(R.string.item_top_order_label)
                    des = _string(R.string.item_top_order_des)
                    key = HawkEngraveKeys::enableItemTopOrder.name
                    type = Boolean::class.java
                    defValue = HawkEngraveKeys.enableItemTopOrder
                })
            }

            if (isDebug() || LaserPeckerConfigHelper.isOpenFun(HawkEngraveKeys::enableRenderEngraveInfo.name)) {
                renderDebugAction(DebugAction().apply {
                    label = _string(R.string.render_engrave_info_label)
                    des = _string(R.string.render_engrave_info_des)
                    key = HawkEngraveKeys::enableRenderEngraveInfo.name
                    type = Boolean::class.java
                    defValue = HawkEngraveKeys.enableRenderEngraveInfo
                })
            }

            if (isDebug() || LaserPeckerConfigHelper.isOpenFun(HawkEngraveKeys::enableAgainEngravePreview.name)) {
                renderDebugAction(DebugAction().apply {
                    label = _string(R.string.again_engrave_preview_label)
                    des = _string(R.string.again_engrave_preview_des)
                    key = HawkEngraveKeys::enableAgainEngravePreview.name
                    type = Boolean::class.java
                    defValue = HawkEngraveKeys.enableAgainEngravePreview
                })
            }

            if (isDebug() || LaserPeckerConfigHelper.isOpenFun(HawkEngraveKeys::enableItemEngraveParams.name)) {
                renderDebugAction(DebugAction().apply {
                    label = _string(R.string.item_engrave_params_label)
                    des = _string(R.string.item_engrave_params_des)
                    key = HawkEngraveKeys::enableItemEngraveParams.name
                    type = Boolean::class.java
                    defValue = HawkEngraveKeys.enableItemEngraveParams
                })
            }

            if (isDebug() || LaserPeckerConfigHelper.isOpenFun(HawkEngraveKeys::enableSingleItemTransfer.name)) {
                renderDebugAction(DebugAction().apply {
                    label = _string(R.string.single_item_transfer_label)
                    des = _string(R.string.single_item_transfer_des)
                    key = HawkEngraveKeys::enableSingleItemTransfer.name
                    type = Boolean::class.java
                    defValue = HawkEngraveKeys.enableSingleItemTransfer

                    action = { _, value ->
                        if (value is Boolean) {
                            LibHawkKeys.enableCanvasRenderLimit = !value
                        }
                    }
                })
            }

            if (isDebug() || LaserPeckerConfigHelper.isOpenFun(HawkEngraveKeys::enableWifiFunConfig.name)) {
                DebugWifiConfigItem()() {
                    initItem()
                }
            }

            if (isDebug() || LaserPeckerConfigHelper.isOpenFun(HawkEngraveKeys::ditherModeConfig.name)) {
                DitherModeConfigItem()() {
                    initItem()
                }
            }
        }
    }
}