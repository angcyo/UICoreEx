package com.angcyo.canvas2.laser.pecker.util

import com.angcyo.bluetooth.fsc.laserpacker._deviceSettingBean
import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas.render.element.IElement
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas.render.renderer.CanvasElementRenderer
import com.angcyo.canvas.render.renderer.CanvasGroupRenderer
import com.angcyo.canvas.render.util.renderElement
import com.angcyo.canvas2.laser.pecker.element.ILaserPeckerElement
import com.angcyo.canvas2.laser.pecker.element.LPBitmapElement
import com.angcyo.canvas2.laser.pecker.element.LPPathElement
import com.angcyo.canvas2.laser.pecker.element.LPTextElement
import com.angcyo.http.base.copyByJson
import com.angcyo.http.base.listType
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.laserpacker.bean.LPElementBean
import com.angcyo.laserpacker.bean.LPVariableBean
import com.angcyo.laserpacker.bean.initFileCacheIfNeed
import com.angcyo.laserpacker.generateGroupName
import com.angcyo.laserpacker.generateName
import com.angcyo.library.annotation.CallPoint
import com.angcyo.library.annotation.MM
import com.angcyo.library.canvas.core.Reason
import com.angcyo.library.component.Strategy
import com.angcyo.library.ex.size
import com.angcyo.library.utils.uuid

/**
 * LP渲染器操作助手
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/08
 */
object LPRendererHelper {

    @MM
    const val POSITION_STEP = 5f

    /**解析对应的数据结构, 返回可以被渲染的元素*/
    @CallPoint
    fun parseElementBean(bean: LPElementBean): ILaserPeckerElement? = when (bean.mtype) {
        LPDataConstant.DATA_TYPE_BITMAP -> LPBitmapElement(bean).apply {
            if (bean._srcBitmap != null) {
                renderBitmap = bean._srcBitmap
            }
            if (bean._imageOriginalBitmap != null) {
                bean._imageOriginalBitmap?.let { bitmap ->
                    if (bean.imageFilter == LPDataConstant.DATA_MODE_GCODE) {
                        originBitmap = bitmap
                    } else {
                        updateOriginBitmap(bitmap, false)
                    }
                }
            }
        }

        LPDataConstant.DATA_TYPE_TEXT,
        LPDataConstant.DATA_TYPE_QRCODE,
        LPDataConstant.DATA_TYPE_BARCODE,
        LPDataConstant.DATA_TYPE_VARIABLE_TEXT,
        LPDataConstant.DATA_TYPE_VARIABLE_QRCODE,
        LPDataConstant.DATA_TYPE_VARIABLE_BARCODE -> LPTextElement(bean)

        LPDataConstant.DATA_TYPE_LINE,
        LPDataConstant.DATA_TYPE_RECT,
        LPDataConstant.DATA_TYPE_OVAL,
        LPDataConstant.DATA_TYPE_LOVE,
        LPDataConstant.DATA_TYPE_POLYGON,
        LPDataConstant.DATA_TYPE_PENTAGRAM,
        LPDataConstant.DATA_TYPE_PEN,
        LPDataConstant.DATA_TYPE_PATH,
        LPDataConstant.DATA_TYPE_SVG,
        LPDataConstant.DATA_TYPE_GCODE -> LPPathElement(bean)

        else -> null
    }

    /**解析数据结构到对应的渲染器
     * [assignLocation] 是否要重新分配位置*/
    fun parseElementRenderer(
        bean: LPElementBean,
        assignLocation: Boolean = false
    ): CanvasElementRenderer? {
        return parseElementBean(bean)?.run {
            val renderer = CanvasElementRenderer()
            updateBeanToElement(renderer)
            if (assignLocation) {
                LPElementHelper.assignLocation(bean)
                updateBeanToElement(renderer)
                renderer.updateRenderProperty()
            }
            renderer.renderElement = this
            renderer
        }
    }

    /**[parseElementRenderer]*/
    fun parseElementRendererList(
        beanList: List<LPElementBean>,
        assignLocation: Boolean = false
    ): List<BaseRenderer> {
        val list = renderElementList(null, beanList, false, Strategy.preview)
        if (assignLocation) {
            LPElementHelper.assignLocation(list)
        }
        return list
    }

    /**渲染元素列表
     * [selected] 是否要选中渲染器
     * @return 返回渲染器集合*/
    fun renderElementList(
        delegate: CanvasRenderDelegate?,
        beanList: List<LPElementBean>,
        selected: Boolean,
        strategy: Strategy
    ): List<BaseRenderer> {
        val result = mutableListOf<BaseRenderer>()
        if (beanList.isEmpty()) {
            return result
        }

        //组内子元素
        val jumpList = mutableListOf<LPElementBean>()

        //分配名称
        val allElementBeanList = mutableListOf<LPElementBean>()
        if (delegate != null) {
            for (element in delegate.renderManager.getAllSingleElementList()) {
                if (element is ILaserPeckerElement) {
                    allElementBeanList.add(element.elementBean)
                }
            }
        }
        allElementBeanList.addAll(beanList)
        allElementBeanList.generateName()//end

        for ((index, bean) in beanList.withIndex()) {
            if (jumpList.contains(bean)) {
                continue
            }

            parseElementRenderer(bean)?.let { renderer ->
                val groupId = bean.groupId
                val groupName = bean.groupName ?: allElementBeanList.generateGroupName()
                if (groupId == null) {
                    //当前元素不带分组信息
                    result.add(renderer)
                } else {
                    //有分组信息
                    val groupRenderer = CanvasGroupRenderer()
                    val subRendererList = mutableListOf<BaseRenderer>()
                    subRendererList.add(renderer)

                    for (i in (index + 1) until beanList.size) {
                        //往后找相同groupId的元素
                        val subBean = beanList[i]
                        if (subBean.groupId == groupId) {
                            subBean.groupName = groupName
                            jumpList.add(subBean)
                            parseElementRenderer(subBean)?.let { subRenderer ->
                                subRendererList.add(subRenderer)
                            }
                        }
                    }

                    if (subRendererList.size() > 1) {
                        //如果组内元素大于1个

                        groupRenderer.resetGroupRendererList(subRendererList, Reason.init, null)
                        result.add(groupRenderer)
                    } else {
                        //只有1个元素
                        result.add(renderer)
                    }
                }
            }
        }
        delegate?.renderManager?.addElementRenderer(result, selected, Reason.user, strategy)
        return result
    }

