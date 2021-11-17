package com.angcyo.amap3d.fragment

import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.view.postDelayed
import androidx.fragment.app.Fragment
import com.amap.api.maps.AMap
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.animation.Animation
import com.amap.api.services.core.AMapException
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.core.PoiItem
import com.amap.api.services.geocoder.GeocodeResult
import com.amap.api.services.geocoder.GeocodeSearch
import com.amap.api.services.geocoder.RegeocodeQuery
import com.amap.api.services.geocoder.RegeocodeResult
import com.amap.api.services.help.Inputtips
import com.amap.api.services.help.InputtipsQuery
import com.amap.api.services.help.Tip
import com.amap.api.services.poisearch.PoiResult
import com.amap.api.services.poisearch.PoiSearch
import com.angcyo.DslFHelper
import com.angcyo.amap3d.*
import com.angcyo.amap3d.core.MapLocation
import com.angcyo.amap3d.core.RTextureMapView
import com.angcyo.amap3d.core.latLng
import com.angcyo.amap3d.core.toLatLng
import com.angcyo.amap3d.dslitem.DslSelectPoiItem
import com.angcyo.base.back
import com.angcyo.base.dslFHelper
import com.angcyo.base.onFragmentResult
import com.angcyo.base.setFragmentResult
import com.angcyo.core.fragment.BaseDslFragment
import com.angcyo.dsladapter.*
import com.angcyo.dsladapter.data.loadDataEnd
import com.angcyo.dsladapter.data.loadDataEndIndex
import com.angcyo.library.L
import com.angcyo.library.ex._color
import com.angcyo.library.ex.dpi
import com.angcyo.library.ex.isResultOk
import com.angcyo.library.model.singlePage
import com.angcyo.widget.base.*
import com.angcyo.widget.recycler.initDslAdapter

/**
 * 高德地图, 选位置界面
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/06/13
 * Copyright (c) 2020 angcyo. All rights reserved.
 */
class AMapSelectorFragment : BaseDslFragment() {

    //输入提示结果展示的DslAdapter
    lateinit var tipsDslAdapter: DslAdapter

    //地图中间显示位置指针的marker
    var centerMarker: Marker? = null

    val mapView: RTextureMapView? get() = _vh.v<RTextureMapView>(R.id.lib_map_view)

    val map: AMap? get() = mapView?.map

    init {
        fragmentTitle = "选择位置"
        fragmentLayoutId = R.layout.map_selector_fragment
        page.firstPageIndex = 0
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapView?.onDestroyR()
    }

    override fun initBaseView(savedInstanceState: Bundle?) {
        super.initBaseView(savedInstanceState)

        _vh.initMapView(this, savedInstanceState) {
            map?.apply {
                //地图事件监听
                onMapLoadedListener {
                    centerMarker = addScreenCenterMarker {
                        icon(markerIcon(R.drawable.map_location_point))
                    }
                }

                onCameraChangeListener {
                    if (_selectedMapLocation != null) {
                        if (_selectedMapLocation!!.latLng().distance(it.target) > 1f) {
                            startSearch(it.target)
                        }
                    } else {
                        startSearch(it.target)
                    }
                }
            }
        }

        //ui初始化
        _vh.rv(R.id.tips_recycler_view)?.initDslAdapter {
            tipsDslAdapter = this
        }

        _vh.enable(R.id.lib_send_view, false)

        _vh.ev(R.id.lib_edit_view)?.apply {
            onTextChange(shakeDelay = 300) {
                searchTips(it.toString())
            }
            onImeAction {
                //重新获取焦点
                postDelayed(60L) {
                    _vh.focus<View>(R.id.lib_edit_view)
                }
            }
        }

        _vh.click(R.id.lib_search_view) {
            _showToSearch()
        }

        _vh.click(R.id.lib_cancel_view) {
            _showToNormal()
        }

        _vh.click(R.id.lib_send_view) {
            _resultMapLocation = _selectedMapLocation
            back()
        }

        //default
        _showToNormal()

        //单选模式
        _adapter.singleModel()
        _adapter.toLoading()
        _adapter.itemSelectorHelper.observer {
            onItemChange = { selectorItems, _, _, _ ->
                selectorItems.firstOrNull()?.apply {
                    if (this is DslSelectPoiItem) {
                        _selectedMapLocation = itemMapLocation
                    }
                }
            }
        }

        //状态监听
        _adapter.onRefreshOrLoadMore { _, loadMore ->
            if (loadMore) {
                onLoadMore()
            } else {
                onRefresh(null)
            }
        }
    }

