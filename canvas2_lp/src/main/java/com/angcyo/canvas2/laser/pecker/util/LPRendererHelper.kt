package com.angcyo.canvas2.laser.pecker.util

import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas.render.core.Reason
import com.angcyo.canvas.render.core.Strategy
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas.render.renderer.CanvasElementRenderer
import com.angcyo.canvas.render.renderer.CanvasGroupRenderer
import com.angcyo.canvas.render.util.renderElement
import com.angcyo.canvas2.laser.pecker.bean.LPElementBean
import com.angcyo.canvas2.laser.pecker.bean.LPProjectBean
import com.angcyo.canvas2.laser.pecker.element.ILaserPeckerElement
import com.angcyo.canvas2.laser.pecker.element.LPBitmapElement
import com.angcyo.canvas2.laser.pecker.element.LPPathElement
import com.angcyo.canvas2.laser.pecker.element.LPTextElement
import com.angcyo.canvas2.laser.pecker.generateName
import com.angcyo.engrave.data.HawkEngraveKeys
import com.angcyo.http.base.*
import com.angcyo.http.rx.doBack
import com.angcyo.library.annotation.MM
import com.angcyo.library.ex.nowTime
import com.angcyo.library.ex.readText
import com.angcyo.library.ex.size
import com.angcyo.library.ex.toBase64Data
import com.angcyo.library.utils.uuid
import com.angcyo.library.utils.writeTo
import java.io.File

/**
 * LP渲染器操作助手
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/08
 */
object LPRendererHelper {

    @MM
    const val POSITION_STEP = 5f

    /**解析对应的数据结构, 返回可以被渲染的元素*/
    fun parseElementBean(bean: LPElementBean): ILaserPeckerElement? = when (bean.mtype) {
        LPConstant.DATA_TYPE_BITMAP -> LPBitmapElement(bean)
        LPConstant.DATA_TYPE_TEXT,
        LPConstant.DATA_TYPE_QRCODE,
        LPConstant.DATA_TYPE_BARCODE -> LPTextElement(bean)
        LPConstant.DATA_TYPE_SVG,
        LPConstant.DATA_TYPE_GCODE -> LPPathElement(bean)
        else -> null
    }

    fun parseElementRenderer(bean: LPElementBean): CanvasElementRenderer? {
        return parseElementBean(bean)?.run {
            val renderer = CanvasElementRenderer()
            updateBeanToElement(renderer)
            renderer.renderElement = this
            renderer
        }
    }

