package com.angcyo.canvas2.laser.pecker.engrave

import android.graphics.RectF
import com.angcyo.bluetooth.fsc.laserpacker.DeviceStateModel
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.isOverflowProductBounds
import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas.render.renderer.CanvasGroupRenderer
import com.angcyo.canvas.render.util.renderElement
import com.angcyo.canvas2.laser.pecker.util.lpElementBean
import com.angcyo.core.vmApp
import com.angcyo.engrave2.EngraveFlowDataHelper
import com.angcyo.laserpacker.bean.LPElementBean
import com.angcyo.laserpacker.device.DeviceHelper
import com.angcyo.laserpacker.device.EngraveHelper
import com.angcyo.laserpacker.device.LayerHelper
import com.angcyo.laserpacker.device.data.EngraveLayerInfo
import com.angcyo.library.component.pool.acquireTempRectF
import com.angcyo.library.component.pool.release
import com.angcyo.library.unit.toMm
import com.angcyo.objectbox.laser.pecker.LPBox
import com.angcyo.objectbox.laser.pecker.entity.EngraveConfigEntity
import com.angcyo.objectbox.laser.pecker.entity.TransferConfigEntity
import com.angcyo.objectbox.laser.pecker.entity.TransferConfigEntity_
import com.angcyo.objectbox.laser.pecker.lpSaveEntity
import com.angcyo.objectbox.queryOrCreateEntity

/**
 * 业务相关的雕刻助手工具类
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/28
 */
object LPEngraveHelper {

    /**获取在范围内的所有有效的渲染器*/
    fun getAllValidRendererList(delegate: CanvasRenderDelegate?): List<BaseRenderer>? {
        delegate ?: return null
        val result = mutableListOf<BaseRenderer>()

        val rendererList = delegate.getSelectorOrAllElementRendererList(true, false)
        rendererList.filterTo(result) { renderer ->
            renderer.isVisible &&
                    !renderer.renderProperty?.getRenderBounds().isOverflowProductBounds()
        }

        return result
    }

    /**根据雕刻图层, 获取对应选中的渲染器
     * [layerInfo] 指定雕刻图层, 不指定时, 表示所有.
     * [sort] 是否要排序, 排序规则离左上角越近的优先
     * */
    fun getLayerRendererList(
        delegate: CanvasRenderDelegate,
        layerInfo: EngraveLayerInfo? = null, /*需要获取的图层*/
        sort: Boolean = false
    ): List<BaseRenderer> {
        val rendererList = delegate.getSelectorOrAllElementRendererList(true, false)
        val haveCutLayer = vmApp<DeviceStateModel>().haveCutLayer()
        val resultList = rendererList.filter { it.isVisible && it.renderElement != null }.filter {
            val elementBean = it.lpElementBean()
            layerInfo == null || /*不指定图层, 则返回所有元素*/
                    elementBean?._layerId == layerInfo.layerId || /*指定图层, 则返回对应的图层元素*/
                    (layerInfo.layerId == LaserPeckerHelper.LAYER_LINE /*查询线条图层*/ &&
                            !haveCutLayer /*不支持切割图层*/ &&
                            elementBean?._layerId == LaserPeckerHelper.LAYER_CUT /*切割数据*/) /*线条图层, 在不支持切割图层时, 需要返回切割图层元素*/
        }

        return if (sort) {
            resultList.engraveSort()
        } else {
            resultList
        }
    }

    /**获取不是指定雕刻图层的渲染器*/
    fun getLayerRendererListNot(
        delegate: CanvasRenderDelegate,
        layerInfo: EngraveLayerInfo?,
        sort: Boolean = false
    ): List<BaseRenderer> {
        //获取所有元素
        val rendererList = mutableListOf<BaseRenderer>()
        rendererList.addAll(delegate.getSelectorOrAllElementRendererList(true, false))

        //获取图层对应的所有元素
        val layerList = getLayerRendererList(delegate, layerInfo, sort)

        //移除图层元素, 返回的就是非本图层的元素了
        rendererList.removeAll(layerList)

        return if (sort) {
            rendererList.engraveSort()
        } else {
            rendererList
        }
    }

    /**获取选中元素的图层id列表*/
    fun getSelectElementLayerList(delegate: CanvasRenderDelegate?): List<String> {
        val result = mutableListOf<String>()
        delegate ?: return result
        val list = getLayerRendererList(delegate, null)
        for (renderer in list) {
            renderer.lpElementBean()?._layerId?.let {
                if (!result.contains(it)) {
                    result.add(it)
                }
            }
        }
        return result
    }

    /**获取选中元素的图层信息列表*/
    fun getSelectElementLayerInfoList(delegate: CanvasRenderDelegate?): List<EngraveLayerInfo> {
        val result = mutableListOf<EngraveLayerInfo>()
        getSelectElementLayerList(delegate).forEach {
            LayerHelper.getEngraveLayerInfo(it)?.let { layerInfo ->
                result.add(layerInfo)
            }
        }
        return result
    }

