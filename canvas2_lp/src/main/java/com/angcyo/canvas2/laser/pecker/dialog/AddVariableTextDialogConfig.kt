package com.angcyo.canvas2.laser.pecker.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.TextView
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dialog.dslitem.VarDateFormatInputItem
import com.angcyo.canvas2.laser.pecker.dialog.dslitem.VarDateFormatWheelItem
import com.angcyo.canvas2.laser.pecker.dialog.dslitem.VarDateOffsetItem
import com.angcyo.canvas2.laser.pecker.dialog.dslitem.VarDateSelectWheelItem
import com.angcyo.canvas2.laser.pecker.dialog.dslitem.VarExcelColumnChooseItem
import com.angcyo.canvas2.laser.pecker.dialog.dslitem.VarExcelSheetChooseItem
import com.angcyo.canvas2.laser.pecker.dialog.dslitem.VarFileChooseItem
import com.angcyo.canvas2.laser.pecker.dialog.dslitem.VarNumberValueItem
import com.angcyo.canvas2.laser.pecker.dialog.dslitem.VarTextFixedItem
import com.angcyo.canvas2.laser.pecker.dialog.dslitem.VarTimeFormatInputItem
import com.angcyo.canvas2.laser.pecker.dialog.dslitem.VarTimeFormatWheelItem
import com.angcyo.canvas2.laser.pecker.dialog.dslitem.VarTimeOffsetItem
import com.angcyo.canvas2.laser.pecker.dialog.dslitem.VarTimeSelectWheelItem
import com.angcyo.dialog.DslDialogConfig
import com.angcyo.dialog.configBottomDialog
import com.angcyo.dialog2.dslitem.LPLabelWheelItem
import com.angcyo.dialog2.dslitem.itemSelectedIndex
import com.angcyo.dialog2.dslitem.itemWheelBean
import com.angcyo.dialog2.dslitem.itemWheelDialogTitle
import com.angcyo.dialog2.dslitem.itemWheelList
import com.angcyo.dialog2.dslitem.itemWheelText
import com.angcyo.dialog2.dslitem.updateWheelSelectedIndex
import com.angcyo.dsladapter.DslAdapter
import com.angcyo.dsladapter.find
import com.angcyo.dsladapter.hideItemBy
import com.angcyo.dsladapter.renderAdapterEmptyStatus
import com.angcyo.item.DslIncrementItem
import com.angcyo.item.DslPropertySwitchItem
import com.angcyo.item.DslRadioGroupItem
import com.angcyo.item.DslSingleInputItem
import com.angcyo.item.DslTextPreviewItem
import com.angcyo.item.style._itemCheckedIndexList
import com.angcyo.item.style.itemCheckItems
import com.angcyo.item.style.itemCheckedItems
import com.angcyo.item.style.itemEditDigits
import com.angcyo.item.style.itemEditHint
import com.angcyo.item.style.itemEditText
import com.angcyo.item.style.itemIncrementMaxValue
import com.angcyo.item.style.itemIncrementMinValue
import com.angcyo.item.style.itemIncrementValue
import com.angcyo.item.style.itemLabel
import com.angcyo.item.style.itemLabelTextSize
import com.angcyo.item.style.itemSwitchChecked
import com.angcyo.item.style.itemText
import com.angcyo.laserpacker.bean.LPVariableBean
import com.angcyo.laserpacker.bean.NumberFormatTypeBean
import com.angcyo.library._screenHeight
import com.angcyo.library.ex._dimen
import com.angcyo.library.ex._drawable
import com.angcyo.library.ex._string
import com.angcyo.library.ex.dpi
import com.angcyo.library.ex.find
import com.angcyo.library.ex.getChildOrNull
import com.angcyo.library.ex.hideSoftInput
import com.angcyo.library.ex.paddingHorizontal
import com.angcyo.library.ex.setHeight
import com.angcyo.library.ex.setSize
import com.angcyo.library.ex.toStr
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget._rv
import com.angcyo.widget.base.resetChild
import com.angcyo.widget.recycler.renderDslAdapter
import com.angcyo.widget.span.span
import com.angcyo.widget.tab

