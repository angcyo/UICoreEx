package com.angcyo.canvas2.laser.pecker.engrave

import android.graphics.RectF
import com.angcyo.bluetooth.fsc.laserpacker.DeviceStateModel
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker._productName
import com.angcyo.bluetooth.fsc.laserpacker.filterFileName
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
import com.angcyo.laserpacker.device.MaterialHelper
import com.angcyo.laserpacker.device.data.EngraveLayerInfo
import com.angcyo.laserpacker.device.updateAllLayerConfig
import com.angcyo.library.component.pool.acquireTempRectF
import com.angcyo.library.component.pool.release
import com.angcyo.library.unit.toMm
import com.angcyo.objectbox.findLast
import com.angcyo.objectbox.laser.pecker.LPBox
import com.angcyo.objectbox.laser.pecker.entity.EngraveConfigEntity
import com.angcyo.objectbox.laser.pecker.entity.EngraveConfigEntity_
import com.angcyo.objectbox.laser.pecker.entity.TransferConfigEntity
import com.angcyo.objectbox.laser.pecker.entity.TransferConfigEntity_
import com.angcyo.objectbox.laser.pecker.lpSaveEntity
import com.angcyo.objectbox.queryOrCreateEntity
import com.angcyo.objectbox.removeAll

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
        val rendererList = delegate.getSelectorOrAllElementRendererList(true, false, false)
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

    /**所有元素都配置了雕刻参数*/
    fun isAllConfigEngraveParams(delegate: CanvasRenderDelegate?): Boolean {
        delegate ?: return false
        val list = getLayerRendererList(delegate, null)
        for (renderer in list) {
            if (renderer.lpElementBean()?._isConfigEngraveParams == true) {
                //same
            } else {
                //未配置参数
                return false
            }
        }
        return true
    }

    /**获取未配置参数的渲染器*/
    fun getNoConfigEngraveParamsRenderer(delegate: CanvasRenderDelegate?): BaseRenderer? {
        delegate ?: return null
        val list = getLayerRendererList(delegate, null)
        for (renderer in list) {
            if (renderer.lpElementBean()?._isConfigEngraveParams == true) {
                //same
            } else {
                //未配置参数
                return renderer
            }
        }
        return null
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
            dataDir = vmApp<LaserPeckerModel>().dataDir() //在发送数据时, 再设置一次

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

    /**构建一个传输信息从[LPElementBean]

     * [generateEngraveConfig]
     * */
    fun generateTransferConfig(
        taskId: String?,
        renderer: BaseRenderer,
        bean: LPElementBean
    ): TransferConfigEntity {
        val index = bean.index ?: EngraveHelper.generateEngraveIndex()
        bean.index = index
        TransferConfigEntity::class.removeAll(LPBox.PACKAGE_NAME) {
            apply(
                TransferConfigEntity_.taskId.equal("$taskId")
                    .and(TransferConfigEntity_.index.equal("$index"))
            )
        }

        return TransferConfigEntity().apply {
            this.taskId = taskId
            this.index = "$index"
            name = (bean.name ?: EngraveHelper.generateEngraveName()).filterFileName()
            layerJson = LayerHelper.getProductLayerSupportPxJson()
                .updateAllLayerConfig(bean.dpi ?: LayerHelper.getProductLastLayerDpi(bean.layerId))
            dataDir = vmApp<LaserPeckerModel>().dataDir()

            val originBounds = renderer.getRendererBounds()
            originWidth = originBounds?.width().toMm()
            originHeight = originBounds?.height().toMm()

            lpSaveEntity()
        }
    }

    /**单元素数据传输的配置*/
    fun getTransferConfig(taskId: String?, index: String): TransferConfigEntity? {
        return TransferConfigEntity::class.findLast(LPBox.PACKAGE_NAME) {
            apply(
                TransferConfigEntity_.taskId.equal("$taskId")
                    .and(TransferConfigEntity_.index.equal(index))
            )
        }
    }

    /**构建一个雕刻参数信息从[LPElementBean]
     *
     * [com.angcyo.engrave2.EngraveFlowDataHelper.generateEngraveConfig]
     *
     * [com.angcyo.engrave2.EngraveFlowDataHelper.generateEngraveConfig(java.lang.String, java.lang.String)]
     * */
    fun generateEngraveConfig(
        taskId: String?,
        bean: LPElementBean
    ): EngraveConfigEntity {
        val layerId = bean._layerId

        return EngraveConfigEntity::class.queryOrCreateEntity(LPBox.PACKAGE_NAME, {
            this.taskId = taskId
            index = "${bean.index}"

            //材质
            val customMaterial =
                MaterialHelper.createCustomLayerMaterialList().find { it.layerId == layerId }
            EngraveFlowDataHelper.initEngraveConfigWithMaterial(this, customMaterial)

            this.layerId = layerId

            //获取最后一次相同图层的雕刻参数
            val productName = _productName
            val last = EngraveConfigEntity::class.findLast(LPBox.PACKAGE_NAME) {
                apply(
                    EngraveConfigEntity_.productName.equal("$productName")
                        .and(EngraveConfigEntity_.layerId.equal(layerId ?: ""))
                )
            }

            //光源, 此处配置有多个地方, 使用find查找
            type = bean.printType?.toByte() ?: last?.type ?: DeviceHelper.getProductLaserType()
            precision = bean.printPrecision ?: last?.precision ?: HawkEngraveKeys.lastPrecision
            pump = bean.pump ?: last?.pump ?: pump
            power = bean.printPower ?: last?.power ?: HawkEngraveKeys.lastPower
            depth = bean.printDepth ?: last?.depth ?: HawkEngraveKeys.lastDepth
            time = bean.printCount ?: 1
            dpi = bean.dpi //2023-7-29
            useLaserFrequency = bean.useLaserFrequency //2023-11-4
            laserFrequency = bean.laserFrequency

            deviceAddress = LaserPeckerHelper.lastDeviceAddress()
            this.productName = productName

            materialCode = bean.materialCode
            materialKey = bean.materialKey
            materialName = bean.materialName

            lpSaveEntity()

        }) {
            apply(
                EngraveConfigEntity_.taskId.equal("$taskId")
                    .and(EngraveConfigEntity_.index.equal("${bean.index}"))
            )
        }
    }

    /**创建单文件雕刻参数
     * [generateTransferConfig]
     * */
    fun generateEngraveConfig(delegate: CanvasRenderDelegate?, taskId: String?) {
        delegate?.let {
            val rendererList = getLayerRendererList(delegate, null)
            rendererList.forEach { renderer ->
                renderer.lpElementBean()?.let { bean ->
                    //为每个元素创建对应的雕刻参数
                    generateEngraveConfig(taskId, bean)
                }
            }
        }
    }

    /**创建单文件传输参数
     * [generateEngraveConfig]*/
    fun generateTransferConfig(delegate: CanvasRenderDelegate?, taskId: String?) {
        delegate?.let {
            val rendererList = getLayerRendererList(delegate, null)
            rendererList.forEach { renderer ->
                renderer.lpElementBean()?.let { bean ->
                    //为每个元素创建对应的雕刻参数
                    generateTransferConfig(taskId, renderer, bean)
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