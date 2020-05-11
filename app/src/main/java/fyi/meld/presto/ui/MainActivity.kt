package fyi.meld.presto.ui

import SpacesItemDecoration
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.media.Image
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.drawToBitmap
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.androidisland.vita.VitaOwner
import com.androidisland.vita.vita
import com.google.common.util.concurrent.ListenableFuture
import fyi.meld.presto.R
import fyi.meld.presto.utils.Constants
import fyi.meld.presto.utils.PriceEngine
import fyi.meld.presto.viewmodels.PrestoViewModel
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.critical_info.*
import kotlinx.android.synthetic.main.hint_bar.*
import java.lang.ref.WeakReference
import java.nio.ByteBuffer


class MainActivity : AppCompatActivity(), View.OnClickListener, LifecycleOwner, MotionLayout.TransitionListener {

    lateinit var prestoVM : PrestoViewModel
    lateinit var mCartItemAdapter: CartItemAdapter
    lateinit var mCameraProviderFuture : ListenableFuture<ProcessCameraProvider>
    lateinit var mPreview : Preview
    lateinit var mCameraSelector : CameraSelector
    lateinit var mImageAnalysisUseCase : ImageAnalysis
    lateinit var mBoundingBoxView: BoundingBoxView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        base_container.setTransitionListener(this)
        new_item_btn.setOnClickListener(this)

        if(!hasPermissions())
        {
            requestPermission()
        }

        configureViewModel()

        setupCartItemsView()

        prestoVM.priceEngine.initialize()

        mCameraProviderFuture = ProcessCameraProvider.getInstance(this);
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.new_item_btn -> openNewItemActivity()
        }
    }

    override fun onBackPressed() {

        if(prestoVM.isCameraRunning)
        {
            base_container.transitionToStart()
        }
    }

    private fun setupCartItemsView()
    {
        cart_items_view.layoutManager =
            LinearLayoutManager(this)

        val decoration = SpacesItemDecoration(16)
        mCartItemAdapter = CartItemAdapter(
            this,
            WeakReference(prestoVM.storeTrip.value!!)
        )

        cart_items_view.adapter = mCartItemAdapter
        cart_items_view.addItemDecoration(decoration)
    }

    private fun configureScannerViews()
    {
        scanner_frame.visibility = VISIBLE
        mBoundingBoxView = BoundingBoxView(this)
        scanner_frame.addView(mBoundingBoxView)
    }

    private fun cleanupScannerViews()
    {
        scanner_frame.visibility = INVISIBLE
        scanner_frame.removeView(mBoundingBoxView)
        var clear = Canvas()
        clear.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        mBoundingBoxView.draw(clear)
    }

    private fun configureViewModel()
    {
        prestoVM = vita.with(VitaOwner.Multiple(this)).getViewModel<PrestoViewModel>()
        prestoVM.initialLayoutState = base_container.currentState
        prestoVM.priceEngine = PriceEngine(WeakReference(this))

        configureDataObservers()
    }

    private fun configureDataObservers()
    {
        prestoVM.storeTrip.observe(this, Observer {
            cart_size_text.text = it.items.size.toString()
            cart_total_text.text = "$" + String.format("%.2f", it.getTotalAfterTax())
            tax_rate_text.text = String.format("%.2f", it.localTaxRate) + "%"
        })
    }

    private fun configureCamera() {

        val viewFinderDisplay =  view_finder.display

        mPreview = Preview.Builder().apply {
            setTargetRotation(viewFinderDisplay.rotation)
            setTargetAspectRatio(AspectRatio.RATIO_4_3)
        }.build()

        view_finder.preferredImplementationMode = PreviewView.ImplementationMode.SURFACE_VIEW;

        mImageAnalysisUseCase = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .build()

        mImageAnalysisUseCase.setAnalyzer(ContextCompat.getMainExecutor(this),
            ImageAnalysis.Analyzer {imageProxy ->

                prestoVM.priceEngine.classifyAsync(imageProxy, windowManager.defaultDisplay.rotation)
                    .addOnSuccessListener {
                        Log.d(Constants.TAG, it.toString())
//                        var numPrices = 0;
//
//                        for(box in it)
//                        {
//                            if(box.classIdentifier == "price")
//                            {
//                                numPrices++
//                            }
//                        }
//
//                        if(numPrices > 0)
//                        {
//                            hint_text.text = "Found " + numPrices + " price(s)"
//                        }
//                        else
//                        {
//                            hint_text.text = "Hover over an item and its price"
//                        }
//
                        mBoundingBoxView.setNewBoxes(it)
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

                    val camera = cameraProvider.bindToLifecycle(
                        this,
                        mCameraSelector,
                        mPreview,
                        mImageAnalysisUseCase
                    )

                    mPreview.setSurfaceProvider(view_finder.createSurfaceProvider(camera.cameraInfo))

                }, ContextCompat.getMainExecutor(this))

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
                scanner_frame.visibility = INVISIBLE

                mCameraProviderFuture.addListener(Runnable {
                    val cameraProvider = mCameraProviderFuture.get()
                    cameraProvider.unbindAll()
                }, ContextCompat.getMainExecutor(this))

                prestoVM.isCameraRunning = false
            }
            .start()
    }

    private fun openNewItemActivity()
    {
        val newItemIntent = Intent(this, NewItemActivity::class.java)
        startActivityForResult(newItemIntent, Constants.NEW_ITEM_REQUEST_CODE)
    }

    private fun hasPermissions(): Boolean{
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }


    private fun requestPermission(){
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA),
            Constants.PERMISSIONS_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == Constants.PERMISSIONS_REQUEST_CODE) {
            if (!hasPermissions()) {
                Toast.makeText(this,
                    "Please allow the app camera permissions.",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if(requestCode == Constants.NEW_ITEM_REQUEST_CODE && resultCode == Activity.RESULT_OK)
        {
            mCartItemAdapter.notifyDataSetChanged()

            if(prestoVM.storeTrip.value!!.items.isNotEmpty() && empty_cart_text.visibility == VISIBLE)
            {
                empty_cart_text.visibility = INVISIBLE
            }
            else if(prestoVM.storeTrip.value!!.items.isEmpty())
            {
                empty_cart_text.visibility = VISIBLE
            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onTransitionTrigger(p0: MotionLayout?, p1: Int, p2: Boolean, p3: Float) {
    }

    override fun onTransitionStarted(motionLayout: MotionLayout?, startingState: Int, endingState: Int) {

    }

    override fun onTransitionChange(p0: MotionLayout?, p1: Int, p2: Int, p3: Float) {
    }

    override fun onTransitionCompleted(motionLayout: MotionLayout?, currentState: Int) {

        if(currentState != prestoVM.initialLayoutState && !prestoVM.isCameraRunning)
        {
            startCamera()
        }
        else if(currentState == prestoVM.initialLayoutState && prestoVM.isCameraRunning)
        {
            stopCamera()
        }
    }
}
