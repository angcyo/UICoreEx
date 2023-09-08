package com.angcyo.canvas2.laser.pecker.dialog

import android.app.Dialog
import android.content.Context
import android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
import com.angcyo.canvas2.laser.pecker.BuildConfig
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dialog.dslitem.VariableTextAddItem
import com.angcyo.canvas2.laser.pecker.dialog.dslitem.VariableTextEditItem
import com.angcyo.canvas2.laser.pecker.dialog.dslitem.VariableTextListItem
import com.angcyo.canvas2.laser.pecker.dialog.dslitem._itemVariableBean
import com.angcyo.canvas2.laser.pecker.util.LPElementHelper
import com.angcyo.canvas2.laser.pecker.util.lpTextElement
import com.angcyo.dialog.DslDialogConfig
import com.angcyo.dialog.configFullScreenDialog
import com.angcyo.dsladapter.DragCallbackHelper
import com.angcyo.dsladapter.DslAdapter
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.eachItem
import com.angcyo.dsladapter.find
import com.angcyo.dsladapter.renderAdapterEmptyStatus
import com.angcyo.dsladapter.updateItemSelected
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.laserpacker.bean.LPVariableBean
import com.angcyo.library.ex._drawable
import com.angcyo.library.ex._string
import com.angcyo.library.ex.dpi
import com.angcyo.library.ex.isDebugType
import com.angcyo.library.ex.isScreenTouchIn
import com.angcyo.library.ex.isTouchFinish
import com.angcyo.library.ex.isTouchMove
import com.angcyo.library.ex.postDelay
import com.angcyo.library.ex.replace
import com.angcyo.library.ex.resetAll
import com.angcyo.library.ex.setSize
import com.angcyo.library.ex.tintDrawable
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget._rv
import com.angcyo.widget.base._textColor
import com.angcyo.widget.layout.onDispatchTouchEventAction
import com.angcyo.widget.recycler.DslRecyclerView
import com.angcyo.widget.recycler.renderDslAdapter
import com.angcyo.widget.span.span

/**
 * 变量模板界面弹窗
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/08/30
 */
class VariableTextDialogConfig(context: Context? = null) : DslDialogConfig(context) {

    private var _listAdapter: DslAdapter? = null
    private var _controlAdapter: DslAdapter? = null
    private var _dragCallbackHelper: DragCallbackHelper? = null

    /**变量文件数据结构集合*/
    var variableTextBeanList = mutableListOf<LPVariableBean>()

    /**编辑模式*/
    private var isVarItemEditMode = false

    /**应用回调*/
    var onApplyVariableListAction: (List<LPVariableBean>) -> Unit = {}

    private val haveData: Boolean
        get() = variableTextBeanList.isNotEmpty()

    init {
        dialogLayoutId = R.layout.variable_text_dialog_layout
        softInputMode = SOFT_INPUT_ADJUST_NOTHING
        canceledOnTouchOutside = false
    }

    override fun initDialogView(dialog: Dialog, dialogViewHolder: DslViewHolder) {
        super.initDialogView(dialog, dialogViewHolder)
        dialogViewHolder.tv(R.id.dialog_title_view)?.text = _string(R.string.canvas_variable_text)
        dialogViewHolder.enable(R.id.dialog_positive_button, false)

        //确定
        dialogViewHolder.click(R.id.dialog_positive_button) {
            onApplyVariableListAction(variableTextBeanList)
            dialog.dismiss()
        }

        //back
        dialogViewHolder.click(R.id.dialog_negative_button) {
            dialog.dismiss()
        }

        //rv
        dialogViewHolder._rv(R.id.lib_recycler_view)?.apply {
            _dragCallbackHelper = DragCallbackHelper.install(this, DragCallbackHelper.FLAG_VERTICAL)
            _dragCallbackHelper?.enableLongPressDrag = false

            _dragCallbackHelper?.onClearViewAction = { _, _ ->
                if (_dragCallbackHelper?._dragHappened == true) {
                    //发生过拖拽
                    val list = mutableListOf<LPVariableBean>()
                    _listAdapter?.eachItem { index, dslAdapterItem ->
                        dslAdapterItem._itemVariableBean?.let {
                            list.add(it)
                        }
                    }
                    variableTextBeanList.resetAll(list)
                    updatePreviewLayout()
                }
            }

            //用来实现移动到此删除元素
            observeDrag()

            renderDslAdapter {
                _listAdapter = this
                renderAdapterEmptyStatus(R.layout.variable_text_empty_layout)

                dslAdapterStatusItem.onItemStateChange = { from: Int, to: Int ->
                    updatePreviewLayout()
                }

                onDispatchUpdatesAfter {
                    updatePreviewLayout()
                }

                //初始化数据
                for (bean in variableTextBeanList) {
                    renderVariableTextListItem(bean)
                }
            }
        }

        //control
        dialogViewHolder._rv(R.id.variable_text_item_view)?.renderDslAdapter {
            _controlAdapter = this
            VariableTextAddItem()() {
                itemClick = {
                    it.context.addVariableTextDialog {
                        onApplyVariableAction = { bean ->
                            if (isVariableEditModel) {
                                _listAdapter?.updateAllItem()
                            } else {
                                //添加
                                variableTextBeanList.add(bean)
                                _listAdapter?.render {
                                    renderVariableTextListItem(bean)
                                }
                            }
                        }
                    }
                }
            }
            VariableTextEditItem()() {
                itemEnable = haveData
                itemIsSelected = isVarItemEditMode
                itemClick = {
                    isVarItemEditMode = !isVarItemEditMode
                    updateItemSelected(isVarItemEditMode)
                    _listAdapter?.eachItem { _, dslAdapterItem ->
                        if (dslAdapterItem is VariableTextListItem) {
                            dslAdapterItem.itemEditMode = isVarItemEditMode
                        }
                    }
                    _listAdapter?.updateAllItem()
                }
            }
        }
    }

