package com.angcyo.canvas2.laser.pecker.dslitem.item

import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas2.laser.pecker.dialog.variableTextDialog
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasIconItem
import com.angcyo.canvas2.laser.pecker.util.LPElementHelper
import com.angcyo.canvas2.laser.pecker.util.lpTextElement
import com.angcyo.http.base.copyByJson
import com.angcyo.http.base.listType
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.laserpacker.bean.LPVariableBean
import com.angcyo.laserpacker.bean.initFileCacheIfNeed
import com.hingin.umeng.UMEvent
import com.hingin.umeng.umengEventValue

/**
 * 添加变量文本item
 *
 * [AddTextItem]
 * [AddVariableTextItem]
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/08/30
 */
class AddVariableTextItem : CanvasIconItem() {

    companion object {

        /**修改变量文本内容*/
        fun amendVariableText(delegate: CanvasRenderDelegate?, renderer: BaseRenderer) {
            delegate ?: return
            val element = renderer.lpTextElement() ?: return
            val bean = element.elementBean
            if (!bean.isVariableElement) {
                return
            }
            delegate.view.context?.variableTextDialog {
                mtype = bean.mtype
                variableTextBeanList =
                    bean.variables?.copyByJson(listType(LPVariableBean::class.java))
                        ?.toMutableList()
                        ?: mutableListOf()
                variableTextBeanList.initFileCacheIfNeed()

                onApplyVariableListAction = {
                    element.updateVariables(it, renderer, delegate)
                }
            }
        }
    }

    init {
        itemClick = {
            it.context.variableTextDialog {
                mtype = LPDataConstant.DATA_TYPE_VARIABLE_TEXT
                onApplyVariableListAction = {
                    LPElementHelper.addVariableTextElement(itemRenderDelegate, it, mtype)
                    UMEvent.CANVAS_VARIABLE_TEXT.umengEventValue()
                }
            }
        }
    }

}