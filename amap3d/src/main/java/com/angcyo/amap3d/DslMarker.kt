package com.angcyo.amap3d

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import com.amap.api.maps.AMap
import com.amap.api.maps.model.*
import com.amap.api.maps.model.animation.AlphaAnimation
import com.amap.api.maps.model.animation.Animation
import com.amap.api.maps.model.animation.ScaleAnimation
import com.amap.api.maps.model.animation.TranslateAnimation
import com.angcyo.amap3d.DslAMap.Companion.DEFAULT_LOCATION_ZOOM
import com.angcyo.amap3d.DslMarker.Companion.DEFAULT_MARKER_ANIM_DURATION
import com.angcyo.amap3d.DslMarker.Companion.DEFAULT_MARKER_ANIM_INTERPOLATOR
import com.angcyo.amap3d.core.toLatLng
import com.angcyo.library.L
import com.angcyo.library.ex.dpi
import com.angcyo.library.ex.isListEmpty
import com.angcyo.widget.base.find
import kotlin.math.sqrt

/**
 * 管理一组[Marker]
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/06/12
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */

class DslMarker : AMap.InfoWindowAdapter {

    companion object {
        //默认动画配置项
        const val DEFAULT_MARKER_ANIM_DURATION = 360L
        val DEFAULT_MARKER_ANIM_INTERPOLATOR = LinearInterpolator()

        //热力图颜色分布
        val ALT_HEATMAP_GRADIENT_COLORS = intArrayOf(
            Color.argb(0, 0, 0, 255),
            Color.argb(50, 0, 0, 255),
            Color.argb(153, 0, 255, 0),
            Color.argb(204, 255, 255, 0),
            Color.rgb(255, 0, 0)
        )

        //颜色分布比例
        val ALT_HEATMAP_GRADIENT_START_POINTS = floatArrayOf(
            0.0f, 0.10f, 0.20f, 0.60f, 1.0f
        )
    }

    //<editor-fold desc="配置属性">

    /**自动进入热力图模式, 当缩放级别小于等于此值时.
     * 将所有[Marker]隐藏, 并展示热力图.推荐使用15f
     *
     * 设置一个超小值, 可以关闭此特性
     * */
    var toHeatMapZoom: Float = 15f

    /**点击地图时, 需要放大地图到zoom*/
    var touchMoveMapZoom: Float = DEFAULT_LOCATION_ZOOM

    /**热力图颜色渐变配置*/
    var heatGradient = Gradient(ALT_HEATMAP_GRADIENT_COLORS, ALT_HEATMAP_GRADIENT_START_POINTS)

    /**配置[Marker]选中后的样式, 如果为null, 将关闭[Marker]选择切换回调*/
    var markerSelectedOptions: MarkerOptions? = null

    var markerSelectedOptionsAction: (Marker) -> MarkerOptions? = {
        markerSelectedOptions
    }

    /**[Marker]选中切换回调, 需要配置[markerSelectedOptions].
     * */
    var markerSelectedAction: (Marker?) -> Unit = {
        L.i("[Marker] 选中切换:${it?.id} ${it?.`object`}")
    }

    /**将要选中[Marker]回调, 此方法可以实现针对不同的[Marker], 更改不同的[markerSelectedOptions], 已达到不同的显示效果
     * 返回true, 拦截默认操作
     */
    var markerSelectedBeforeAction: (Marker) -> Boolean = {
        map?.moveTo(it.position)
        false
    }

    //</editor-fold desc="配置属性">

    //<editor-fold desc="初始化">

    var map: AMap? = null

    //管理所有Marker
    var _allMarker: MutableList<Marker> = mutableListOf()

    lateinit var _context: Context

    fun init(context: Context, map: AMap) {
        this.map = map
        this._context = context

        map.setInfoWindowAdapter(this)

        map.onCameraChangeListener(change = {
            handlerCameraChange(it)
        })

        map.onMarkerClickListener {
            handlerMarkerClick(it)
        }

        map.onMapClickListener {
            handlerMapClick(it)
        }
    }

