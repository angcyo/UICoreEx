package com.angcyo.canvas2.laser.pecker.engrave

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.isOverflowProductBounds
import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas.render.util.renderElement
import com.angcyo.canvas2.laser.pecker.util.lpElementBean
import com.angcyo.engrave2.EngraveFlowDataHelper
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.laserpacker.bean.LPElementBean
import com.angcyo.laserpacker.device.DeviceHelper
import com.angcyo.laserpacker.device.EngraveHelper
import com.angcyo.laserpacker.device.HawkEngraveKeys
import com.angcyo.laserpacker.device.data.EngraveLayerInfo
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

        val rendererList = delegate.getSelectorOrAllElementRendererList(false, false)
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
        layerInfo: EngraveLayerInfo? = null, /**/
        sort: Boolean = false
    ): List<BaseRenderer> {
        val rendererList = delegate.getSelectorOrAllElementRendererList(true, false)

        val resultList = rendererList.filter { it.isVisible && it.renderElement != null }.filter {
            layerInfo == null || it.lpElementBean()?._layerMode == layerInfo.layerMode
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
        delegate ?: return EngraveFlowDataHelper.generateTransferConfig(taskId)
        return TransferConfigEntity::class.queryOrCreateEntity(LPBox.PACKAGE_NAME) {
            if (!taskId.isNullOrBlank()) {
                apply(TransferConfigEntity_.taskId.equal("$taskId"))
            }
        }.apply {
            //参数设置
            this.taskId = taskId
            val supportPxList = LaserPeckerHelper.findProductSupportPxList()
            val find = supportPxList.find { it.dpi == HawkEngraveKeys.lastDpi }
            //优先使用存在的最后一次使用的dpi, 否则默认使用第一个dpi
            dpi = find?.dpi ?: (supportPxList.firstOrNull()?.dpi ?: LaserPeckerHelper.DPI_254)
            mergeData = false
            dataMode = null

            //数据dpi恢复
            val list = getLayerRendererList(delegate, null)

            val isAllGCode = isAllSameLayerMode(list, LPDataConstant.DATA_MODE_GCODE)

            if (isAllGCode) {
                //全部是GCode则只能是1k
                dpi = LaserPeckerHelper.DPI_254
            } else {
                list.find {
                    (it.lpElementBean()?.dpi ?: 0f) > 0f
                }?.let { renderer ->
                    dpi = renderer.lpElementBean()?.dpi ?: dpi
                }
            }

            if (name.isEmpty()) {
                name = EngraveHelper.generateEngraveName()
            }
        }
    }

    /**构建一个雕刻参数信息从[LPElementBean]*/
    fun generateEngraveConfig(
        taskId: String?,
        bean: LPElementBean
    ): EngraveConfigEntity {
        val layerMode = bean._layerMode ?: -1
        return EngraveFlowDataHelper.generateEngraveConfig(taskId, layerMode).apply {
            power = bean.printPower ?: HawkEngraveKeys.lastPower
            depth = bean.printDepth ?: HawkEngraveKeys.lastDepth
            time = bean.printCount ?: 1

            type = bean.printType?.toByte() ?: DeviceHelper.getProductLaserType()
            precision = bean.printPrecision ?: HawkEngraveKeys.lastPrecision

            deviceAddress = LaserPeckerHelper.lastDeviceAddress()

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

}

/**排序规则从上到下, 从左到右
 * 比较它的两个参数的顺序。
 * 如果两个参数相等，则返回零;
 * 如果第一个参数小于第二个参数，则返回负数;
 * 如果第一个参数大于第二个参数，则返回正数;
 * 从小到大的自然排序
 * */
fun List<BaseRenderer>.engraveSort(): List<BaseRenderer> {
    //return sortedBy { it.getRotateBounds().top }
    return sortedWith { left, right ->
        val leftBounds = left.renderProperty!!.getRenderBounds()
        val rightBounds = right.renderProperty!!.getRenderBounds()
        if (leftBounds.top == rightBounds.top) {
            leftBounds.left.compareTo(rightBounds.left)
        } else {
            leftBounds.top.compareTo(rightBounds.top)
        }
    }
}