package com.angcyo.engrave

import com.angcyo.dsladapter.renderEmptyItem
import com.angcyo.engrave.data.EngraveReadyDataInfo
import com.angcyo.engrave.dslitem.EngraveDataNextItem
import com.angcyo.engrave.dslitem.EngraveDataPreviewItem
import com.angcyo.fragment.AbsLifecycleFragment
import com.angcyo.item.form.checkItemThrowable
import com.angcyo.library.ex.ClickAction
import com.angcyo.library.ex._dimen
import com.angcyo.library.ex._string

/**
 * 雕刻之前的处理界面, 比如来自历史文档操作
 *
 * 显示预览的效果, 并且可以选择预览还是下一步雕刻
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/07/06
 */
class EngraveBeforeLayoutHelper(val fragment: AbsLifecycleFragment) : BaseEngraveLayoutHelper() {

    /**目标数据*/
    var engraveReadyDataInfo: EngraveReadyDataInfo? = null

    /**预览*/
    var onPreviewAction: ClickAction? = null

    /**下一步*/
    var onNextAction: ClickAction? = null

    init {
        iViewTitle = _string(R.string.print_v2_package_size_setting)
    }

    override fun onIViewCreate() {
        super.onIViewCreate()
        renderDslAdapter {
            engraveReadyDataInfo?.let { dataInfo ->
                //效果预览
                EngraveDataPreviewItem()() {
                    itemEngraveReadyDataInfo = dataInfo
                }
                //文件名
                /*EngraveDataNameItem()() {
                    itemEngraveReadyDataInfo = dataInfo
                }*/
                //control
                EngraveDataNextItem()() {
                    itemPreviewAction = {
                        onPreviewAction?.invoke(it)
                    }
                    itemClick = {
                        if (!checkItemThrowable()) {
                            //next, update name
                            onNextAction?.invoke(it)
                        }
                    }
                }
                renderEmptyItem(_dimen(R.dimen.lib_xxhdpi))
            }
        }
    }
}