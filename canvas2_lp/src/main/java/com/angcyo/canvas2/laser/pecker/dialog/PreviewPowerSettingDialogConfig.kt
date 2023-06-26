package com.angcyo.canvas2.laser.pecker.dialog

import android.app.Dialog
import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import com.angcyo.bluetooth.fsc.enqueue
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker.command.FactoryCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.toLaserPeckerPower
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.engrave.EngraveLaserSegmentItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.preview.PreviewBrightnessItem
import com.angcyo.dialog.SingleRecyclerDialogConfig
import com.angcyo.dialog.configBottomDialog
import com.angcyo.dsladapter.DslAdapter
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.library.annotation.DSL
import com.angcyo.library.toast
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.progress.DslSeekBar

/**
 * 工厂设置, 预览光功率设置
 * [com.angcyo.bluetooth.fsc.laserpacker.command.FactoryCmd.Companion.previewPowerSettingCmd]
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/06/26
 */
class PreviewPowerSettingDialogConfig : SingleRecyclerDialogConfig() {

    init {
        dialogTitle = "激光点预览功率设置"
        configBottomDialog()
        hideControlButton = true
    }

    override fun onInitRecyclerAdapter(
        dialog: Dialog,
        holder: DslViewHolder,
        recyclerView: RecyclerView,
        adapter: DslAdapter
    ) {
        adapter.apply {

            //激光点类型切换
            EngraveLaserSegmentItem()() {
                observeItemChange {
                    val type = currentLaserTypeInfo().type
                    HawkEngraveKeys.lastType = type.toInt()
                    updatePreviewPowerSetting(it)
                }
            }

            //激光点功率调整
            PreviewBrightnessItem()() {
                itemProgressTextFormatAction = {
                    "${(it._progressFraction * 255).toInt()}"
                }
                itemBindOverride = { itemHolder, _, _, _ ->
                    itemHolder.tv(R.id.lib_text_view)?.text = "激光功率"
                    itemHolder.v<DslSeekBar>(R.id.lib_seek_view)?.apply {
                        showProgressText = true
                    }
                }
                observeItemChange(this@PreviewPowerSettingDialogConfig::updatePreviewPowerSetting)
            }
        }
    }

    /**发送指令*/
    fun updatePreviewPowerSetting(item: DslAdapterItem) {
        FactoryCmd.previewPowerSettingCmd(
            HawkEngraveKeys.lastPwrProgress.toLaserPeckerPower(),
            HawkEngraveKeys.lastType.toByte()
        ).enqueue { bean, error ->
            error?.let { toast(it.message) }
        }
    }
}

@DSL
fun Context.previewPowerSettingDialog(): Dialog {
    return PreviewPowerSettingDialogConfig().run {
        dialogContext = this@previewPowerSettingDialog
        show()
    }
}