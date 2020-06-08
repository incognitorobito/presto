package fyi.meld.presto.utils

import ai.customvision.CustomVisionManager
import ai.customvision.tflite.ObjectDetector
import ai.customvision.visionskills.CVSObjectDetector
import android.content.Context
import android.graphics.*
import android.util.Log
import android.view.Surface
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks.call
import fyi.meld.presto.models.BoundingBox
import java.io.ByteArrayOutputStream
import java.lang.ref.WeakReference
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.collections.ArrayList


class PriceEngine(private val context : WeakReference<Context>) {

    private lateinit var detector: ObjectDetector
    private var availableLabels = arrayListOf<String>()
    private var isInitialized = false

    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()

    fun initialize() {
        CustomVisionManager.setAppContext(context.get())

        val config : CVSObjectDetector.Configuration = ObjectDetector.ConfigurationBuilder()
            .setModelFile("cvexport.manifest")
            .build()

        availableLabels = config.SupportedIdentifiers.stringVector.toList() as ArrayList<String>
        detector = ObjectDetector(config)

        isInitialized = true
    }

    private fun previewToBitmap(imageProxy : ImageProxy, deviceRotation: Int): Bitmap {
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

        when(deviceRotation)
        {
            Surface.ROTATION_0 -> rotationMatrix.postRotate(90F)
            Surface.ROTATION_90 -> rotationMatrix.postRotate(-90F)
            Surface.ROTATION_180 -> rotationMatrix.postRotate(180F)
            Surface.ROTATION_270 -> rotationMatrix.postRotate(270F)
        }

        return Bitmap.createBitmap(convertedBitmap, 0, 0, convertedBitmap.width, convertedBitmap.height, rotationMatrix, true)
    }

    private fun classify(sourceImage: ImageProxy, deviceRotation : Int) {

        check(isInitialized) { "Price Engine has not yet been initialized." }

        var sourceBitmap = previewToBitmap(sourceImage, deviceRotation)

        detector.setImage(sourceBitmap)
        detector.run()

        sourceBitmap.recycle()
        sourceImage.close()
    }

    private fun getBoundingBoxes() : ArrayList<BoundingBox>
    {
        var results = ArrayList<BoundingBox>()
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
                results.add(
                    BoundingBox(
                        classIndex,
                        label,
                        confidence,
                        location
                    )
                )
            }
        }

        return results
    }

    fun classifyAsync(imageProxy: ImageProxy, orientation : Int): Task<ArrayList<BoundingBox>> {
        return call(executorService, Callable<ArrayList<BoundingBox>> {
            classify(imageProxy, orientation)
            Log.d(Constants.TAG, String.format("Detector ran in: %.0f", detector.TimeInMilliseconds.getFloat()));
            return@Callable getBoundingBoxes()
        })
    }
}