package fyi.meld.presto.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.camera.core.*
import androidx.camera.core.impl.ImageAnalysisConfig
import androidx.camera.core.impl.ImageReaderProxy
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
import fyi.meld.presto.utils.Constants
import fyi.meld.presto.viewmodels.PrestoViewModel
import kotlinx.android.synthetic.main.hint_bar.*
import kotlinx.android.synthetic.main.price_engine_fragment.*

/**
 * A simple [Fragment] subclass.
 * Use the [PriceEngineFragment.newInstance] factory method to
 * create an instance of this fragment.
 */

class PriceEngineFragment: Fragment() {

    lateinit var mCameraProviderFuture : ListenableFuture<ProcessCameraProvider>
    lateinit var mPreview : Preview
    lateinit var mCameraSelector : CameraSelector
    lateinit var mImageAnalysisUseCase : ImageAnalysis
    lateinit var mBoundingBoxView: BoundingBoxView

    private var prestoVM : PrestoViewModel = vita.with(VitaOwner.Multiple(this)).getViewModel<PrestoViewModel>()

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

        mCameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
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

    private fun configureScannerViews()
    {
        scanner_frame.visibility = View.VISIBLE
        mBoundingBoxView = BoundingBoxView(requireContext())
        mBoundingBoxView.setBoxDrawnHandler {
            handleBoxDrawn(it)
        }
        scanner_frame.addView(mBoundingBoxView)
    }

    private fun handleBoxDrawn(areaPriceBox: BoundingBox)
    {
        val dpFactor: Float = scanner_frame.resources.displayMetrics.density
        val width = (300 * dpFactor).toInt()
        val height = (100 * dpFactor).toInt()

        val layoutParams = FrameLayout.LayoutParams(width, height)
        layoutParams.leftMargin = areaPriceBox.location?.left!!.toInt()
        layoutParams.topMargin = areaPriceBox.location?.top!!.toInt() - price_tag_diag.height
        price_tag_diag.layoutParams = layoutParams
        scanner_frame.postInvalidate()

        price_tag_diag.visibility = View.VISIBLE
    }

    private fun cleanupScannerViews()
    {
        scanner_frame.visibility = View.INVISIBLE
        scanner_frame.removeView(mBoundingBoxView)
        val clear = Canvas()
        clear.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        mBoundingBoxView.draw(clear)
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
            ImageAnalysis.Analyzer {imageProxy ->

                prestoVM.priceEngine.classifyAsync(imageProxy, requireActivity().windowManager.defaultDisplay.rotation)
                    .addOnSuccessListener {
                        Log.d(Constants.TAG, it.toString())
                        if(it.size > 0)
                        {
                            hint_text.text = "Tap a price tag for more info"
                            mCameraProviderFuture.get().unbindAll()
                            mBoundingBoxView.setNewBoxes(it)
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
        scanner_frame.animate().
            alpha(1.0f).
            setDuration(Constants.CAMERA_PREVIEW_FADE_DURATION).
            withStartAction {
                configureScannerViews()
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
            }
            .start()
    }

    private fun stopCamera()
    {
        scanner_frame.animate().
            alpha(0.0f).
            setDuration(Constants.CAMERA_PREVIEW_FADE_DURATION / 5).
            withEndAction {
                cleanupScannerViews()
                scanner_frame.visibility = View.INVISIBLE

                mCameraProviderFuture.addListener(Runnable {
                    val cameraProvider = mCameraProviderFuture.get()
                    cameraProvider.unbindAll()
                }, ContextCompat.getMainExecutor(requireContext()))

                prestoVM.isCameraRunning = false
            }
            .start()
    }

    companion object {
        @JvmStatic
        fun newInstance() = PriceEngineFragment()
    }
}