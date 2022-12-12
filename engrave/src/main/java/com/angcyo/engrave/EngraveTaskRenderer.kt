package com.angcyo.engrave

import android.graphics.*
import android.view.Gravity
import com.angcyo.canvas.CanvasDelegate
import com.angcyo.canvas.R
import com.angcyo.canvas.core.RenderParams
import com.angcyo.canvas.core.renderer.BaseRenderer
import com.angcyo.canvas.graphics.dataItemIndex
import com.angcyo.canvas.items.renderer.BaseItemRenderer
import com.angcyo.canvas.utils.createPaint
import com.angcyo.engrave.data.HawkEngraveKeys
import com.angcyo.library._refreshRateRatio
import com.angcyo.library.annotation.Pixel
import com.angcyo.library.ex.*
import com.angcyo.objectbox.laser.pecker.entity.EngraveTaskEntity

/**
 * 雕刻任务状态的渲染, 用来绘制当前雕刻的文件顺序, 以及当前雕刻的元素进度
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/12/12
 */
class EngraveTaskRenderer(val canvasDelegate: CanvasDelegate) : BaseRenderer(canvasDelegate) {

    companion object {

        /**安装*/
        fun install(canvasDelegate: CanvasDelegate): EngraveTaskRenderer {
            val renderer = EngraveTaskRenderer(canvasDelegate)
            canvasDelegate.rendererLastList.add(renderer)
            return renderer
        }
    }

    /**画笔*/
    private val paint = createPaint().apply {
        //init
        textSize = 9 * dp
        strokeWidth = 1 * dp
    }

    /**边框的颜色*/
    var borderColor: Int = EngraveProductLayoutHelper.ENGRAVE_COLOR

    /**文本的颜色*/
    var textColor: Int = _color(R.color.canvas_progress_text_color, canvasDelegate.view.context)

    /**进度的颜色*/
    var progressColor: Int = _color(R.color.canvas_progress_color, canvasDelegate.view.context)

    /**指定任务的id, 后续会自动查询*/
    var engraveTaskId: String? = null

    /**需要文本的绘制偏移量*/
    @Pixel
    var serialNumberOffset = 1 * dp

    /**边框向外插入的像素大小*/
    @Pixel
    var borderInset = -1 * dp

    override fun render(canvas: Canvas, renderParams: RenderParams) {
        if (!HawkEngraveKeys.enableRenderEngraveInfo) return
        val taskId = engraveTaskId ?: return
        val engraveTask = EngraveFlowDataHelper.getEngraveTask(taskId) ?: return
        val indexList = engraveTask.dataIndexList
        if (indexList.size() <= 0) {
            return
        }
        //
        val rendererList = canvasDelegate.getRendererItem(indexList)
        if (rendererList.size() <= 0) {
            //nop op
        } else if (rendererList.size() <= 1) {
            //只有1个元素, 则只绘制进度
            renderRender(canvas, rendererList.first(), -1, engraveTask)
        } else {
            //多个元素绘制, 绘制顺序和进度
            rendererList.forEachIndexed { index, itemRenderer ->
                renderRender(canvas, itemRenderer, index + 1, engraveTask)
            }
        }
    }

    /**绘制一个渲染器应该有的数据
     * 包括:边框, 进度, 索引序号
     * [serialNumber] 索引序号, 小于0时, 不绘制
     * */
    fun renderRender(
        canvas: Canvas,
        renderer: BaseItemRenderer<*>,
        serialNumber: Int,
        engraveTask: EngraveTaskEntity
    ) {
        //1:先渲染元素的边框
        _renderBorder(canvas, renderer, engraveTask)
        //2:再渲染元素的进度
        _renderProgress(canvas, renderer, engraveTask)
        //3:再渲染元素的序号
        if (serialNumber >= 0) {
            val visualBounds = renderer.getVisualRotateBounds().adjustFlipRect(_tempRect)
            _drawText(canvas, "$serialNumber", visualBounds, Gravity.LEFT or Gravity.TOP)
        }
    }