    fun handlerMapClick(it: LatLng) {
        if (isEnableSelect) {
            selectMarker(null)
        }
        if (isHeatOverlay) {
            //热力图状态下, 点击地图. 移动到最近的一个Marker
            map?.moveTo(findLatelyLatLng(it), touchMoveMapZoom)
        }
    }

    fun handlerMarkerClick(it: Marker): Boolean {
        return if (isEnableSelect) {
            selectMarker(it)
            true
        } else {
            false
        }
    }

    fun handlerCameraChange(it: CameraPosition) {
        if (it.zoom <= toHeatMapZoom) {
            //进入热力图模式
            if (!isHeatOverlay) {
                changeToHeat()
            }
        } else {
            //显示所有的[Marker]
            if (isHeatOverlay) {
                changeToMarker()
            }
        }
    }

    fun _checkInit(action: AMap.() -> Unit) {
        if (map == null) {
            L.w("map is not init!")
        } else {
            map?.action()
        }
    }

    /**此方法不能修改整个 InfoWindow 的背景和边框，无论自定义的样式是什么样，SDK 都会在最外层添加一个默认的边框。*/
    override fun getInfoContents(marker: Marker): View? {
        //do something
        L.i("getInfoContents $marker")
        return null
    }

    val isEnableInfoWindow: Boolean = false

    var _infoWindow: View? = null

    /**如果此方法返回的 View 没有设置 InfoWindow 背景图，SDK 会默认添加一个背景图。*/
    override fun getInfoWindow(marker: Marker): View? {
        //do something
        L.i("getInfoWindow title:${marker.title} snippet:${marker.snippet}")
        if (isEnableInfoWindow) {
            if (_infoWindow == null) {
                val context = _context
                _infoWindow = LayoutInflater.from(context).inflate(
                    R.layout.map_info_window_layout,
                    FrameLayout(context),
                    false
                )
            }
            _infoWindow?.apply {
                find<TextView>(R.id.lib_title_view)?.text = marker.title
                find<TextView>(R.id.lib_des_view)?.text = marker.snippet
            }
            return _infoWindow
        } else {
            return null
        }
    }

    //</editor-fold desc="初始化">

    //<editor-fold desc="热力图操作">

    //热力图层
    var _tileOverlay: TileOverlay? = null

    //是否处于热力图模式
    val isHeatOverlay: Boolean get() = _tileOverlay != null

    /**热力图状态
     * http://a.amap.com/lbs/static/unzip/Android_Map_Doc/3D/com/amap/api/maps/model/HeatmapTileProvider.Builder.html
     * */
    fun changeToHeat() {
        //取消之前的选中[Marker]
        selectMarker(null)

        removeHeatOverlay()
        hideAllMarker()

        if (_allMarker.isEmpty()) {
            return
        }

        _checkInit {

            // 第一步： 生成热力点坐标列表
            val latLngList = getAllMarkerLatLng()

            // 第二步： 构建热力图 TileProvider
            val builder = HeatmapTileProvider.Builder().apply {
                // 设置热力图绘制的数据
                data(latLngList)
                // 设置热力图渐变，有默认值 DEFAULT_GRADIENT，可不设置该接口
                gradient(heatGradient)
            }

            // Gradient 的设置可见参考手册
            // 构造热力图对象
            val tileProvider = builder.build()

            // 第三步： 构建热力图参数对象
            val tileOverlayOptions = TileOverlayOptions()
            tileOverlayOptions.tileProvider(tileProvider) // 设置瓦片图层的提供者

            // 第四步： 添加热力图
            _tileOverlay = addTileOverlay(tileOverlayOptions)
        }
    }

    /**[Marker]状态*/
    fun changeToMarker() {
        removeHeatOverlay()
        showAllMarker()
    }

    /**移除热力图*/
    fun removeHeatOverlay() {
        _tileOverlay?.clearTileCache()
        _tileOverlay?.remove()
        _tileOverlay = null
    }

    /**隐藏所有[Marker]*/
    fun hideAllMarker() {
        _allMarker.forEach {
            it.isVisible = false
        }
    }

    /**显示所有[Marker]*/
    fun showAllMarker() {
        _allMarker.forEach {
            it.isVisible = true
        }
    }