    /**渲染元素列表
     * [selected] 是否要选中渲染器
     * @return 返回渲染器集合*/
    fun renderElementList(
        delegate: CanvasRenderDelegate,
        beanList: List<LPElementBean>,
        selected: Boolean,
        strategy: Strategy
    ): List<BaseRenderer> {
        val result = mutableListOf<BaseRenderer>()

        //组内子元素
        val jumpList = mutableListOf<LPElementBean>()

        //分配名称
        val allElementBeanList = mutableListOf<LPElementBean>()
        for (element in delegate.renderManager.getAllElementList()) {
            if (element is ILaserPeckerElement) {
                allElementBeanList.add(element.elementBean)
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
                            jumpList.add(subBean)
                            parseElementRenderer(bean)?.let { subRenderer ->
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
        delegate.renderManager.addElementRenderer(result, selected, Reason.user, strategy)
        return result
    }

    /**复制渲染器*/
    fun copyRenderer(
        delegate: CanvasRenderDelegate,
        rendererList: List<BaseRenderer>,
        offset: Boolean
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
                if (newBean.groupId != null) {
                    newBean.groupId = groupId
                }
                if (offset) {
                    newBean.left += POSITION_STEP
                    newBean.top += POSITION_STEP
                    newBean.index = null//清空索引
                }
                elementBeanList.add(newBean)
            }
        }
        return elementBeanList
    }

    /**分配一个元素名称*/
    fun generateName(delegate: CanvasRenderDelegate) {
        //分配名称
        val allElementBeanList = mutableListOf<LPElementBean>()
        for (element in delegate.renderManager.getAllElementList()) {
            if (element is ILaserPeckerElement) {
                allElementBeanList.add(element.elementBean)
            }
        }
        allElementBeanList.generateName()//end
    }

}

//region---Bean---

/**
 * 支持[LPProjectBean]
 * 支持[LPElementBean]
 * */
typealias CanvasRenderDataType = Any

/**json字符串转换成[LPProjectBean]*/
fun String.toProjectBean() = fromJson<LPProjectBean>()

/**json字符串转换成[LPElementBean]*/
fun String.toElementBean() = fromJson<LPElementBean>()

/**json字符串转换成[List<LPElementBean>]*/
fun String.toElementBeanList() = fromJson<List<LPElementBean>>(listType(LPElementBean::class.java))

//endregion---Bean---

//region---Delegate---

/**删除项目文件*/
fun deleteProjectFile(name: String = ".temp"): Boolean {
    val file = CanvasDataHandleOperate._defaultProjectOutputFile(name, false)
    return file.delete()
}

/**获取工程结构[LPProjectBean]*/
fun CanvasRenderDelegate.getProjectBean(renderList: List<BaseRenderer>? = renderManager.elementRendererList): LPProjectBean {
    return LPProjectBean().apply {
        create_time = nowTime()
        update_time = nowTime()

        val preview = preview(
            overrideSize = HawkEngraveKeys.projectOutSize.toFloat(),
            rendererList = renderList
        )
        preview_img = preview?.toBase64Data()

        data = jsonArray {
            renderList?.forEach { renderer ->
                val list = renderer.getRendererList()
                list.forEach { sub ->
                    try {
                        sub.lpElement()?.let { element ->
                            element.updateBeanFromElement(sub)
                            add(element.elementBean.toJson().json())
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }.toString()
    }
}

/**打开工程文件*/
fun CanvasRenderDelegate.openProjectFile(file: File?, clearOld: Boolean = true): LPProjectBean? {
    val projectBean = file?.readText()?.toProjectBean()
    openProjectBean(projectBean, clearOld)
    return projectBean
}

/**打开工程文件*/
fun CanvasRenderDelegate.openProjectBean(
    projectBean: LPProjectBean?,
    clearOld: Boolean = true
): Boolean {
    if (clearOld) {
        renderManager.removeAllElementRenderer(Strategy.init)
        undoManager.clear()
    }
    val result = projectBean?.data?.toElementBeanList()?.let { beanList ->
        beanList.generateName()
        LPRendererHelper.renderElementList(this, beanList, false, Strategy.init)
    } != null
    return result
}

/**保存实例数据, 实际就是保存工程数据
 * [fileName] 保存的工程文件名, 请包含后缀
 * [async] 是否异步保存
 * @return 返回保存的文件路径
 * */
fun CanvasRenderDelegate.saveProjectState(
    fileName: String = ".temp",
    async: Boolean = true
): String {
    val file = CanvasDataHandleOperate._defaultProjectOutputFile(fileName, false)
    val save = Runnable {
        val bean = getProjectBean()
        val json = bean.toJson()
        json.writeTo(file, false)
    }
    if (async) {
        doBack {
            save.run()
        }
    } else {
        //同步保存
        save.run()
    }
    return file.absolutePath
}

/**恢复实例数据, 可自定义线程加载
 * [saveProjectState]
 * @return 返回打开的工程文件路径
 * */
fun CanvasRenderDelegate.restoreProjectState(
    fileName: String = ".temp",
    async: Boolean = true
): String {
    val file = CanvasDataHandleOperate._defaultProjectOutputFile(fileName, false)
    val restore = Runnable {
        openProjectFile(file, true)
    }
    if (async) {
        doBack {
            restore.run()
        }
    } else {
        //同步保存
        restore.run()
    }
    return file.absolutePath
}

//endregion---Delegate---

//region---LpRenderer---

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

//endregion---LpRenderer---

//region---LPElementBean---

/**是否加粗*/
fun LPElementBean.isBold() = fontWeight == "bold"

/**是否斜体*/
fun LPElementBean.isItalic() = fontStyle == "italic"

//endregion---LPElementBean---