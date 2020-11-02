package fyi.meld.presto.ui

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.androidisland.vita.VitaOwner
import com.androidisland.vita.vita
import com.google.android.material.transition.MaterialSharedAxis
import com.google.common.util.concurrent.ListenableFuture
import fyi.meld.presto.R
import fyi.meld.presto.models.BoundingBox
import fyi.meld.presto.models.CartItem
import fyi.meld.presto.utils.Constants
import fyi.meld.presto.utils.ItemType
import fyi.meld.presto.utils.PriceEngine
import fyi.meld.presto.viewmodels.PrestoViewModel
import kotlinx.android.synthetic.main.hint_bar.*
import kotlinx.android.synthetic.main.price_engine_fragment.*
import kotlinx.android.synthetic.main.price_tag_dialog.*
import java.io.File
import java.io.FileOutputStream


/**
 * A simple [Fragment] subclass.
 * Use the [PriceEngineFragment.newInstance] factory method to
 * create an instance of this fragment.
 */

class PriceEngineFragment: Fragment(), PriceEngine.DetectionStatusHandler, View.OnClickListener {

    private lateinit var mCameraProviderFuture : ListenableFuture<ProcessCameraProvider>
    private lateinit var mPreview : Preview
    private lateinit var mCameraSelector : CameraSelector
    private lateinit var mImageAnalysisUseCase : ImageAnalysis
    private lateinit var criticalInfoContainer : View
    private var newItem : CartItem? = null
    private var newItemBitmap: Bitmap? = null
    private lateinit var prestoVM : PrestoViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val backward = MaterialSharedAxis(MaterialSharedAxis.Y, true)
        val forward = MaterialSharedAxis(MaterialSharedAxis.Y, false)

        enterTransition = forward
        returnTransition = backward
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.price_engine_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prestoVM = vita.with(VitaOwner.Single(requireActivity())).getViewModel<PrestoViewModel>()