/**
 * 变量模板界面弹窗
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/08/30
 */
class AddVariableTextDialogConfig(context: Context? = null) : DslDialogConfig(context) {

    private var _adapter: DslAdapter? = null

    private val _fileBean = LPVariableBean(LPVariableBean._TYPE_FILE).apply {
        _fileType = LPVariableBean.TYPE_TXT
    }
    private val _txtBean = LPVariableBean(LPVariableBean.TYPE_TXT)
    private val _excelBean = LPVariableBean(LPVariableBean.TYPE_EXCEL)

    private val variableBeanList = mutableListOf<LPVariableBean>().apply {
        add(LPVariableBean(LPVariableBean.TYPE_FIXED))
        add(LPVariableBean(LPVariableBean.TYPE_NUMBER).apply {
            format = LPVariableBean.DEFAULT_NUMBER_FORMAT
            min = 1
            current = min
            formatType = LPVariableBean.NUMBER_TYPE_DEC
            formatType = LPVariableBean.NUMBER_TYPE_HEX
        })
        add(LPVariableBean(LPVariableBean.TYPE_DATE).apply {
            format = LPVariableBean.DEFAULT_DATE_FORMAT
            value = 0
        })
        add(LPVariableBean(LPVariableBean.TYPE_TIME).apply {
            format = LPVariableBean.DEFAULT_TIME_FORMAT
            value = 0
        })
        add(_fileBean)
    }

    /**应用回调*/
    var onApplyVariableAction: (LPVariableBean) -> Unit = {}

    init {
        dialogLayoutId = R.layout.dialog_add_variable_text_layout
        canceledOnTouchOutside = false
    }

    private var _currentVariableBean: LPVariableBean? = null

    override fun initDialogView(dialog: Dialog, dialogViewHolder: DslViewHolder) {
        super.initDialogView(dialog, dialogViewHolder)
        dialogViewHolder.tv(R.id.dialog_title_view)?.text =
            _string(R.string.canvas_add_variable_text)
        dialogViewHolder.enable(R.id.dialog_positive_button, false)

        //确定
        dialogViewHolder.click(R.id.dialog_positive_button) {
            _currentVariableBean?.let(onApplyVariableAction)
            dialog.dismiss()
        }

        //back
        dialogViewHolder.click(R.id.dialog_negative_button) {
            dialog.dismiss()
        }

        //tab
        dialogViewHolder.tab(R.id.lib_tab_layout)?.apply {
            resetChild(variableBeanList, R.layout.lib_segment_layout) { itemView, item, itemIndex ->
                itemView.find<TextView>(R.id.lib_text_view)?.apply {
                    text = item.type.variableTypeToStr()
                    paddingHorizontal(12 * dpi)
                }
            }
            observeIndexChange { fromIndex, toIndex, reselect, fromUser ->
                if (fromUser || fromIndex == -1) {
                    updateVariableTextItemView(getChildOrNull(fromIndex), fromIndex, false)
                    updateVariableTextItemView(getChildOrNull(toIndex), toIndex, true)

                    val bean = variableBeanList.getOrNull(toIndex)
                    if (bean != null) {
                        if (bean.type == LPVariableBean._TYPE_FILE) {
                            if (bean._fileType == LPVariableBean.TYPE_EXCEL) {
                                renderVariableTextItem(_excelBean)
                            } else {
                                renderVariableTextItem(_txtBean)
                            }
                        } else {
                            renderVariableTextItem(bean)
                        }
                    }
                }
            }
        }

        //rv
        dialogViewHolder._rv(R.id.lib_recycler_view)?.apply {
            setHeight(_screenHeight * 2 / 4)
            renderDslAdapter {
                _adapter = this
                renderAdapterEmptyStatus(R.layout.variable_text_empty_layout)
            }
        }
    }

    private fun enablePositiveButton(enable: Boolean = true) {
        _dialogViewHolder?.enable(R.id.dialog_positive_button, enable)
    }

