package fyi.meld.presto.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.Log
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import fyi.meld.presto.R
import fyi.meld.presto.utils.BoundingBox
import fyi.meld.presto.utils.Constants


class BoundingBoxView(
    context: Context?
) :
    View(context) {
    private var newBoxes: List<BoundingBox>? = null
    private var drawnSuperBoxes = ArrayList<BoundingBox>()
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

    private fun drawNewBoxes(canvas: Canvas, newBoxes: List<BoundingBox>?) {
        if(newBoxes != null)
        {
            for (boundingBox in newBoxes) {
                val label = boundingBox.classIdentifier

                // Get bounding box coords
                var location: RectF = boundingBox.location!!
                location.left *= width.toFloat()
                location.top *= height.toFloat()
                location.right *= width.toFloat()
                location.bottom *= height.toFloat()

//                //Slightly enlarge the boxes because the neural net isn't perfect.
//                location.left -= location.left * 0.35f
//                location.right *= 1.25f

                if(label == "price")
                {
                    //Create a superbox to rule them all.
                    location.left -= location.left * 0.65f
                    location.top -= location.top * 0.1f

                    location.right *= 1.5f
                    location.bottom *= 1.20f

                    // Draw box
                    fgPaint.color = ContextCompat.getColor(context, R.color.accent)
                    fgPaint.style = Paint.Style.STROKE
                    canvas.drawRect(location, fgPaint)

                    drawnSuperBoxes.add(boundingBox);

                    // Draw label
                    val labelBounds = RectF()
                    val fm = fgPaint.fontMetrics
                    labelBounds.left = location.left
                    labelBounds.top =
                        location.top - (-fm.ascent + fm.descent + LABEL_VERTICAL_PADDING * 2)
                    labelBounds.right =
                        location.left + (fgPaint.measureText(label) + LABEL_HORIZONTAL_PADDING * 2)
                    labelBounds.bottom = location.top
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
            }
        }
    }

    public override fun onDraw(canvas: Canvas) {

        var correctedBoxes: List<BoundingBox>?

        synchronized(this) {
            correctedBoxes = this.newBoxes
        }

        if(newBoxes != null && drawnSuperBoxes!!.isNotEmpty() && newBoxes!!.isNotEmpty())
        {
            for(drawnBox in drawnSuperBoxes!!)
            {
                for(newBox in newBoxes!!)
                {
                    if( newBox.location?.left!! <= drawnBox.location?.left!! &&
                        newBox.location?.right!! >= drawnBox.location?.left!! &&
                        newBox.location?.top!! >= drawnBox.location?.bottom!! &&
                        newBox.location?.bottom!! <= drawnBox.location?.top!!)
                    {
                        Log.d(Constants.TAG, "Attempting to draw an overlapping box.")
                    }
                }
            }
        }

        drawNewBoxes(canvas, newBoxes)

    }

    companion object {
        private const val TEXT_SIZE_DIP = 16f
        private const val BOX_STROKE_WITDTH = 8f
        private const val LABEL_HORIZONTAL_PADDING = 16f
        private const val LABEL_VERTICAL_PADDING = 4f
    }
}
