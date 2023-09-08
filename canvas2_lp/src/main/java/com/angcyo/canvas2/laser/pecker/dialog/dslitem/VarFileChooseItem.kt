package com.angcyo.canvas2.laser.pecker.dialog.dslitem

import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.core.component.manage.InnerFileManageModel
import com.angcyo.core.component.manage.innerFileSelectFragment
import com.angcyo.dialog2.dslitem.LPLabelWheelItem
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.find
import com.angcyo.item.style.itemLabel
import com.angcyo.laserpacker.bean.LPVariableBean
import com.angcyo.library.annotation.CallPoint
import com.angcyo.library.ex._color
import com.angcyo.library.ex._string
import com.angcyo.library.ex.file
import com.angcyo.library.ex.isFileExists
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.base._textColor
import java.io.File

/**
 * 文件选择item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/09/07
 */
class VarFileChooseItem : LPLabelWheelItem() {

    /**需要选择的文件类型*/
    var itemFileType: String = InnerFileManageModel.EXT_TXT

    init {
        itemLabel = _string(R.string.variable_file_label)
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        val fileName = _itemVariableBean?.fileName
        itemHolder.tv(R.id.lib_text_view)?.apply {
            text =
                if (fileName.isNullOrEmpty()) _string(R.string.ui_choose) else fileName

            val fileUri = _itemVariableBean?.fileUri
            _textColor = if (fileUri.isNullOrBlank() || fileUri.file().isFileExists()) {
                _color(R.color.text_general_color)
            } else {
                _color(R.color.error)
            }
        }

        itemHolder.click(R.id.lib_content_wrap_layout) {
            //选择文件
            it.context.innerFileSelectFragment(1, listOf(itemFileType))
        }
    }

    /**更新选中文件后的操作*/
    @CallPoint
    fun updateChooseFile(file: File?) {
        val bean = _itemVariableBean
        _itemVariableBean?.apply {
            fileName = file?.name
            fileUri = file?.absolutePath

            initFileCache()

            if (type == LPVariableBean.TYPE_EXCEL) {
                //init
                itemDslAdapter?.apply {
                    find<VarExcelSheetChooseItem>()?.updateFileChoose(bean!!)
                    find<VarExcelColumnChooseItem>()?.updateFileChoose(bean!!)
                    updateVarFileItem(bean)
                }
            }
        }
        updateAdapterItem()
    }

}