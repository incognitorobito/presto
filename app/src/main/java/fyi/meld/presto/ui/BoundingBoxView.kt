package fyi.meld.presto.ui

import android.content.Context
import android.graphics.*
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import fyi.meld.presto.R
import fyi.meld.presto.models.BoundingBox


class BoundingBoxView(
    context: Context?
) :
    View(context) {
    private var newBoxes: List<BoundingBox>? = null
    private lateinit var boxDrawnHandler: (BoundingBox) -> Unit
    private var areaPriceBox: BoundingBox? = null
    private val fgPaint: Paint

    init {

        val textSizePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            TEXT_SIZE_DIP,
            resources.displayMetrics
        )

        fgPaint = Paint()
        fgPaint.textSize = textSizePx
        fgPaint.strokeWidth = BOX_STROKE_WITDTH
    }

    fun setNewBoxes(newBoxes: List<BoundingBox>) {
        synchronized(this) { this.newBoxes = newBoxes }
        postInvalidate()
    }

    public fun setBoxDrawnHandler(handler: (BoundingBox) -> Unit)
    {
        boxDrawnHandler = handler
    }

    private fun drawLabels(canvas: Canvas, newBoxLocat: RectF, label: String)
    {
        // Draw label
        val labelBounds = RectF()
        val fm = fgPaint.fontMetrics
        labelBounds.left = newBoxLocat.left
        labelBounds.top =
            newBoxLocat.top - (-fm.ascent + fm.descent + LABEL_VERTICAL_PADDING * 2)
        labelBounds.right =
            newBoxLocat.left + (fgPaint.measureText(label) + LABEL_HORIZONTAL_PADDING * 2)
        labelBounds.bottom = newBoxLocat.top
        fgPaint.style = Paint.Style.FILL_AND_STROKE
        canvas.drawRect(labelBounds, fgPaint)
        fgPaint.color = Color.WHITE
        fgPaint.style = Paint.Style.FILL
        canvas.drawText(
            label!!,
            labelBounds.left + LABEL_HORIZONTAL_PADDING,
            labelBounds.bottom - (fm.descent + LABEL_VERTICAL_PADDING),
            fgPaint
        )
    }

    private fun drawBox(canvas: Canvas, newBoxLocat: RectF)
    {
        fgPaint.color = ContextCompat.getColor(context, R.color.accent)
        fgPaint.style = Paint.Style.STROKE
        canvas.drawRect(newBoxLocat, fgPaint)
    }

    private fun handleNewBoxes(canvas: Canvas, newBoxes: List<BoundingBox>?) {
        if(newBoxes != null)
        {
            for (boundingBox in newBoxes) {
                val label = boundingBox.classIdentifier
                var location: RectF = boundingBox.location!!
                location.left *= width.toFloat()
                location.top *= height.toFloat()
                location.right *= width.toFloat()
                location.bottom *= height.toFloat()

                if(label == "price")
                {
                    location.left -= location.left * 0.65f
                    location.top -= location.top * 0.1f
                    location.right *= 1.25f
                    location.bottom *= 1.20f
                    areaPriceBox = boundingBox.copy()
                    break
                }
            }

            drawBox(canvas, areaPriceBox!!.location!!)
            drawLabels(canvas, areaPriceBox!!.location!!, areaPriceBox!!.classIdentifier!!)
            boxDrawnHandler(areaPriceBox!!)
        }
    }

    public override fun onDraw(canvas: Canvas) {

        var boxes: List<BoundingBox>?

        synchronized(this) {
            boxes = this.newBoxes
        }

        handleNewBoxes(canvas, boxes)
    }

    companion object {
        private const val TEXT_SIZE_DIP = 16f
        private const val BOX_STROKE_WITDTH = 8f
        private const val LABEL_HORIZONTAL_PADDING = 16f
        private const val LABEL_VERTICAL_PADDING = 4f
    }
}