    /**渲染[bean]对应的操作界面*/
    private fun renderVariableTextItem(bean: LPVariableBean) {
        _currentVariableBean = bean
        _dialogViewHolder?.itemView?.hideSoftInput()
        _adapter?.render {
            clearAllItems()
            when (bean.type) {
                LPVariableBean.TYPE_FIXED -> {
                    VarTextFixedItem()() {
                        itemData = bean
                        observeItemChange {
                            enablePositiveButton()
                        }
                    }
                }

                LPVariableBean.TYPE_NUMBER -> renderNumberType(bean)
                LPVariableBean.TYPE_DATE -> renderDateType(bean)
                LPVariableBean.TYPE_TIME -> renderTimeType(bean)
                LPVariableBean.TYPE_TXT -> renderTxtType(bean)
                LPVariableBean.TYPE_EXCEL -> renderExcelType(bean)
            }
        }
    }

    /**序列号*/
    private fun DslAdapter.renderNumberType(bean: LPVariableBean) {
        DslTextPreviewItem()() {
            itemText = bean.variableText
        }
        DslSingleInputItem()() {
            itemLabel = _string(R.string.variable_format)
            itemEditText = bean.format
            itemEditDigits = "#,0./'`-"
            itemEditHint = LPVariableBean.DEFAULT_NUMBER_FORMAT
            itemData = itemEditDigits
            labelItemConfig.itemLabelTextStyle.apply {
                rightDrawable = null
                paddingRight = 0
                paddingTop = _dimen(R.dimen.lib_xhdpi)
                paddingBottom = paddingTop
            }
            observeItemChange {
                bean.format = itemEditText.toString()
                updateTextPreviewItem()
            }
        }
        DslIncrementItem()() {
            itemLabel = _string(R.string.variable_start_number)
            itemIncrementValue = bean.min.toString()
            observeItemChange {
                bean.min = itemIncrementValue?.toString()?.toLongOrNull() ?: 0
                bean.reset()
                updateNumberValueItem()
            }
        }
        VarNumberValueItem()() {
            itemLabel = _string(R.string.variable_current_number)
            itemIncrementMinValue = bean.min
            itemIncrementMaxValue = bean.max
            itemIncrementValue = bean.current.toString()
            observeItemChange {
                bean.current = itemIncrementValue?.toString()?.toLongOrNull() ?: 0
                bean.reset()
                updateTextPreviewItem()
            }
        }
        DslIncrementItem()() {
            itemLabel = _string(R.string.variable_max_number)
            itemIncrementValue = bean.max.toString()
            observeItemChange {
                bean.max = itemIncrementValue?.toString()?.toLongOrNull() ?: 0
                bean.reset()
                updateNumberValueItem()
            }
        }
        DslIncrementItem()() {
            itemLabel = _string(R.string.variable_number_step)
            itemIncrementValue = bean.value.toString()
            observeItemChange {
                bean.value = itemIncrementValue?.toString()?.toLongOrNull() ?: 0
                bean.reset()
            }
        }
        DslIncrementItem()() {
            itemLabel = _string(R.string.variable_number_step_threshold)
            itemIncrementValue = bean.step.toString()
            observeItemChange {
                bean.step = itemIncrementValue?.toString()?.toLongOrNull() ?: 0
                bean.reset()
            }
        }
        LPLabelWheelItem()() {
            itemLabel = _string(R.string.variable_model)
            val list = listOf(
                NumberFormatTypeBean(
                    LPVariableBean.NUMBER_TYPE_DEC,
                    _string(R.string.variable_model_dec)
                ),
                NumberFormatTypeBean(
                    LPVariableBean.NUMBER_TYPE_HEX,
                    _string(R.string.variable_model_hex_upper)
                ),
                NumberFormatTypeBean(
                    LPVariableBean.NUMBER_TYPE_HEX_LOWER,
                    _string(R.string.variable_model_hex_lower)
                )
            )
            itemWheelList = list
            itemSelectedIndex = list.indexOfFirst { it.formatType == bean.formatType }
            observeItemChange {
                bean.formatType = itemWheelBean<NumberFormatTypeBean>()?.formatType
                    ?: LPVariableBean.NUMBER_TYPE_DEC
                updateTextPreviewItem()
            }
        }
        DslPropertySwitchItem()() {
            itemLabel = _string(R.string.variable_reset)
            itemSwitchChecked = bean.reset
            itemLabelTextSize = _dimen(R.dimen.text_body_size).toFloat()
            observeItemChange {
                bean.reset = itemSwitchChecked
            }
        }
        enablePositiveButton()
    }