        add_to_cart_btn.setOnClickListener(this)
        mCameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        prestoVM.priceEngine.detectionStatusHandler = this
        criticalInfoContainer = requireActivity().findViewById(R.id.critical_info_container)
    }

    override fun onClick(v: View?) {
        when(v?.id)
        {
            R.id.add_to_cart_btn -> {
                if(newItem != null && newItemBitmap != null) {

                    newItem!!.photoUri = saveBitmap()

                    prestoVM.addToCart(newItem!!)

                    price_text.visibility = View.GONE
                    add_to_cart_btn.visibility = View.GONE
                    hint_text.visibility = View.INVISIBLE

                    add_success_image.visibility = View.VISIBLE
                    price_tag_hint_text.text = getString(R.string.add_item_success)

                    Handler().postDelayed({
                        onPriceLost()
                    }, (Constants.CAMERA_PREVIEW_FADE_DURATION * 1.5).toLong())
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if(!prestoVM.isCameraRunning)
        {
            startCamera()
        }
    }

    override fun onPause() {
        super.onPause()
        if(prestoVM.isCameraRunning)
        {
            stopCamera()
        }
    }

    private fun saveBitmap() : String
    {
        var filePath = ""
        val dest = File(requireActivity().filesDir, newItem?.uid.toString() + ".png")

        try {
            val out = FileOutputStream(dest)
            newItemBitmap?.compress(Bitmap.CompressFormat.PNG, 90, out)
            out.flush()
            out.close()
            filePath = dest.path
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return filePath
    }

    private fun showPriceDiag(detectedPrice: Pair<String, Bitmap>)
    {
        newItem = CartItem("", ItemType.Other, detectedPrice.first.toFloat())
        newItemBitmap = detectedPrice.second
        val priceAfterTax = String.format("$%.2f", newItem?.getPriceAfterTax(prestoVM.storeTrip.value?.localTaxRate!!))

        price_tag_diag.animate()
            .withStartAction {
                hint_text.text = getString(R.string.add_item_hint)

                val dpFactor: Float = scanner_frame.resources.displayMetrics.density
                val width = scanner_frame.width
                val height = (175 * dpFactor).toInt()

                val layoutParams = FrameLayout.LayoutParams(width, height)
                layoutParams.leftMargin = (scanner_frame.width / 2) - (width / 2)
                layoutParams.topMargin = (scanner_frame.height - criticalInfoContainer.height) - (height / 2)

                price_tag_diag.layoutParams = layoutParams
                price_detected_indicator.visibility = View.INVISIBLE

                price_tag_diag.visibility = View.VISIBLE
                price_text.text = priceAfterTax

            }
            .alpha(1.0f)
            .setDuration(Constants.CAMERA_PREVIEW_FADE_DURATION / 10)
    }

    private fun configureCamera() {

        val viewFinderDisplay =  view_finder.display

        mPreview = Preview.Builder().apply {
            setTargetRotation(viewFinderDisplay.rotation)
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
        }.build()

        view_finder.preferredImplementationMode = PreviewView.ImplementationMode.SURFACE_VIEW;

        mImageAnalysisUseCase = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetResolution(Size(480, 640))
            .build()

        mImageAnalysisUseCase.setAnalyzer(
            ContextCompat.getMainExecutor(requireContext()),
            ImageAnalysis.Analyzer { imageProxy ->

                val displayScaleFactor: Int = scanner_frame.resources.displayMetrics.densityDpi

                prestoVM.priceEngine.findPricesAsync(imageProxy, displayScaleFactor)
                    .addOnSuccessListener {
                        Log.d(Constants.TAG, it.first.toString())

                        requireActivity().runOnUiThread {

                            if (it.first.isNotBlank()) {

                                if (price_tag_diag.visibility == View.INVISIBLE || ((newItem != null && it.first != newItem?.name) && price_tag_diag.visibility == View.VISIBLE)) {
                                    showPriceDiag(it)
                                }
                            }
                        }
                    }
                    .addOnFailureListener { e -> Log.e(Constants.TAG, "Price Engine encountered an error.", e) }
            }
        )

        mCameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()
    }

    private fun startCamera()
    {
        scanner_frame.animate()
            .alpha(1.0f)
            .setDuration(Constants.CAMERA_PREVIEW_FADE_DURATION)
            .withStartAction {
                scanner_frame.visibility = View.VISIBLE

                configureCamera()

                mCameraProviderFuture.addListener(Runnable {
                    val cameraProvider = mCameraProviderFuture.get()

                    cameraProvider.bindToLifecycle(
                        this,
                        mCameraSelector,
                        mPreview,
                        mImageAnalysisUseCase
                    )

                    mPreview.setSurfaceProvider(view_finder.createSurfaceProvider())

                }, ContextCompat.getMainExecutor(requireContext()))

                prestoVM.isCameraRunning = true
            }.start()
    }

    private fun stopCamera()
    {
        scanner_frame.animate()
            .alpha(0.0f)
            .setDuration(Constants.CAMERA_PREVIEW_FADE_DURATION / 5)
            .withEndAction {
                scanner_frame.visibility = View.INVISIBLE

                mCameraProviderFuture.addListener(Runnable {
                    val cameraProvider = mCameraProviderFuture.get()
                    cameraProvider.unbindAll()
                }, ContextCompat.getMainExecutor(requireContext()))

                prestoVM.isCameraRunning = false
            }.start()
    }

    companion object {
        @JvmStatic
        fun newInstance() = PriceEngineFragment()
    }

    override fun onPriceLost() {
        requireActivity().runOnUiThread {
            if(price_tag_diag.visibility == View.VISIBLE)
            {
                price_tag_diag.animate()
                    .alpha(0.0f)
                    .setDuration(Constants.CAMERA_PREVIEW_FADE_DURATION / 10)
                    .withEndAction {
                        price_tag_diag.visibility = View.INVISIBLE
                        hint_text.text = getString(R.string.scanner_open_hint)

                        if(add_success_image.visibility == View.VISIBLE)
                        {
                            price_tag_hint_text.text = getString(R.string.after_tax)

                            price_text.visibility = View.VISIBLE
                            add_to_cart_btn.visibility = View.VISIBLE
                            hint_text.visibility = View.VISIBLE

                            add_success_image.visibility = View.GONE
                        }
                    }
            }
            else if(price_detected_indicator.visibility == View.VISIBLE)
            {
                price_detected_indicator.visibility = View.INVISIBLE
            }
            newItemBitmap?.let {
                it.recycle()
            }

            newItem = null
            newItemBitmap = null
        }
    }

    override fun onPriceFound(box: BoundingBox?) {
        requireActivity().runOnUiThread {
            if(price_tag_diag.visibility == View.INVISIBLE && price_detected_indicator.visibility == View.INVISIBLE)
            {
                var location = box?.location!!

                val layoutParams = FrameLayout.LayoutParams(price_detected_indicator.layoutParams)

                val scaledLeft = (location.left * scanner_frame.width).toInt()
                val scaledRight = (location.right * scanner_frame.width).toInt()
                val scaledTop = (location.top * scanner_frame.height).toInt()
                val scaledBottom = (location.bottom * scanner_frame.height).toInt()

                layoutParams.leftMargin = scaledRight - ((scaledRight - scaledLeft) / 2)
                layoutParams.topMargin = scaledBottom - ((scaledBottom - scaledTop) / 2)

                price_detected_indicator.layoutParams = layoutParams
                price_detected_indicator.visibility = View.VISIBLE
            }
        }
    }
}