    fun _showToNormal() {
        _vh.ev(R.id.lib_edit_view)?.apply {
            hideSoftInput()
            setInputText()
        }

        _vh.view(R.id.search_wrap_layout)?.apply {
            doAnimate {
                animate().translationY(-mH().toFloat()).setDuration(300)
                    .withEndAction { _vh.gone(R.id.search_wrap_layout) }.start()
            }
        }
        _vh.gone(R.id.lib_content_overlay_wrap_layout)
    }

    fun _showToSearch() {
        _vh.visible(R.id.search_wrap_layout)
        _vh.visible(R.id.lib_content_overlay_wrap_layout)

        _vh.view(R.id.search_wrap_layout)?.apply {
            doAnimate {
                translationY = -mH().toFloat()
                animate().translationY(0f).setDuration(300).start()
            }
        }

        _vh.view(R.id.lib_edit_view)?.apply {
            postDelayed(60L) {
                showSoftInput()
            }
        }
    }

    override fun onBackPressed(): Boolean {
        if (_vh.view(R.id.search_wrap_layout).isVisible()) {
            _showToNormal()
            return false
        }
        return super.onBackPressed()
    }

    override fun onFragmentSetResult() {
        setFragmentResult(_resultMapLocation)
    }

    //<editor-fold desc="POI搜索">

    /**POI搜索*/
    val poiSearch: PoiSearch by lazy {
        PoiSearch(
            fContext(),
            null
        ).apply {
            setOnPoiSearchListener(object : PoiSearch.OnPoiSearchListener {

                //poi id搜索的结果回调, 搜索指定poi的id时, 返回的回调
                override fun onPoiItemSearched(poiItem: PoiItem?, errorCode: Int) {
                    L.d(errorCode, " ", poiItem)
                }

                //返回POI搜索异步处理的结果。
                override fun onPoiSearched(pageResult: PoiResult?, errorCode: Int) {
                    centerMarker?.cancelAnim()

                    if (pageResult != null && _lastPoiQuery == pageResult.query) {
                        if (errorCode.isSearchSuccess()) {
                            renderPoiResult(pageResult.pois)
                        } else {
                            _adapter.toError()
                        }
                    } else {
                        _adapter.toError()
                    }
                }
            })
        }
    }

    var _lastLatLng: LatLng? = null

    var _lastRegeocodeQuery: RegeocodeQuery? = null

    //居中点的位置信息
    var _centerMapLocation: MapLocation? = null

    //选中的位置信息
    var _selectedMapLocation: MapLocation? = null
        set(value) {
            field = value
            _vh.enable(R.id.lib_send_view, value != null)
            value?.apply {
                map?.moveTo(toLatLng())
                //map?.moveInclude(toLatLng(), map!!.myLocation.toLatLng())
            }
        }

    //要返回的数据
    var _resultMapLocation: MapLocation? = null

    val geocodeSearch: GeocodeSearch by lazy {
        GeocodeSearch(fContext()).apply {
            setOnGeocodeSearchListener(object : GeocodeSearch.OnGeocodeSearchListener {

                /**经纬度 转 中文地址回调*/
                override fun onRegeocodeSearched(
                    regeocodeResult: RegeocodeResult?,
                    resultCode: Int
                ) {
                    //根据给定的经纬度和最大结果数返回逆地理编码的结果列表。
                    if (resultCode == AMapException.CODE_AMAP_SUCCESS && regeocodeResult?.regeocodeQuery == _lastRegeocodeQuery) {
                        //返回成功
                        if (regeocodeResult?.regeocodeAddress?.formatAddress != null) {
                            //有效地址

                            _centerMapLocation =
                                MapLocation.from(regeocodeResult.regeocodeAddress, _lastLatLng)

                            if (_adapter.isAdapterStatusLoading()) {
                                onRefresh(null)
                            } else {
                                _adapter.toLoading()
                            }
                        } else {
                            centerMarker?.cancelAnim()
                            _adapter.toError()
                        }
                    } else {
                        //返回失败
                        centerMarker?.cancelAnim()
                        _adapter.toError()
                    }
                }

                /**中文地址 转 经纬度回调*/
                override fun onGeocodeSearched(geocodeResult: GeocodeResult?, resultCode: Int) {
                    //no op
                }

            })
        }
    }

    /**开始搜索[latLng]附近的poi信息*/
    fun startSearch(latLng: LatLng) {
        if (_lastLatLng != null && _lastLatLng!!.distance(latLng).apply { L.w(this) } <= 20f) {
            //20米内的移动不处理请求
            return
        }

        centerMarker?.jump(map!!, 50) {
            repeatCount = Animation.INFINITE
            repeatMode = Animation.REVERSE
            setInterpolator(LinearInterpolator())
            setDuration(500)
        }

        _lastLatLng = latLng

        //需要先将当前指针指向的位置逆地址编码, 再请求周边的POI信息

        val query = RegeocodeQuery(latLng.toLatLonPoint(), 1000f, GeocodeSearch.AMAP)
        _lastRegeocodeQuery = query

        // 设置异步逆地理编码请求
        geocodeSearch.getFromLocationAsyn(query)
    }