    /**复制渲染器*/
    fun copyRenderer(
        delegate: CanvasRenderDelegate,
        rendererList: List<BaseRenderer>,
        offset: Boolean = _deviceSettingBean?.copyElementOffset == true
    ) {
        //复制元素, 主要就是复制元素的数据
        val elementBeanList = mutableListOf<LPElementBean>()
        for (renderer in rendererList) {
            elementBeanList.addAll(copyRenderer(renderer, null, offset))
        }
        renderElementList(delegate, elementBeanList, true, Strategy.normal)
    }

    private fun copyRenderer(
        rootRenderer: BaseRenderer,
        groupId: String?,
        offset: Boolean
    ): List<LPElementBean> {
        val elementBeanList = mutableListOf<LPElementBean>()
        if (rootRenderer is CanvasGroupRenderer) {
            //分组, 则组内所有元素重新分配group id
            val newGroupId: String = uuid()
            for (renderer in rootRenderer.rendererList) {
                elementBeanList.addAll(copyRenderer(renderer, newGroupId, offset))
            }
        } else {
            rootRenderer.lpElement()?.apply {
                updateBeanFromElement(rootRenderer)
                val newBean = elementBean.copy()

                newBean.variables =
                    elementBean.variables?.copyByJson(listType(LPVariableBean::class.java))
                newBean.variables?.initFileCacheIfNeed(true)
                //复制完元素之后, 是否要清空数据缓存?

                if (this is LPBitmapElement) {
                    newBean._srcBitmap = renderBitmap
                    newBean._imageOriginalBitmap = originBitmap
                }

                if (newBean.groupId != null) {
                    newBean.groupId = groupId
                }
                if (offset) {
                    newBean.left += POSITION_STEP
                    newBean.top += POSITION_STEP
                    newBean.clearIndex("复制渲染器", false) //清空索引
                }
                elementBeanList.add(newBean)
            }
        }
        return elementBeanList
    }

    /**分配一个元素名称*/
    fun generateName(delegate: CanvasRenderDelegate?) {
        delegate ?: return
        //分配名称
        val allElementBeanList = mutableListOf<LPElementBean>()
        for (element in delegate.renderManager.getAllSingleElementList()) {
            if (element is ILaserPeckerElement) {
                allElementBeanList.add(element.elementBean)
            }
        }
        allElementBeanList.generateName()//end
    }

}

//region---LpRenderer---

fun IElement?.lpElement(): ILaserPeckerElement? {
    if (this is ILaserPeckerElement) {
        return this
    }
    return null
}

/**[ILaserPeckerElement]*/
fun BaseRenderer.lpElement(): ILaserPeckerElement? {
    val element = renderElement
    if (element is ILaserPeckerElement) {
        return element
    }
    return null
}

/**[LPElementBean]*/
fun BaseRenderer.lpElementBean(): LPElementBean? = lpElement()?.elementBean

/**[LPBitmapElement]*/
fun BaseRenderer.lpBitmapElement(): LPBitmapElement? {
    val element = lpElement()
    if (element is LPBitmapElement) {
        return element
    }
    return null
}

/**[LPTextElement]*/
fun BaseRenderer.lpTextElement(): LPTextElement? {
    val element = lpElement()
    if (element is LPTextElement) {
        return element
    }
    return null
}

/**[LPPathElement]*/
fun BaseRenderer.lpPathElement(): LPPathElement? {
    val element = lpElement()
    if (element is LPPathElement) {
        return element
    }
    return null
}

fun CanvasRenderDelegate.updateElementAfterEngrave() {
    getAllSingleElementRendererList().updateElementAfterEngrave(this)
}

/**雕刻完成, 更新变量文本*/
fun List<BaseRenderer>.updateElementAfterEngrave(renderDelegate: CanvasRenderDelegate?) {
    for (renderer in this) {
        renderer.lpTextElement()?.updateElementAfterEngrave(renderer, renderDelegate)
    }
}

fun CanvasRenderDelegate.updateElementAutoDateTime() {
    getAllSingleElementRendererList().updateElementAutoDateTime(this)
}

/**更新变量文本-仅更新时间变量*/
fun List<BaseRenderer>.updateElementAutoDateTime(renderDelegate: CanvasRenderDelegate?) {
    for (renderer in this) {
        renderer.lpTextElement()?.updateElementAutoDateTime(renderer, renderDelegate)
    }
}

//endregion---LpRenderer---