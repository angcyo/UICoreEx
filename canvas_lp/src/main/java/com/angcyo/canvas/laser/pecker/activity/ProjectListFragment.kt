package com.angcyo.canvas.laser.pecker.activity

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import com.angcyo.base.removeThis
import com.angcyo.canvas.laser.pecker.activity.dslitem.ProjectListItem
import com.angcyo.canvas.laser.pecker.mode.CanvasOpenModel
import com.angcyo.core.fragment.BaseDslFragment
import com.angcyo.engrave.R
import com.angcyo.http.rx.doBack
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.laserpacker.bean.LPProjectBean
import com.angcyo.laserpacker.toCanvasProjectBean
import com.angcyo.library.ex._string
import com.angcyo.library.ex.getColor
import com.angcyo.library.ex.size
import com.angcyo.library.utils.appFolderPath
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
    }

    override fun initBaseView(savedInstanceState: Bundle?) {
        super.initBaseView(savedInstanceState)
        _recycler.resetLayoutManager("SV2")
    }

    override fun onLoadData() {
        super.onLoadData()

        doBack {
            val projectList = mutableListOf<LPProjectBean>()
            projectPathFile.listFiles()?.filter { it.name.endsWith(LPDataConstant.PROJECT_EXT) }
                ?.sortedByDescending { it.lastModified() }?.apply {
                    val startIndex = page.requestPageIndex * page.requestPageSize
                    for ((index, file) in this.withIndex()) {
                        if (index >= startIndex) {
                            val json = file.readText()
                            json.toCanvasProjectBean()?.let {
                                it._filePath = file.absolutePath
                                projectList.add(it)
                            }
                        }
                        if (projectList.size() >= page.requestPageSize) {
                            break
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
        }
    }

}