    /**清空所有Marker*/
    fun clearAllMarker() {
        selectMarker(null)
        _allMarker.forEach {
            it.remove()
        }
        _allMarker.clear()
    }

    //</editor-fold desc="热力图操作">

    //<editor-fold desc="选择切换">

    /**是否激活了选择切换*/
    val isEnableSelect: Boolean get() = markerSelectedOptions != null

    /// 记录当前选中的[Marker]
    var currentSelectedMarker: Marker? = null

    /// 选中效果的实现, 采用隐藏真实的选中[Marker], 在当前位置使用一个替补的[Marker]显示.
    /// 此[Marker]不受[_allMarker]管理
    var currentSelectedShowMarker: Marker? = null

    /**选中指定的[Marker], 或者取消当前选中*/
    fun selectMarker(marker: Marker?) {

        if (marker != null) {
            //需要选中[Marker]
            if (currentSelectedShowMarker == marker || currentSelectedMarker == marker) {
                //目标是替身或者已经选中的[Marker]
                return
            }
            if (!_allMarker.contains(marker)) {
                //目标并不属于管理范围
                return
            }
        }

        //之前是否有选中的[Marker]
        val haveSelectedMarker = currentSelectedShowMarker != null

        //切换选中的[Marker]
        currentSelectedShowMarker?.apply {
            //移除替补[Marker]
            remove()
            currentSelectedShowMarker = null
        }

        currentSelectedMarker?.apply {
            //显示真身
            isVisible = true
            currentSelectedMarker = null
        }

        //是否被拦截了
        var intercept = false

        marker?.apply {
            //将要选中[Marker]
            intercept = markerSelectedBeforeAction(marker)

            _checkInit {
                //隐藏真身
                currentSelectedMarker = marker
                currentSelectedMarker?.isVisible = false

                //显示替身
                markerSelectedOptionsAction(this@apply)?.let {
                    it.position(marker.position)
                    currentSelectedShowMarker = addMarker(it)
                }
            }
        }

        if (intercept) {
            return
        }

        //选中/取消 操作结束
        if (marker == null) {
            if (haveSelectedMarker) {
                markerSelectedAction(marker)
            }
        } else {
            markerSelectedAction(marker)
        }
    }

    //</editor-fold desc="选择切换">

    //<editor-fold desc="操作方法">

    /**添加一个[Marker]并管理*/
    fun addMarker(
        latLng: LatLng,
        data: Any? = null,
        icon: BitmapDescriptor? = null,
        action: MarkerOptions.() -> Unit = {}
    ): Marker? {
        var result: Marker? = null
        _checkInit {
            result = addMarker {
                anchor(0.5f, 0.5f) //将图标的中心位置放置在点上
                icon(icon)
                position(latLng)
                draggable(false) //不可拖拽
                isFlat = false //贴地, 3D视角时效果明显
                visible(!isHeatOverlay)
                action()
            }.apply {
                `object` = data
                _allMarker.add(this)
            }
        }
        return result
    }

    /**通过位置, 移除一个[Marker]*/
    fun removeMarker(latLng: LatLng) {
        if (currentSelectedShowMarker?.position == latLng) {
            removeSelectedMarker()
        }
        findMarker(latLng)?.apply {
            isVisible = false
            remove()
            `object` = null
            _allMarker.remove(this)
        }
    }

    /**移动选中的marker*/
    fun removeSelectedMarker() {
        //切换选中的[Marker]
        currentSelectedShowMarker?.apply {
            //移除替补[Marker]
            remove()
            currentSelectedShowMarker = null

            currentSelectedMarker?.apply {
                //显示真身
                isVisible = true
                currentSelectedMarker = null
            }
        }
    }

    /**移除所有Marker*/
    fun removeAllMarker() {
        removeSelectedMarker()
        for (marker in _allMarker) {
            marker.isVisible = false
            marker.remove()
            marker.`object` = null
        }
        _allMarker.clear()
    }

