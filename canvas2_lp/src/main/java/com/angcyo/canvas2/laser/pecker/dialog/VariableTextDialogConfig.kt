package com.angcyo.canvas2.laser.pecker.dialog

import android.app.Dialog
import android.content.Context
import android.view.KeyEvent
import android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
import com.angcyo.bluetooth.fsc.laserpacker._deviceSettingBean
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
import com.angcyo.dialog.normalIosDialog
import com.angcyo.dsladapter.DragCallbackHelper
import com.angcyo.dsladapter.DslAdapter
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.eachItem
import com.angcyo.dsladapter.find
import com.angcyo.dsladapter.renderAdapterEmptyStatus
import com.angcyo.dsladapter.updateItemSelected
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.laserpacker.bean.LPVariableBean
import com.angcyo.library.ex._color
import com.angcyo.library.ex._drawable
import com.angcyo.library.ex._string
import com.angcyo.library.ex.dpi
import com.angcyo.library.ex.isDebugType
import com.angcyo.library.ex.isKeyUp
import com.angcyo.library.ex.isScreenTouchIn
import com.angcyo.library.ex.isTouchFinish
import com.angcyo.library.ex.isTouchMove
import com.angcyo.library.ex.longFeedback
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
 * 变量模板选中后的list界面弹窗, 支持变量元素的预览
 *
 * [com.angcyo.canvas2.laser.pecker.dialog.AddVariableTextDialogConfig]
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/08/30
 */
class VariableTextDialogConfig(context: Context? = null) : DslDialogConfig(context) {

    /**
     * 变量模板类型
     * [LPDataConstant.DATA_TYPE_VARIABLE_TEXT]
     * [LPDataConstant.DATA_TYPE_VARIABLE_QRCODE]
     * [LPDataConstant.DATA_TYPE_VARIABLE_BARCODE]
     * */
    var varElementType: Int = LPDataConstant.DATA_TYPE_VARIABLE_TEXT

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
        dialogViewHolder.tv(R.id.dialog_title_view)?.text = when (varElementType) {
            LPDataConstant.DATA_TYPE_VARIABLE_QRCODE -> _string(R.string.canvas_variable_qrcode)
            LPDataConstant.DATA_TYPE_VARIABLE_BARCODE -> _string(R.string.canvas_variable_barcode)
            else -> _string(R.string.canvas_variable_text)
        }
        dialogViewHolder.enable(R.id.dialog_positive_button, false)

        //确定
        dialogViewHolder.click(R.id.dialog_positive_button) {
            onApplyVariableListAction(variableTextBeanList)
            dialog.dismiss()
        }

        //back
        dialogViewHolder.click(R.id.dialog_negative_button) {
            showBackTipDialog(dialog)
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
                        addVarElementType = varElementType
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

    override fun onDialogKey(
        dialog: Dialog,
        dialogViewHolder: DslViewHolder,
        keyCode: Int,
        event: KeyEvent
    ): Boolean {
        if (event.isKeyUp() && showBackTipDialog(dialog)) {
            //返回键
            return true
        }
        return super.onDialogKey(dialog, dialogViewHolder, keyCode, event)
    }

    /**是否要显示退出确定对话框*/
    private fun showBackTipDialog(dialog: Dialog): Boolean {
        return if (variableTextBeanList.isNotEmpty()) {
            dialog.context.normalIosDialog {
                dialogTitle = _string(R.string.ui_warn)
                dialogMessage = _string(R.string.variable_back_tip)
                positiveButton { dialog2, dialogViewHolder ->
                    dialog2.dismiss()
                    dialog.dismiss()
                }
            }
            true
        } else {
            dialog.dismiss()
            false
        }
    }

    private fun DslAdapter.renderVariableTextListItem(bean: LPVariableBean) {
        VariableTextListItem()() {
            itemData = bean
            itemEditMode = isVarItemEditMode
            itemDragHelper = _dragCallbackHelper
            itemVarElementType = varElementType

            itemEditChangedAction = { newBean ->
                variableTextBeanList.replace(_itemVariableBean!!, newBean)
                itemData = newBean
                updateAdapterItem()
                updatePreviewLayout()
            }
        }
    }

    private var _isTouchMoveInTrash: Boolean? = null

    /**观察拖拽, 用来实现移动到此删除元素*/
    private fun DslRecyclerView.observeDrag() {
        onDispatchTouchEventAction {
            if (_dragCallbackHelper?._isStartDrag == true) {
                _dialogViewHolder?.visible(R.id.lib_trash_view)
                val textView = _dialogViewHolder?.tv(R.id.lib_trash_view)
                if (it.isTouchMove()) {
                    val isTouchIn = it.isScreenTouchIn(textView)
                    if (isTouchIn != _isTouchMoveInTrash) {
                        _isTouchMoveInTrash = isTouchIn
                        val size = 24 * dpi
                        if (_isTouchMoveInTrash == true) {
                            textView?.setBackgroundColor(_color(R.color.error_light))
                            textView?.text = span {
                                appendDrawable(
                                    _drawable(R.drawable.core_trash_open_svg)
                                        .tintDrawable(textView!!._textColor)?.setSize(size)
                                )
                                append(_string(R.string.core_trash_delete_tip))
                            }
                            textView?.longFeedback()
                        } else {
                            textView?.setBackgroundColor(_color(R.color.error))
                            textView?.text = span {
                                appendDrawable(
                                    _drawable(R.drawable.core_trash_svg)
                                        .tintDrawable(textView!!._textColor)?.setSize(size)
                                )
                                append(_string(R.string.core_trash_delete))
                            }
                        }
                    }
                } else if (it.isTouchFinish()) {
                    _dialogViewHolder?.gone(R.id.lib_trash_view)
                    if (_isTouchMoveInTrash == true) {
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
                    _isTouchMoveInTrash = null
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

        var beanList = variableTextBeanList
        if (variableTextBeanList.isEmpty()) {
            //默认预览条形码的内容
            if (varElementType == LPDataConstant.DATA_TYPE_VARIABLE_QRCODE || varElementType == LPDataConstant.DATA_TYPE_VARIABLE_BARCODE) {
                beanList = mutableListOf(LPVariableBean().apply {
                    if (varElementType == LPDataConstant.DATA_TYPE_VARIABLE_QRCODE) {
                        content = _deviceSettingBean?.barcode2DPreviewContent
                    } else {
                        content = _deviceSettingBean?.barcode1DPreviewContent
                    }
                })
            }
        }

        //获取对应的渲染器
        val renderer = LPElementHelper.addVariableTextElement(null, beanList, varElementType)
        val renderDrawable = renderer?.requestRenderDrawable()
        _dialogViewHolder?.img(R.id.lib_preview_view)?.setImageDrawable(renderDrawable)

        val lpTextElement = renderer?.lpTextElement()
        val visibleError = variableTextBeanList.isNotEmpty() &&
                (lpTextElement?.elementBean?.is1DCodeElement == true || lpTextElement?.elementBean?.is2DCodeElement == true) &&
                lpTextElement.codeBitmap == null
        _dialogViewHolder?.visible(R.id.lib_preview_tip_view, visibleError)

        if (visibleError) {
            _dialogViewHolder?.enable(R.id.dialog_positive_button, false)
        }

        if (BuildConfig.BUILD_TYPE.isDebugType()) {
            _dialogViewHolder?.click(R.id.lib_preview_view) {
                lpTextElement?.updateElementAfterEngrave()
                _dialogViewHolder?.img(R.id.lib_preview_view)
                    ?.setImageDrawable(renderDrawable)
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