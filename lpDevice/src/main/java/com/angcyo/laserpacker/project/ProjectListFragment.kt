package com.angcyo.laserpacker.project

import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.ImageView
import androidx.core.widget.ImageViewCompat
import com.angcyo.base.removeThis
import com.angcyo.laserpacker.project.dslitem.ProjectListItem
import com.angcyo.core.appendIconItem
import com.angcyo.core.component.model.DataShareModel
import com.angcyo.core.fragment.BaseDslFragment
import com.angcyo.core.vmApp
import com.angcyo.http.rx.doBack
import com.angcyo.http.rx.doMain
import com.angcyo.item.component.initSearchAdapterFilter
import com.angcyo.laserpacker.bean.LPProjectBean
import com.angcyo.laserpacker.device.R
import com.angcyo.laserpacker.open.CanvasOpenModel
import com.angcyo.library.ex._string
import com.angcyo.library.ex.dpi
import com.angcyo.library.ex.file
import com.angcyo.library.ex.getColor
import com.angcyo.library.ex.gone
import com.angcyo.library.utils.isChildClassOf
import com.angcyo.objectbox.laser.pecker.LPBox
import com.angcyo.objectbox.laser.pecker.entity.EntitySync
import com.angcyo.objectbox.laser.pecker.entity.ProjectSyncEntity
import com.angcyo.objectbox.laser.pecker.entity.ProjectSyncEntity_
import com.angcyo.objectbox.page
import com.angcyo.widget.base.clickIt
import com.angcyo.widget.recycler.resetLayoutManager
import io.objectbox.query.OrderFlags

/**
 * 工程列表
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/12/01
 */
class ProjectListFragment : BaseDslFragment() {

    init {
        fragmentTitle = _string(R.string.project_title)
        fragmentConfig.isLightStyle = true
        fragmentConfig.showTitleLineView = true
        fragmentConfig.fragmentBackgroundDrawable =
            ColorDrawable(getColor(R.color.lib_theme_white_color))

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
        val size = 20 * dpi
        val vertical = 18 * dpi
        val horizontal = vertical / 4
        rightControl()?.appendIconItem {
            gone()
            ImageViewCompat.setImageTintList(
                this,
                ColorStateList.valueOf(fragmentConfig.titleItemIconColor)
            )
            setImageResource(R.drawable.sort_name_ascending)
            //setWidthHeight(size, size)
            setPadding(horizontal, vertical, horizontal, vertical)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            clickIt {
                changeSortName(this)
            }
        }
        rightControl()?.appendIconItem {
            gone()
            ImageViewCompat.setImageTintList(
                this,
                ColorStateList.valueOf(fragmentConfig.titleItemIconColor)
            )
            setImageResource(R.drawable.sort_time_ascending)
            //setWidthHeight(size, size)
            setPadding(horizontal, vertical, horizontal, vertical)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            clickIt {
                changeSortTime(this)
            }
        }
        initSearchAdapterFilter(_string(R.string.project_filter_tip))
    }

    override fun onLoadData() {
        super.onLoadData()

        doBack {
            val entityList = ProjectSyncEntity::class.page(page, LPBox.PACKAGE_NAME) {
                sortTime?.let {
                    order(ProjectSyncEntity_.updateTime, it)
                }
                sortName?.let {
                    order(ProjectSyncEntity_.name, it)
                }
                apply(
                    ProjectSyncEntity_.userId.equal("${EntitySync.userId}")
                        .and(ProjectSyncEntity_.isDelete.equal(false))
                )
            }

            val projectList = mutableListOf<LPProjectBean>()
            entityList.forEach { entity ->
                entity.filePath?.file()?.readProjectBean()?.let {
                    it.entityId = entity.entityId
                    it.file_name = entity.name//换一下名称
                    projectList.add(it)
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
                rightControl()?.apply {
                    goneIndex(0, _adapter.isEmpty())
                    goneIndex(1, _adapter.isEmpty())
                }
            }
        }
    }

    private var sortTime: Int? = OrderFlags.DESCENDING //1:降序, 0:升序
    private var sortName: Int? = null //1:降序, 0:升序

    /**更新右边的按钮图标*/
    fun updateRightIco() {
        rightControl()?.eachChild { index, child ->
            (child as? ImageView)?.apply {
                if (index == 0) {
                    if (sortName == OrderFlags.DESCENDING) {
                        setImageResource(R.drawable.sort_name_descending)
                    } else {
                        setImageResource(R.drawable.sort_name_ascending)
                    }
                } else if (index == 1) {
                    if (sortTime == OrderFlags.DESCENDING) {
                        setImageResource(R.drawable.sort_time_descending)
                    } else {
                        setImageResource(R.drawable.sort_time_ascending)
                    }
                }
            }
        }
    }

    /**改变时间排序规则*/
    fun changeSortTime(imageView: ImageView?) {
        sortTime = if (sortTime == OrderFlags.DESCENDING) {
            0
        } else {
            sortName = null
            OrderFlags.DESCENDING
        }
        updateRightIco()
        startRefresh() //重新刷新
    }

    /**改变名称排序规则*/
    fun changeSortName(imageView: ImageView?) {
        sortName = if (sortName == OrderFlags.DESCENDING) {
            0
        } else {
            sortTime = null
            OrderFlags.DESCENDING
        }
        updateRightIco()
        startRefresh() //重新刷新
    }

}