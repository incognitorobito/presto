package fyi.meld.presto.utils

import ai.customvision.CustomVisionManager
import ai.customvision.tflite.ObjectDetector
import ai.customvision.visionskills.CVSObjectDetector
import android.content.Context
import android.graphics.*
import android.util.Log
import android.util.SparseArray
import androidx.camera.core.ImageProxy
import androidx.core.graphics.get
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks.call
import com.google.android.gms.vision.text.TextBlock
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import fyi.meld.presto.models.BoundingBox
import java.io.ByteArrayOutputStream
import java.lang.IllegalArgumentException
import java.lang.Math.abs
import java.lang.ref.WeakReference
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


private const val MAX_EMPTY_OR_LOW_CONFIDENCE_FRAMES = 2
private const val MIN_CONFIDENCE_THRESHOLD = 0.50f

class PriceEngine(private val context : WeakReference<Context>) {

    var detectionStatusHandler : DetectionStatusHandler? = null

    private lateinit var detector: ObjectDetector
    private lateinit var recognizer: TextRecognizer;
    private lateinit var availableLabels : List<String>
    private var isInitialized = false
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()
    private var lowOrNoDetectionFrames = 0

    fun initialize() {
        CustomVisionManager.setAppContext(context.get())

        val config : CVSObjectDetector.Configuration = ObjectDetector.ConfigurationBuilder()
            .setModelFile("cvexport.manifest")
            .build()

        availableLabels = config.SupportedIdentifiers.stringVector.asList()
        detector = ObjectDetector(config)

        recognizer = TextRecognition.getClient()

        isInitialized = true
    }

    fun shutdown() {
        detector.close()
        recognizer.close()
    }

    private fun previewToBitmap(imageProxy : ImageProxy, imageRotation: Int): Bitmap {
        val yBuffer = imageProxy.planes[0].buffer // Y
        val uBuffer = imageProxy.planes[1].buffer // U
        val vBuffer = imageProxy.planes[2].buffer // V

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        // U and V are swapped
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 100, out)
        val imageBytes = out.toByteArray()

        var ops = BitmapFactory.Options()
        ops.inPreferredConfig = Bitmap.Config.ARGB_8888