    /**日期*/
    private fun DslAdapter.renderDateType(bean: LPVariableBean) {

        DslTextPreviewItem()() {
            itemText = bean.variableText
        }

        DslRadioGroupItem()() {
            itemLabel = _string(R.string.variable_date_format)
            itemCheckItems = listOf(
                _string(R.string.variable_date_format_system),
                _string(R.string.variable_date_format_custom)
            )
            itemCheckedItems =
                if (bean._systemDateFormat) mutableListOf(_string(R.string.variable_date_format_system))
                else mutableListOf(_string(R.string.variable_date_format_custom))
            observeItemChange {
                val index = _itemCheckedIndexList.firstOrNull() ?: 0
                bean._systemDateFormat = index == 0
                find<VarDateFormatInputItem>()?.apply {
                    itemEditText = bean.format
                    itemEditHint = bean.format
                }
                hideItemBy(index != 0) { it is VarDateFormatWheelItem }
                hideItemBy(index != 1) { it is VarDateFormatInputItem }
                updateTextPreviewItem()
            }
        }
        VarDateFormatWheelItem()() {
            itemWheelDialogTitle = _string(R.string.variable_date_format_system)
            itemHidden = !bean._systemDateFormat
            updateWheelSelectedIndex(bean.format)
            observeItemChange {
                bean.format = itemWheelText()?.toStr()
                updateTextPreviewItem()
            }
        }
        VarDateFormatInputItem()() {
            itemEditHint = bean.format
            itemEditText = bean.format
            itemHidden = bean._systemDateFormat
            observeItemChange {
                bean.format = itemEditText?.toStr()
                updateTextPreviewItem()
            }
        }

        DslRadioGroupItem()() {
            itemLabel = _string(R.string.variable_date_type)
            itemCheckItems = listOf(
                _string(R.string.variable_date_type_system),
                _string(R.string.variable_date_type_custom)
            )
            itemCheckedItems =
                if (bean.auto) mutableListOf(_string(R.string.variable_date_type_system))
                else mutableListOf(_string(R.string.variable_date_type_custom))
            observeItemChange {
                val index = _itemCheckedIndexList.firstOrNull() ?: 0
                bean.auto = index == 0
                hideItemBy(index != 1) { it is VarDateSelectWheelItem }
                updateTextPreviewItem()
            }
        }

        VarDateSelectWheelItem()() {
            itemHidden = bean.auto
            itemText = bean.content
            observeItemChange {
                bean.content = itemDateSelectedTime.toStr()
                updateTextPreviewItem()
            }
        }

        VarDateOffsetItem()() {
            itemVariableBean = bean
            observeItemChange {
                updateTextPreviewItem()
            }
        }
        enablePositiveButton()
    }

