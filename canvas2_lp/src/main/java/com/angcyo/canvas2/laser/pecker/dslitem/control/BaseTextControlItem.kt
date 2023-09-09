package com.angcyo.canvas2.laser.pecker.dslitem.control

import com.angcyo.canvas.render.data.TextProperty
import com.angcyo.canvas.render.element.TextElement
import com.angcyo.canvas.render.util.element
import com.angcyo.canvas2.laser.pecker.RenderControlHelper
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasIconItem
import com.angcyo.canvas2.laser.pecker.dslitem.ICanvasRendererItem
import com.angcyo.dsladapter.findItemByTag

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/13
 */
abstract class BaseTextControlItem : CanvasIconItem() {

    /**[com.angcyo.canvas.render.element.TextElement.updateTextProperty]*/
    protected fun updateTextProperty(
        keepVisibleSize: Boolean = false,
        block: TextProperty.() -> Unit
    ) {
        itemRenderer?.element<TextElement>()?.updateTextProperty(
            itemRenderer,
            itemRenderDelegate,
            keepVisibleSize,
            block = block
        )
        updateAdapterItem()
    }

    /**更新曲线文本item的可用性*/
    fun updateCurveTextItem() {
        itemRenderer?.element<TextElement>()?.let {
            itemDslAdapter?.findItemByTag(RenderControlHelper.TAG_CURVE_ITEM)?.let { item ->
                item.itemEnable = it.isSupportCurve
                item.updateAdapterItem()
            }
        }
    }

    /**初始化*/
    protected fun ICanvasRendererItem.initItem() {
        itemRenderer = this@BaseTextControlItem.itemRenderer
        itemRenderDelegate = this@BaseTextControlItem.itemRenderDelegate
    }

}