    /**优先使用相同位置的[Marker]*/
    fun <T> resetMarker(
        latLng: LatLng,
        data: T? = null,
        icon: BitmapDescriptor? = null,
        action: MarkerOptions.() -> Unit = {}
    ) {
        _checkInit {
            val marker = findMarker(latLng)

            if (marker == null) {
                addMarker {
                    icon(icon)
                    position(latLng)
                    visible(!isHeatOverlay)
                    action()
                }.apply {
                    `object` = data
                    _allMarker.add(this)
                }
            } else {
                markerOptions {
                    icon(icon)
                    position(latLng)
                    visible(!isHeatOverlay)
                    action()

                    marker.`object` = data
                    marker.reset(this)
                }
            }
        }
    }

    fun <T> resetMarker(
        latLngList: List<LatLng>?,
        dataList: List<T?>? = null,
        icon: BitmapDescriptor? = null,
        action: MarkerOptions.(data: T?, index: Int) -> Unit = { _, _ -> }
    ) {
        val existLatLngList = ArrayList(getAllMarkerLatLng())
        latLngList?.forEachIndexed { index, latLng ->
            existLatLngList.remove(latLng)
            val data = dataList?.getOrNull(index)
            resetMarker(latLng, data, icon) {
                action(data, index)
            }
        }

        //剩下的LatLng, 就是需要移除的
        existLatLngList.forEach {
            removeMarker(it)
        }
    }

    fun <T> resetMarker(
        latLngList: List<LatLng>?,
        dataList: List<T?>? = null,
        iconAction: (LatLng, T?) -> BitmapDescriptor?,
        action: MarkerOptions.(LatLng, data: T?, index: Int) -> Unit = { _, _, _ -> }
    ) {
        val existLatLngList = ArrayList(getAllMarkerLatLng())
        latLngList?.forEachIndexed { index, latLng ->
            existLatLngList.remove(latLng)
            val data = dataList?.getOrNull(index)
            resetMarker(latLng, data, iconAction(latLng, data)) {
                action(latLng, data, index)
            }
        }

        //剩下的LatLng, 就是需要移除的
        existLatLngList.forEach {
            removeMarker(it)
        }
    }

    /**获取已经存在的所有[Marker]对应的[LatLng]集合*/
    fun getAllMarkerLatLng(): List<LatLng> {
        val result = mutableListOf<LatLng>()
        _allMarker.forEach {
            result.add(it.position)
        }
        return result
    }

    /**移动地图, 使得地图包含所有[Marker]*/
    fun moveIncludeMarker(
        includeMy: Boolean = true,
        padding: Int = 50 * dpi,
        animDuration: Long = 250
    ) {
        _checkInit {
            val latLngList = mutableListOf<LatLng>()
            if (includeMy) {
                (myLatLng() ?: AMapHelper.lastMapLocation?.toLatLng())?.let { latLngList.add(it) }
            }
            latLngList.addAll(getAllMarkerLatLng())
            moveInclude(latLngList, padding, animDuration)
        }
    }

    /**查找指定位置是否有[Marker]*/
    fun findMarker(latLng: LatLng): Marker? {
        return _allMarker.find { it.position == latLng }
    }

    /**查找指定位置附近[500米]的[Marker]对应的理想位置*/
    fun findLatelyLatLng(latLng: LatLng): LatLng {
        var result: LatLng? = null
        var minDistance: Float = Float.MAX_VALUE

        _allMarker.forEach {
            val distance = latLng.distance(it.position)
            //L.i("附近距离:$distance ${map?.scalePerPixel}")
            if (distance < 100 * (map?.scalePerPixel ?: 1f) && distance < minDistance) {
                result = it.position
                minDistance = distance
            }
        }

        return result ?: latLng
    }

    /**移动地图, 使其在一屏内显示所有[Marker]*/
    fun moveToShowAllMarker(vararg latLng: LatLng?, padding: Int = 50 * dpi) {
        _checkInit {
            val list = mutableListOf<LatLng>()
            list.addAll(getAllMarkerLatLng())

            latLng.forEach { it?.apply { list.add(it) } }

            moveInclude(list, padding)
        }
    }

    fun moveToShowAllMarker(includeMyLocation: Boolean = false, padding: Int = 50 * dpi) {
        if (includeMyLocation) {
            _checkInit {
                moveToShowAllMarker(myLocation?.toLatLng(), padding = padding)
            }
        } else {
            moveToShowAllMarker(null, padding = padding)
        }
    }