    /**[isAllSameLayerMode]*/
    fun isAllSameLayerMode(delegate: CanvasRenderDelegate?, layerMode: Int): Boolean {
        delegate ?: return false
        val list = getLayerRendererList(delegate, null)
        return isAllSameLayerMode(list, layerMode)
    }

    /**判断雕刻的数据类型是否全部一致*/
    fun isAllSameLayerMode(list: List<BaseRenderer>, layerMode: Int): Boolean {
        for (renderer in list) {
            if (renderer.lpElementBean()?._layerMode == layerMode) {
                //same
            } else {
                //不一致
                return false
            }
        }
        return true
    }

    /**构建或者获取生成数据需要的配置信息
     * [taskId] 可以为空*/
    fun generateTransferConfig(
        taskId: String?,
        delegate: CanvasRenderDelegate?
    ): TransferConfigEntity {
        delegate ?: return EngraveFlowDataHelper.getOrGenerateTransferConfig(taskId)
        var newFileName = false
        return TransferConfigEntity::class.queryOrCreateEntity(LPBox.PACKAGE_NAME) {
            if (!taskId.isNullOrBlank()) {
                apply(TransferConfigEntity_.taskId.equal("$taskId"))
            } else {
                newFileName = true
            }
        }.apply {
            //参数设置
            this.taskId = taskId
            layerJson = LayerHelper.getProductLayerSupportPxJson()
            mergeData = false
            dataMode = null

            //数据dpi恢复
            val list = getLayerRendererList(delegate, null)
            //工程占用的宽高mm单位
            val originBounds =
                CanvasGroupRenderer.getRendererListRenderProperty(list).getRenderBounds(RectF())
            originWidth = originBounds.width().toMm()
            originHeight = originBounds.height().toMm()

            if (name.isEmpty() || newFileName) {
                name = EngraveHelper.generateEngraveName()
            }
        }
    }

    /**构建一个雕刻参数信息从[LPElementBean]*/
    fun generateEngraveConfig(
        taskId: String?,
        bean: LPElementBean
    ): EngraveConfigEntity {
        val layerId = bean._layerId
        return EngraveFlowDataHelper.generateEngraveConfig(taskId, layerId).apply {
            type = bean.printType?.toByte() ?: DeviceHelper.getProductLaserType()
            precision = bean.printPrecision ?: HawkEngraveKeys.lastPrecision
            power = bean.printPower ?: HawkEngraveKeys.lastPower
            depth = bean.printDepth ?: HawkEngraveKeys.lastDepth
            time = bean.printCount ?: 1
            dpi = bean.dpi //2023-7-29

            deviceAddress = LaserPeckerHelper.lastDeviceAddress()
            productName = vmApp<LaserPeckerModel>().productInfoData.value?.name
            lpSaveEntity()
        }
    }

    /**创建单文件雕刻参数*/
    fun generateEngraveConfig(delegate: CanvasRenderDelegate?) {
        delegate?.let {
            val rendererList = getLayerRendererList(delegate, null)
            rendererList.forEach { renderer ->
                renderer.lpElementBean()?.let { bean ->
                    //为每个元素创建对应的雕刻参数
                    generateEngraveConfig("${bean.index}", bean)
                }
            }
        }
    }

    /**通过数据索引[indexList], 获取对应的[BaseRenderer]*/
    fun getRendererList(
        delegate: CanvasRenderDelegate?,
        indexList: List<String>?
    ): List<BaseRenderer>? {
        indexList ?: return null
        val allRendererList = delegate?.getAllSingleElementRendererList() ?: return null
        val result = mutableListOf<BaseRenderer>()
        indexList.forEach { index ->
            allRendererList.find { "${it.lpElementBean()?.index}" == index }
                ?.let { item -> result.add(item) }
        }
        return result
    }

}

/**排序规则从上到下, 从左到右
 * 比较它的两个参数的顺序。
 * 如果两个参数相等，则返回零,0;
 * 如果第一个参数小于第二个参数，则返回负数,-1;
 * 如果第一个参数大于第二个参数，则返回正数,1;
 * 从小到大的自然排序
 * */
fun List<BaseRenderer>.engraveSort(): List<BaseRenderer> {
    //return sortedBy { it.getRotateBounds().top }
    val bounds1 = acquireTempRectF()
    val bounds2 = acquireTempRectF()
    val result = sortedWith { left, right ->
        try {
            val leftBounds = left.renderProperty!!.getRenderBounds(bounds1)
            val rightBounds = right.renderProperty!!.getRenderBounds(bounds2)
            if (leftBounds.top == rightBounds.top) {
                leftBounds.left.compareTo(rightBounds.left)
            } else {
                leftBounds.top.compareTo(rightBounds.top)
            }
        } catch (e: Exception) {
            0
        }
    }
    bounds1.release()
    bounds2.release()
    return result
}