    /**时间*/
    private fun DslAdapter.renderTimeType(bean: LPVariableBean) {

        DslTextPreviewItem()() {
            itemText = bean.variableText
        }

        DslRadioGroupItem()() {
            itemLabel = _string(R.string.variable_time_format)
            itemCheckItems = listOf(
                _string(R.string.variable_time_format_system),
                _string(R.string.variable_time_format_custom)
            )
            itemCheckedItems =
                if (bean._systemTimeFormat) mutableListOf(_string(R.string.variable_time_format_system))
                else mutableListOf(_string(R.string.variable_time_format_custom))
            observeItemChange {
                val index = _itemCheckedIndexList.firstOrNull() ?: 0
                bean._systemTimeFormat = index == 0
                find<VarTimeFormatInputItem>()?.apply {
                    itemEditText = bean.format
                    itemEditHint = bean.format
                }
                hideItemBy(index != 0) { it is VarTimeFormatWheelItem }
                hideItemBy(index != 1) { it is VarTimeFormatInputItem }
                updateTextPreviewItem()
            }
        }
        VarTimeFormatWheelItem()() {
            itemWheelDialogTitle = _string(R.string.variable_time_format_system)
            itemHidden = !bean._systemTimeFormat
            updateWheelSelectedIndex(bean.format)
            observeItemChange {
                bean.format = itemWheelText()?.toStr()
                updateTextPreviewItem()
            }
        }
        VarTimeFormatInputItem()() {
            itemEditHint = bean.format
            itemEditText = bean.format
            itemHidden = bean._systemTimeFormat
            observeItemChange {
                bean.format = itemEditText?.toStr()
                updateTextPreviewItem()
            }
        }

        DslRadioGroupItem()() {
            itemLabel = _string(R.string.variable_time_type)
            itemCheckItems = listOf(
                _string(R.string.variable_time_type_system),
                _string(R.string.variable_time_type_custom)
            )
            itemCheckedItems =
                if (bean.auto) mutableListOf(_string(R.string.variable_time_type_system))
                else mutableListOf(_string(R.string.variable_time_type_custom))
            observeItemChange {
                val index = _itemCheckedIndexList.firstOrNull() ?: 0
                bean.auto = index == 0
                hideItemBy(index != 1) { it is VarTimeSelectWheelItem }
                updateTextPreviewItem()
            }
        }

        VarTimeSelectWheelItem()() {
            itemHidden = bean.auto
            itemText = bean.content
            observeItemChange {
                bean.content = itemDateSelectedTime.toStr()
                updateTextPreviewItem()
            }
        }

        VarTimeOffsetItem()() {
            itemVariableBean = bean
            observeItemChange {
                updateTextPreviewItem()
            }
        }
        enablePositiveButton()
    }

    /**文本文件*/
    private fun DslAdapter.renderTxtType(bean: LPVariableBean) {
        DslTextPreviewItem()() {
            itemText = bean.variableText
        }
        DslRadioGroupItem()() {
            itemLabel = _string(R.string.variable_file_type)
            itemCheckItems = listOf(
                _string(R.string.variable_file_txt),
                _string(R.string.variable_file_excel)
            )
            itemCheckedItems = mutableListOf(_string(R.string.variable_file_txt))
            observeItemChange {
                val index = _itemCheckedIndexList.firstOrNull() ?: 0
                if (index == 0) {
                    _fileBean._fileType = LPVariableBean.TYPE_TXT
                } else {
                    _fileBean._fileType = LPVariableBean.TYPE_EXCEL
                    renderVariableTextItem(_excelBean)
                }
            }
        }
        VarFileChooseItem()() {
            itemData = bean
        }
        DslIncrementItem()() {
            itemLabel = _string(R.string.variable_file_line)
            itemIncrementValue = bean.current.toString()
            observeItemChange {
                bean.current = itemIncrementValue?.toString()?.toLongOrNull() ?: 0
                bean.reset()
                updateTextPreviewItem()
            }
        }
        DslIncrementItem()() {
            itemLabel = _string(R.string.variable_file_increment)
            itemIncrementValue = bean.stepVal.toString()
            observeItemChange {
                bean.stepVal = itemIncrementValue?.toString()?.toLongOrNull() ?: 0
                bean.reset()
                updateTextPreviewItem()
            }
        }
        DslPropertySwitchItem()() {
            itemLabel = _string(R.string.variable_reset)
            itemSwitchChecked = bean.reset
            itemLabelTextSize = _dimen(R.dimen.text_body_size).toFloat()
            observeItemChange {
                bean.reset = itemSwitchChecked
            }
        }
    }