    //---

    private val _tempRect = RectF()
    private val _tempPath = Path()

    /**蚂蚁线间隔*/
    private var intervals = floatArrayOf(5 * dp, 5 * dp)

    /**偏移距离*/
    private var phase = 0f

    /**
     * 正数是逆时针动画
     * 负数是顺时针动画
     * */
    private var phaseStep = -2

    /**绘制指定渲染器的边框*/
    fun _renderBorder(
        canvas: Canvas,
        renderer: BaseItemRenderer<*>,
        engraveTask: EngraveTaskEntity
    ) {
        val dataItemIndex = renderer.dataItemIndex
        val bounds = renderer.getVisualRotateBounds().adjustFlipRect(_tempRect)

        bounds.inset(borderInset, borderInset)
        _tempPath.rewind()
        _tempPath.addRect(bounds, Path.Direction.CW)

        if (engraveTask.currentIndex == dataItemIndex) {
            //正在雕刻的索引, 使用虚线动画绘制
            _drawBorder(canvas, _tempPath, DashPathEffect(intervals, phase))
            //动画
            phase += phaseStep / _refreshRateRatio
            if (phaseStep < 0 && phase < -intervals.sum()) {
                phase = 0f
            } else if (phaseStep > 0 && phase > intervals.sum()) {
                phase = 0f
            }
            refresh()
        } else {
            //已完成的索引使用实线, 未完成的使用虚线.
            val isBefore = engraveTask.dataIndexList?.isElementBeforeWith(
                "$dataItemIndex",
                "${engraveTask.currentIndex}",
                false
            ) ?: false
            _drawBorder(canvas, _tempPath, if (isBefore) null else DashPathEffect(intervals, 0f))
        }
    }

    /**绘制边框*/
    fun _drawBorder(canvas: Canvas, path: Path, effect: PathEffect?) {
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.color = borderColor //EngraveProductLayoutHelper.ENGRAVE_COLOR
        paint.pathEffect = effect
        canvas.drawPath(path, paint)
    }

    /**绘制指定渲染器的进度
     * [renderer] 在这个渲染器上绘制进度
     * [index] 数据索引, 进度通过这个索引自动查询*/
    fun _renderProgress(
        canvas: Canvas,
        renderer: BaseItemRenderer<*>,
        engraveTask: EngraveTaskEntity
    ) {
        val index = renderer.dataItemIndex ?: return
        val progress =
            EngraveFlowDataHelper.getEngraveDataEntity(engraveTask.taskId, index)?.progress
                ?: return
        val visualBounds = renderer.getVisualRotateBounds().adjustFlipRect(_tempRect)

        //进度背景

        //进度文本
        if (progress < 100) {
            _drawText(
                canvas,
                "${progress}%",
                visualBounds,
                Gravity.CENTER,
                Paint.Style.FILL_AND_STROKE
            )
        }
    }

    /**在指定的[rect]内部绘制文本
     * [gravity] 重力*/
    fun _drawText(
        canvas: Canvas,
        text: String,
        rect: RectF,
        gravity: Int = Gravity.CENTER,
        style: Paint.Style = Paint.Style.FILL
    ) {
        paint.shader = null
        paint.color = textColor
        paint.style = style

        val textWidth = paint.textWidth(text)
        val textHeight = paint.textHeight()

        //默认居中绘制
        var anchorX = rect.centerX()
        var anchorY = rect.centerY()

        if (gravity == Gravity.LEFT or Gravity.TOP) {
            //左上角
            anchorX = serialNumberOffset + rect.left + textWidth / 2
            anchorY = serialNumberOffset + rect.top + textHeight / 2
        }

        val x = anchorX - textWidth / 2
        val y = anchorY + textHeight / 2 - paint.descent()

        canvas.drawText(text, x, y, paint)
    }
}