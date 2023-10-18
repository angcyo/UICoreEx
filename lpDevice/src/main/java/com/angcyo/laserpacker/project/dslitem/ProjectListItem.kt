package com.angcyo.laserpacker.project.dslitem

import android.content.Context
import android.graphics.Bitmap
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.dialog.inputDialog
import com.angcyo.dialog.itemsDialog
import com.angcyo.dialog.messageDialog
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.glide.glide
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.laserpacker.bean.LPProjectBean
import com.angcyo.laserpacker.device.R
import com.angcyo.library.component.lastContext
import com.angcyo.library.ex.*
import com.angcyo.library.extend.IFilterItem
import com.angcyo.library.libCacheFile
import com.angcyo.library.utils.writeToFile
import com.angcyo.objectbox.laser.pecker.entity.EntitySync
import com.angcyo.widget.DslViewHolder

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/12/01
 */
class ProjectListItem : DslAdapterItem(), IFilterItem {

    companion object {

        /**获取工程状态同步资源*/
        var getProjectListSyncStateRes: (item: ProjectListItem) -> Int? = { null }

        /**默认工程文件分享*/
        var projectFileShareAction: (bean: LPProjectBean, projectName: CharSequence?) -> Unit =
            { bean, projectName ->
                bean._filePath?.file()?.apply {
                    val cacheFile = libCacheFile(
                        (projectName?.toStr()
                            ?.run { "$this${LPDataConstant.PROJECT_EXT2}" }
                            ?: name).encode())
                    writeToFile(cacheFile, false, false)
                    cacheFile.shareFile()
                }
            }

        /**工程分享功能实现*/
        var onShareProjectAction: (bean: LPProjectBean, projectName: CharSequence?) -> Unit =
            { bean, projectName ->
                projectFileShareAction(bean, projectName)
            }

        /**输入工程名的对话框*/
        fun Context.inputProjectNameDialog(
            fileName: CharSequence?,
            action: (CharSequence) -> Unit
        ) {
            inputDialog {
                dialogTitle = _string(R.string.save_project_title)
                hintInputString = _string(R.string.project_title_limit)
                maxInputLength = 20
                canInputEmpty = false
                defaultInputString = fileName ?: "Untitled-${HawkEngraveKeys.lastProjectCount + 1}"
                onInputResult = { dialog, inputText ->
                    action(inputText)
                    false
                }
            }
        }
    }

    /**[itemProjectFile]对应的数据结构*/
    var itemProjectBean: LPProjectBean? = null
        set(value) {
            field = value
            _bitmap = value?._previewImgBitmap ?: value?.preview_img?.toBitmapOfBase64()
        }

    private val _projectName: CharSequence?
        get() = (itemProjectBean?.file_name
            ?: itemProjectBean?._filePath?.file()?.name)?.noExtName()

    private var _bitmap: Bitmap? = null

    init {
        itemLayoutId = R.layout.item_project_list_layout

        itemLongClick = {
            val item = this
            it.context.itemsDialog {
                addDialogItem {
                    itemText = _string(R.string.canvas_delete_project)
                    itemClick = {
                        lastContext.messageDialog {
                            dialogMessage = _string(R.string.canvas_delete_project_tip)
                            needPositiveButton { dialog, dialogViewHolder ->
                                dialog.dismiss()
                                itemProjectBean?._filePath?.file()?.delete()
                                item.removeAdapterItemJust()
                                EntitySync.deleteProjectSyncEntity(itemProjectBean?.entityId)
                            }
                        }
                    }
                }
                addDialogItem {
                    itemText = _string(R.string.canvas_rename)
                    itemClick = {
                        lastContext.inputProjectNameDialog(_projectName) {
                            val newName = "$it"
                            itemProjectBean?.file_name = newName
                            EntitySync.updateProjectSyncEntity(itemProjectBean?.entityId) {
                                name = newName
                            }
                            item.updateAdapterItem()
                        }
                    }
                }
                addDialogItem {
                    itemText = _string(R.string.external_sharing)
                    itemClick = {
                        itemProjectBean?.let {
                            projectFileShareAction(it, _projectName)
                        }
                    }
                }
            }
            true
        }
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        itemHolder.tv(R.id.lib_text_view)?.text = _projectName
        itemHolder.img(R.id.lib_image_view)?.glide {
            load(_bitmap)
        }
        itemHolder.click(R.id.lib_more_view) {
            itemLongClick?.invoke(it)
        }
        itemHolder.click(R.id.lib_share_view) {
            itemProjectBean?.let {
                onShareProjectAction(it, _projectName)
            }
        }

        //同步状态
        getProjectListSyncStateRes(this).let {
            itemHolder.visible(R.id.lib_sync_view, it != null)
            itemHolder.img(R.id.lib_sync_view)?.setImageResource(it ?: 0)
        }
    }

    override fun containsFilterText(text: CharSequence): Boolean =
        _projectName?.contains(text, true) == true
}