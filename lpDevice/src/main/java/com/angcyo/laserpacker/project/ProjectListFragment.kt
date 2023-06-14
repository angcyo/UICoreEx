package com.angcyo.laserpacker.project

import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.ImageView
import androidx.core.view.setPadding
import androidx.core.widget.ImageViewCompat
import com.angcyo.base.removeThis
import com.angcyo.core.appendIconItem
import com.angcyo.core.component.model.DataShareModel
import com.angcyo.core.fragment.BaseDslFragment
import com.angcyo.core.vmApp
import com.angcyo.http.rx.doBack
import com.angcyo.http.rx.doMain
import com.angcyo.item.component.initSearchAdapterFilter
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.laserpacker.bean.LPProjectBean
import com.angcyo.laserpacker.device.R
import com.angcyo.laserpacker.open.CanvasOpenModel
import com.angcyo.laserpacker.project.dslitem.ProjectListItem
import com.angcyo.library.ex._dimen
import com.angcyo.library.ex._string
import com.angcyo.library.ex.getColor
import com.angcyo.library.ex.gone
import com.angcyo.library.ex.size
import com.angcyo.library.utils.appFolderPath
import com.angcyo.library.utils.isChildClassOf
import com.angcyo.widget.base.clickIt
import com.angcyo.widget.recycler.resetLayoutManager
import java.io.File

/**
 * 工程列表
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/12/01
 */
class ProjectListFragment : BaseDslFragment() {

    /**项目文件夹目录*/
    val projectPathFile: File = File(appFolderPath(LPDataConstant.PROJECT_FILE_FOLDER))

    init {
        fragmentTitle = _string(R.string.project_title)
        fragmentConfig.isLightStyle = true
        fragmentConfig.showTitleLineView = true
        fragmentConfig.fragmentBackgroundDrawable = ColorDrawable(getColor(R.color.lib_white))

        page.firstPageIndex = 0
        enableRefresh = true

        titleLayoutId = R.layout.lib_search_title_layout
    }

    override fun initBaseView(savedInstanceState: Bundle?) {
        super.initBaseView(savedInstanceState)
        _recycler.resetLayoutManager("SV2")

        //监听同步更新的状态
        vmApp<DataShareModel>().shareUpdateAdapterItemOnceData.observe(this) {
            it?.let {
                if (it is Class<*> && it.isChildClassOf(ProjectListItem::class.java)) {
                    _adapter.updateAllItem()
                }
            }
        }

        //搜索过滤
        rightControl()?.appendIconItem {
            gone()
            ImageViewCompat.setImageTintList(
                this,
                ColorStateList.valueOf(fragmentConfig.titleItemIconColor)
            )
            setImageResource(R.drawable.sort_time_ascending)
            setPadding(_dimen(R.dimen.lib_xhdpi))
            clickIt {
                changeSort(this)
            }
        }
        initSearchAdapterFilter(_string(R.string.project_filter_tip))
    }

    override fun onLoadData() {
        super.onLoadData()

        doBack {
            val projectList = mutableListOf<LPProjectBean>()
            projectPathFile.listFiles()?.filter {
                it.name.endsWith(LPDataConstant.PROJECT_EXT, true) ||
                        it.name.endsWith(LPDataConstant.PROJECT_EXT2, true)
            }?.apply {
                if (sortTime == 1) {
                    sortedByDescending { it.lastModified() }
                } else {
                    sortedBy { it.lastModified() }
                }.apply {
                    val startIndex = page.requestPageIndex * page.requestPageSize
                    for ((index, file) in this.withIndex()) {
                        if (index >= startIndex) {
                            file.readProjectBean()?.let { projectList.add(it) }
                        }
                        if (projectList.size() >= page.requestPageSize) {
                            break
                        }
                    }
                }
            }
            loadDataEnd(ProjectListItem::class, projectList) { bean ->
                itemProjectBean = bean

                itemClick = {
                    itemProjectBean?.let {
                        CanvasOpenModel.open(it)
                        removeThis()
                    }
                }
            }

            doMain {
                //right ico
                rightControl()?.goneIndex(0, _adapter.itemCount == 0)
            }
        }
    }

    private var sortTime = 1 //1:降序, -1:升序

    /**改变排序规则*/
    fun changeSort(imageView: ImageView?) {
        if (sortTime == 1) {
            sortTime = -1
            imageView?.setImageResource(R.drawable.sort_time_descending)
        } else {
            sortTime = 1
            imageView?.setImageResource(R.drawable.sort_time_ascending)
        }
        startRefresh() //重新刷新
    }

}