    override fun onLoadData() {
        super.onLoadData()
        _lastLatLng?.apply {
            searchPoiByBound(toLatLonPoint())
        }
    }

    var _lastPoiQuery: PoiSearch.Query? = null

    /**搜索附近的POI信息*/
    fun searchPoiByBound(latLonPoint: LatLonPoint) {
        // 第一个参数表示搜索字符串，第二个参数表示poi搜索类型，第三个参数表示poi搜索区域（空字符串代表全国）
        val query = PoiSearch.Query("", null)
        // 设置每页最多返回多少条poiitem
        query.pageSize = page.requestPageSize
        // 设置查第一页
        query.pageNum = page.requestPageIndex

        // 设置搜索区域为以lp点为圆心，其周围5000米范围
        poiSearch.bound =
            PoiSearch.SearchBound(latLonPoint /*该范围的中心点。*/, 5000 /*半径，单位：米。*/, true/*是否按照距离排序。*/)

        _lastPoiQuery = query
        poiSearch.query = query

        poiSearch.searchPOIAsyn() // 异步搜索
    }

    fun renderPoiResult(list: List<PoiItem>?) {
        val locationList = mutableListOf<MapLocation>()

        if (page.isFirstPage()) {
            _centerMapLocation?.apply {
                poiName = "[位置]"
                locationList.add(this)
            }
        }

        list?.forEach {
            locationList.add(MapLocation.from(it))
        }

        _adapter.loadDataEndIndex(
            DslSelectPoiItem::class.java,
            locationList,
            null,
            page
        ) { bean, _ ->
            itemMapLocation = bean
            itemBottomInsert = 1 * dpi
            itemLeftOffset = 10 * dpi
            itemRightOffset = itemLeftOffset
            itemDecorationColor = _color(R.color.lib_line_dark)
            itemIsSelected = false

            itemClick = {
                _adapter.itemSelectorHelper.selector(this)
            }
        }

        if (page.isFirstPage()) {
            _adapter.onDispatchUpdatesOnce {
                _adapter.itemSelectorHelper.selector(0)
            }
        }
    }

    //</editor-fold desc="POI搜索">

    //<editor-fold desc="输入提示搜索">

    val inputTipsSearch: Inputtips by lazy {
        Inputtips(fContext(), InputtipsQuery("", null)).apply {
            //输入提示回调的方法。
            setInputtipsListener { mutableList, resultID ->
                if (resultID.isSearchSuccess()) {
                    if (mutableList.isNullOrEmpty()) {
                        tipsDslAdapter.toEmpty()
                    } else {
                        renderTipsResult(mutableList)
                    }
                } else {
                    tipsDslAdapter.toError()
                }
            }
        }
    }

    fun searchTips(keyword: String?, city: String? = null) {
        if (keyword.isNullOrEmpty()) {
            _vh.gone(R.id.tips_recycler_view)
            return
        }
        _vh.visible(R.id.tips_recycler_view)
        tipsDslAdapter.toLoading()

        val query = InputtipsQuery(keyword, city)

        inputTipsSearch.query = query
        inputTipsSearch.requestInputtipsAsyn()
    }

    fun renderTipsResult(list: List<Tip>?) {
        tipsDslAdapter.loadDataEnd(DslSelectPoiItem::class.java, list, null, singlePage()) { bean ->
            itemMapLocation = MapLocation.from(bean)
            itemBottomInsert = 1 * dpi
            itemDecorationColor = _color(R.color.lib_line_dark)

            itemClick = {
                _showToNormal()
                bean.point?.toLatLon()?.apply {
                    map?.moveTo(this)
                }
            }
        }
    }

    //</editor-fold desc="输入提示搜索">

}

/**选择地图定位点*/
fun Fragment.aMapSelector(result: (location: MapLocation?) -> Unit) {
    dslFHelper {
        aMapSelector(result)
    }
}

/**快速启动高德地图选位置界面, 并获取返回值*/
fun DslFHelper.aMapSelector(result: (location: MapLocation?) -> Unit) {
    show(AMapSelectorFragment::class.java) {
        onFragmentResult { resultCode, data ->
            if (resultCode.isResultOk() && data is MapLocation) {
                result(data)
            } else {
                result(null)
            }
        }
    }
}