        val convertedBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, ops)

        var rotationMatrix = Matrix()

        rotationMatrix.postRotate(imageRotation.toFloat())

        return Bitmap.createBitmap(convertedBitmap, 0, 0, convertedBitmap.width, convertedBitmap.height, rotationMatrix, true)
    }

    private fun findPrice(sourceImage: ImageProxy, displayScaleFactor: Int) : Pair<Bitmap?, BoundingBox?> {

        check(isInitialized) { "Price Engine has not yet been initialized." }

        var sourceBitmap = previewToBitmap(sourceImage, sourceImage.imageInfo.rotationDegrees)
        var croppedBitmap : Bitmap? = null

        detector.setImage(sourceBitmap)
        detector.run()

        Log.d(Constants.TAG, String.format("Detector ran in: %.0f", detector.TimeInMilliseconds.getFloat()));

        val firstBox = findLargestBoundingBox(sourceBitmap)

        if(firstBox != null)
        {
            val location = firstBox.location!!

            val scaledLeft = (location.left * sourceBitmap.getScaledWidth(displayScaleFactor)).toInt()
            val scaledRight = (location.right * sourceBitmap.getScaledWidth(displayScaleFactor)).toInt()
            val scaledTop = (location.top * sourceBitmap.getScaledHeight(displayScaleFactor)).toInt()
            val scaledBottom = (location.bottom * sourceBitmap.getScaledHeight(displayScaleFactor)).toInt()

            try {
                croppedBitmap = Bitmap.createBitmap(sourceBitmap,
                    scaledLeft,
                    scaledTop,
                    scaledRight - scaledLeft,
                    scaledBottom - scaledTop
                )
            }
            catch(e: IllegalArgumentException)
            {
                Log.e(Constants.TAG, "Cannot properly crop the bitmap for this particular price.")
            }
        }

        sourceBitmap.recycle()
        sourceImage.close()

        return Pair(croppedBitmap, firstBox)
    }

    private fun getPriceText(result: Text?) : String
    {
        var foundPrice: String = ""

        if(result != null)
        {
            val resultText = result.text
            for (block in result.textBlocks) {
                val blockText = block.text
                val blockCornerPoints = block.cornerPoints
                val blockFrame = block.boundingBox
                for (line in block.lines) {
                    val lineText = line.text
                    val lineCornerPoints = line.cornerPoints
                    val lineFrame = line.boundingBox
                    for (element in line.elements) {
                        val elementText = element.text

                        var potentialPrice = if (elementText.startsWith('$')) elementText.drop(1) else elementText

                        if(potentialPrice.matches(Regex(Constants.US_CURRENCY_REGEX)))
                        {
                            foundPrice = potentialPrice
                            Log.d(Constants.TAG, elementText)
                        }
                    }
                }
            }
        }
        return foundPrice
    }

    private fun findLargestBoundingBox(sourceBitmap: Bitmap) : BoundingBox?
    {
        var largestBox: BoundingBox? = null
        val labels: Array<String> = detector.Identifiers.getStringVector()

        if (labels.size != 0) {
            val indexes: IntArray = detector.IdentifierIndexes.getIntVector()
            val confidences: FloatArray = detector.Confidences.getFloatVector()
            val boundingBoxes: Array<RectF> =
                detector.BoundingBoxes.getRectangleVector()
            for (i in confidences.indices) {
                val label = labels[i]
                val confidence = confidences[i]
                val location = boundingBoxes[i]
                val classIndex = indexes[i]

                if(label == "price" && confidence >= MIN_CONFIDENCE_THRESHOLD)
                {
//                    Enlarge the bounding box to further ensure the contents will be picked up by OCR.
                    location.left -= location.left * 0.45f
                    location.top -= location.top * 0.05f
                    location.right *= 1.15f
                    location.bottom *= 1.05f

                    val box = BoundingBox(classIndex, label, confidence, location)

                    if(largestBox == null || (largestBox.getArea() <= box.getArea()))
                    {
                        largestBox = box
                    }
                }
            }
        }

        if(largestBox == null)
        {
            lowOrNoDetectionFrames +=1
        }

        if(lowOrNoDetectionFrames >= MAX_EMPTY_OR_LOW_CONFIDENCE_FRAMES)
        {
            detectionStatusHandler?.onPriceLost()
            lowOrNoDetectionFrames = 0
        }

        return largestBox
    }

    fun findPricesAsync(sourceImage: ImageProxy, displayScaleFactor: Int): Task<String> {

        var croppedImage : Bitmap? = null
        var boundingBox : BoundingBox? = null

        return call(executorService, Callable<Pair<Bitmap?, BoundingBox?>> {
            return@Callable findPrice(sourceImage, displayScaleFactor)
        }).continueWithTask { findPriceTask ->

            var priceRecoTask : Task<Text?> = call(Callable<Text?> { return@Callable null })

            croppedImage = findPriceTask.result.first
            boundingBox = findPriceTask.result.second

            if(croppedImage != null)
            {
                detectionStatusHandler?.onPriceFound(boundingBox)
                val image = InputImage.fromBitmap(croppedImage!!, 0)
                priceRecoTask = recognizer.process(image)
            }
            else
            {
                Log.d(Constants.TAG, "No prices found in image.")
            }

            return@continueWithTask priceRecoTask

        }.continueWith { findPriceTextTask ->

            croppedImage?.recycle()

            var foundPrice = ""

            if(findPriceTextTask.isSuccessful)
            {
                foundPrice = getPriceText(findPriceTextTask.result)
            }
            else
            {
                Log.e(Constants.TAG, "An error occurred while attempting to find text in a price tag.", findPriceTextTask.exception)
            }

            return@continueWith foundPrice
        }
    }

    interface DetectionStatusHandler
    {
        fun onPriceLost()
        fun onPriceFound(box: BoundingBox?)
    }
}