    /**xls Excel文件*/
    private fun DslAdapter.renderExcelType(bean: LPVariableBean) {
        DslTextPreviewItem()() {
            itemText = bean.variableText
        }
        DslRadioGroupItem()() {
            itemLabel = _string(R.string.variable_file_type)
            itemCheckItems = listOf(
                _string(R.string.variable_file_txt),
                _string(R.string.variable_file_excel)
            )
            itemCheckedItems = mutableListOf(_string(R.string.variable_file_excel))
            observeItemChange {
                val index = _itemCheckedIndexList.firstOrNull() ?: 0
                if (index == 0) {
                    _fileBean._fileType = LPVariableBean.TYPE_TXT
                    renderVariableTextItem(_txtBean)
                } else {
                    _fileBean._fileType = LPVariableBean.TYPE_EXCEL
                }
            }
        }
        VarFileChooseItem()() {
            itemData = bean
        }
        VarExcelSheetChooseItem()() {
            itemData = bean
        }
        VarExcelColumnChooseItem()() {
            itemData = bean
        }
        DslIncrementItem()() {
            itemLabel = _string(R.string.variable_file_line)
            itemIncrementValue = bean.current.toString()
            observeItemChange {
                bean.current = itemIncrementValue?.toString()?.toLongOrNull() ?: 0
                bean.reset()
                updateTextPreviewItem()
            }
        }
        DslIncrementItem()() {
            itemLabel = _string(R.string.variable_file_increment)
            itemIncrementValue = bean.stepVal.toString()
            observeItemChange {
                bean.stepVal = itemIncrementValue?.toString()?.toLongOrNull() ?: 0
                bean.reset()
                updateTextPreviewItem()
            }
        }
        DslPropertySwitchItem()() {
            itemLabel = _string(R.string.variable_reset)
            itemSwitchChecked = bean.reset
            itemLabelTextSize = _dimen(R.dimen.text_body_size).toFloat()
            observeItemChange {
                bean.reset = itemSwitchChecked
            }
        }
    }

    /**更新需要预览的item*/
    private fun updateTextPreviewItem() {
        _currentVariableBean?.let { bean ->
            _adapter?.find<DslTextPreviewItem>()?.itemText = bean.variableText
        }
    }

    /**更新限制的最小最大值*/
    private fun updateNumberValueItem() {
        _currentVariableBean?.let { bean ->
            _adapter?.find<VarNumberValueItem>()?.apply {
                itemIncrementMinValue = bean.min
                itemIncrementMaxValue = bean.max
                itemIncrementValue = bean.current.toString()
                updateAdapterItem()
            }
        }
    }

    private fun updateVariableTextItemView(view: View?, index: Int, selected: Boolean) {
        view ?: return
        val bean = variableBeanList.getOrNull(index) ?: return
        view.find<TextView>(R.id.lib_text_view)?.text = span {
            if (selected) {
                appendDrawable(bean.type.variableTypeToIco()?.setSize(18 * dpi))
                appendSpace(2 * dpi)
            }
            append(bean.type.variableTypeToStr())
        }
    }
}

/** 底部弹出涂鸦对话框 */
fun Context.addVariableTextDialog(config: AddVariableTextDialogConfig.() -> Unit): Dialog {
    return AddVariableTextDialogConfig().run {
        configBottomDialog(this@addVariableTextDialog)
        config()
        show()
    }
}

/**变量文本类型转字符串*/
fun String.variableTypeToStr(): String = when (this) {
    LPVariableBean.TYPE_FIXED -> _string(R.string.variable_fixed_text)
    LPVariableBean.TYPE_NUMBER -> _string(R.string.variable_serial_number)
    LPVariableBean.TYPE_DATE -> _string(R.string.variable_date)
    LPVariableBean.TYPE_TIME -> _string(R.string.variable_time)
    LPVariableBean._TYPE_FILE, LPVariableBean.TYPE_TXT, LPVariableBean.TYPE_EXCEL -> _string(R.string.variable_file)
    else -> this
}

/**变量文本类型转ico*/
fun String.variableTypeToIco(): Drawable? = when (this) {
    LPVariableBean.TYPE_FIXED -> _drawable(R.drawable.variable_fixed_text_svg)
    LPVariableBean.TYPE_NUMBER -> _drawable(R.drawable.variable_serial_number_svg)
    LPVariableBean.TYPE_DATE -> _drawable(R.drawable.variable_date_svg)
    LPVariableBean.TYPE_TIME -> _drawable(R.drawable.variable_time_svg)
    LPVariableBean._TYPE_FILE, LPVariableBean.TYPE_TXT, LPVariableBean.TYPE_EXCEL -> _drawable(R.drawable.variable_file_svg)
    else -> null
}