    //</editor-fold desc="操作方法">

}

fun AMap.dslMarker(context: Context, action: DslMarker.() -> Unit): DslMarker {
    val dslMarker = DslMarker()
    dslMarker.init(context, this)
    dslMarker.action()
    return dslMarker
}

fun markerOptions(action: MarkerOptions.() -> Unit): MarkerOptions {
    val options = MarkerOptions()
    options.apply {
        anchor(0.5f, 0.5f) //将图标的中心位置放置在点上
        draggable(false) //不可拖拽
        isFlat = false //贴地, 3D视角时效果明显
        action()
    }
    options.action()
    return options
}

fun Marker.reset(options: MarkerOptions) {
    alpha = options.alpha
    setAnchor(options.anchorU, options.anchorV)
    isDraggable = options.isDraggable
    if (options.icons.isListEmpty()) {
        setIcon(options.icon)
    } else {
        icons = options.icons
    }
    period = options.period
    position = options.position
    rotateAngle = options.rotateAngle
    snippet = options.snippet
    title = options.title
    zIndex = options.zIndex
    isFlat = options.isFlat
    isVisible = options.isVisible
}

//<editor-fold desc="Marker动画">

//https://a.amap.com/lbs/static/unzip/Android_Map_Doc/3D/com/amap/api/maps/model/animation/ScaleAnimation.html

fun Marker.cancelAnim() {
    val animation = AlphaAnimation(1f, 1f)
    setAnimation(animation)
    startAnimation()
}

fun Marker.anim(animation: Animation, action: Animation.() -> Unit = {}) {
    //缩放动画保持最后一帧
    animation.fillMode = Animation.FILL_MODE_BACKWARDS
    animation.setDuration(DEFAULT_MARKER_ANIM_DURATION)
    animation.setInterpolator(DEFAULT_MARKER_ANIM_INTERPOLATOR)
    animation.action()
    setAnimation(animation)
    startAnimation()
}

fun Marker.scale(from: Float, to: Float, action: ScaleAnimation.() -> Unit = {}) {
    scale(from, to, from, to, action)
}

fun Marker.scale(
    fromX: Float,
    toX: Float,
    fromY: Float,
    toY: Float,
    action: ScaleAnimation.() -> Unit = {}
) {
    val animation = ScaleAnimation(fromX, toX, fromY, toY)
    anim(animation) {
        animation.action()
    }
}

/**上下跳动动画
 * */
fun Marker.jump(map: AMap, offsetY: Int = 100 * dpi, action: TranslateAnimation.() -> Unit = {}) {
    val latLng = position
    val point = map.projection.toScreenLocation(latLng)
    point.y -= offsetY //偏移
    val fromLatLng = map.projection.fromScreenLocation(point)

    //动画
    val animation = TranslateAnimation(fromLatLng)
    anim(animation) {
        animation.setInterpolator { input ->
            // 模拟重加速度的interpolator
            if (input <= 0.5) {
                (0.5f - 2 * (0.5 - input) * (0.5 - input)).toFloat()
            } else {
                (0.5f - sqrt((input - 0.5f) * (1.5f - input).toDouble())).toFloat()
            }
        }
        animation.action()
    }
}

//</editor-fold desc="Marker动画">

//<editor-fold desc="Marker图标">

/**设置动图图标*/
fun MarkerOptions.icons(iconList: List<BitmapDescriptor>, period: Int) {
    icons(ArrayList(iconList))
    period(period)
}

fun MarkerOptions.icon(view: View) {
    icon(markerIcon(view))
}

fun MarkerOptions.icon(res: Int) {
    icon(markerIcon(res))
}

fun markerIcon(resId: Int) = BitmapDescriptorFactory.fromResource(resId)

fun markerIcon(view: View) = BitmapDescriptorFactory.fromView(view)

fun markerIcon(context: Context, layoutId: Int, init: View.() -> Unit = {}): BitmapDescriptor {
    val view = LayoutInflater.from(context).inflate(layoutId, FrameLayout(context), false)
    view.init()
    return markerIcon(view)
}

//</editor-fold desc="Marker图标">