    private fun DslAdapter.renderVariableTextListItem(bean: LPVariableBean) {
        VariableTextListItem()() {
            itemData = bean
            itemEditMode = isVarItemEditMode
            itemDragHelper = _dragCallbackHelper

            itemEditChangedAction = { newBean ->
                variableTextBeanList.replace(_itemVariableBean!!, newBean)
                itemData = newBean
                updateAdapterItem()
                updatePreviewLayout()
            }
        }
    }

    private var _isTouchMoveInTrash = false

    /**观察拖拽, 用来实现移动到此删除元素*/
    private fun DslRecyclerView.observeDrag() {
        onDispatchTouchEventAction {
            if (_dragCallbackHelper?._isStartDrag == true) {
                _dialogViewHolder?.visible(R.id.lib_trash_view)
                val textView = _dialogViewHolder?.tv(R.id.lib_trash_view)
                if (it.isTouchMove()) {
                    _isTouchMoveInTrash = it.isScreenTouchIn(textView)
                    val size = 20 * dpi
                    if (_isTouchMoveInTrash) {
                        textView?.text = span {
                            appendDrawable(
                                _drawable(R.drawable.core_trash_open_svg)
                                    .tintDrawable(textView!!._textColor)?.setSize(size)
                            )
                            append(_string(R.string.core_trash_delete_tip))
                        }
                    } else {
                        textView?.text = span {
                            appendDrawable(
                                _drawable(R.drawable.core_trash_svg)
                                    .tintDrawable(textView!!._textColor)?.setSize(size)
                            )
                            append(_string(R.string.core_trash_delete))
                        }
                    }
                } else if (it.isTouchFinish()) {
                    _dialogViewHolder?.gone(R.id.lib_trash_view)
                    if (_isTouchMoveInTrash) {
                        (_dragCallbackHelper?.dragTagData as? DslAdapterItem)?.apply {
                            //延迟删除, 避免界面bug
                            postDelay(300) {
                                _itemVariableBean?.let {
                                    variableTextBeanList.remove(it)
                                }
                                removeAdapterItemJust()
                            }
                        }
                    }
                }
            }
        }
    }

    /**更新预览*/
    private fun updatePreviewLayout() {
        _dialogViewHolder?.enable(R.id.dialog_positive_button, haveData)

        _controlAdapter?.find<VariableTextEditItem>()?.apply {
            itemEnable = haveData

            if (!itemEnable) {
                isVarItemEditMode = false
                updateItemSelected(false)
            } else {
                updateAdapterItem()
            }
        }

        val renderer = LPElementHelper.addVariableTextElement(
            null,
            variableTextBeanList,
            LPDataConstant.DATA_TYPE_VARIABLE_TEXT
        )
        _dialogViewHolder?.img(R.id.lib_preview_view)
            ?.setImageDrawable(renderer?.requestRenderDrawable())
        if (BuildConfig.BUILD_TYPE.isDebugType()) {
            _dialogViewHolder?.click(R.id.lib_preview_view) {
                renderer?.lpTextElement()?.updateElementAfterEngrave()
                _dialogViewHolder?.img(R.id.lib_preview_view)
                    ?.setImageDrawable(renderer?.requestRenderDrawable())
            }
        }
    }
}

/** 底部弹出涂鸦对话框 */
fun Context.variableTextDialog(config: VariableTextDialogConfig.() -> Unit): Dialog {
    return VariableTextDialogConfig().run {
        configFullScreenDialog(this@variableTextDialog)
        config()
        show()
    }
}