package com.angcyo.canvas2.laser.pecker.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dialog.dslitem.MagicToleranceItem
import com.angcyo.dialog.DslDialogConfig
import com.angcyo.dialog.configBottomDialog
import com.angcyo.doodle.ui.dslitem.DoodleIconItem
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.library.component.Strategy
import com.angcyo.library.component.UndoManager
import com.angcyo.library.ex._string
import com.angcyo.rust.handle.RustBitmapHandle
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.base.addDslItem
import com.angcyo.widget.base.resetDslItem
import com.angcyo.widget.image.TouchImageView
import kotlin.math.roundToInt

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023-05-28
 */
class MagicWandDialogConfig(context: Context? = null) : DslDialogConfig(context) {

    /**需要魔棒处理的图片*/
    var originBitmap: Bitmap? = null

    private var resultBitmap: Bitmap? = null

    /**色差*/
    private var fuzziness: Int = 20

    /**图片的回调*/
    var onMagicResultAction: (bitmap: Bitmap?) -> Unit = {}

    private val undoManager = UndoManager().apply {
        onUndoRedoChangeAction = {
            undoItemList[0].itemEnable = canUndo()
            undoItemList[1].itemEnable = canRedo()
            updateUndoLayout(_dialogViewHolder)
        }
    }

    //undo redo
    val undoItemList = mutableListOf<DslAdapterItem>()

    init {
        dialogLayoutId = R.layout.dialog_magic_wand
        undoItemList.apply {
            add(DoodleIconItem().apply {
                itemIco = R.drawable.doodle_undo
                itemText = _string(R.string.doodle_undo)
                itemEnable = false
                itemClick = {
                    undoManager.undo()
                }
            })
            add(DoodleIconItem().apply {
                itemIco = R.drawable.doodle_redo
                itemText = _string(R.string.doodle_redo)
                itemEnable = false
                itemClick = {
                    undoManager.redo()
                }
            })
        }
    }

    override fun initDialogView(dialog: Dialog, dialogViewHolder: DslViewHolder) {
        super.initDialogView(dialog, dialogViewHolder)
        updateResultImage(originBitmap, dialogViewHolder)

        //
        dialogViewHolder.group(R.id.tolerance_wrap_layout)?.let { layout ->
            layout.addDslItem(MagicToleranceItem().apply {
                itemValue = fuzziness.toFloat()
                itemParentLayout = layout
                itemValueChangeAction = {
                    fuzziness = it.roundToInt()
                }
            })
        }
        updateUndoLayout(dialogViewHolder)

        //
        dialogViewHolder.click(R.id.lib_cancel_view) {
            dialog.cancel()
        }
        dialogViewHolder.click(R.id.lib_confirm_view) {
            resultBitmap?.let {
                onMagicResultAction(it)
            }
            dialog.dismiss()
        }
    }

    override fun onDialogCancel(dialog: Dialog, dialogViewHolder: DslViewHolder) {
        super.onDialogCancel(dialog, dialogViewHolder)
        onMagicResultAction(null)
    }

    private fun updateResultImage(bitmap: Bitmap?, viewHolder: DslViewHolder) {
        resultBitmap = bitmap
        viewHolder.v<TouchImageView>(R.id.lib_image_view)?.apply {
            setImageBitmap(bitmap)
            onTouchPointAction = { point ->
                bitmap?.let {

                    //魔棒处理
                    val newBitmap = RustBitmapHandle.bitmapMagicWand(
                        it,
                        intArrayOf(point.x.toInt(), point.y.toInt()),
                        fuzziness
                    )
                    val oldBitmap = bitmap
                    undoManager.addAndRedo(Strategy.normal, true, {
                        updateResultImage(oldBitmap, viewHolder)
                    }, {
                        updateResultImage(newBitmap, viewHolder)
                    })
                }
            }
        }
    }

    /**undo redo*/
    private fun updateUndoLayout(viewHolder: DslViewHolder?) {
        viewHolder?.group(R.id.undo_wrap_layout)?.resetDslItem(undoItemList)
    }
}

fun Context.magicWandDialog(config: MagicWandDialogConfig.() -> Unit): Dialog {
    return MagicWandDialogConfig().run {
        configBottomDialog(this@magicWandDialog)
        dialogWidth = -1
        dialogHeight = -1
        dialogThemeResId = R.style.LibDialogBaseFullTheme
        config()
        show()
    }
}