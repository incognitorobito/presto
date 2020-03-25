package fyi.meld.presto.ui

import SpacesItemDecoration
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.View.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.androidisland.vita.VitaOwner
import com.androidisland.vita.vita
import com.google.common.util.concurrent.ListenableFuture
import fyi.meld.presto.R
import fyi.meld.presto.utils.Constants
import fyi.meld.presto.viewmodels.PrestoViewModel
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.critical_info.*
import kotlinx.android.synthetic.main.hint_bar.*
import java.lang.ref.WeakReference


class MainActivity : AppCompatActivity(), View.OnClickListener, LifecycleOwner, MotionLayout.TransitionListener {

    lateinit var prestoVM : PrestoViewModel
    lateinit var mCartItemAdapter: CartItemAdapter
    lateinit var mCameraProviderFuture : ListenableFuture<ProcessCameraProvider>
    lateinit var mPreview : Preview
    lateinit var mCameraSelector : CameraSelector

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

        cart_items_view.setLayoutManager(
            StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        )

        val decoration = SpacesItemDecoration(16)
        mCartItemAdapter = CartItemAdapter(
            this,
            WeakReference(prestoVM.storeTrip.value!!)
        )

        cart_items_view.adapter = mCartItemAdapter
        cart_items_view.addItemDecoration(decoration)

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

    private fun configureViewModel()
    {
        prestoVM = vita.with(VitaOwner.Multiple(this)).getViewModel<PrestoViewModel>()
        prestoVM.initialLayoutState = base_container.currentState

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
        // Build the viewfinder use case
        mPreview = Preview.Builder().apply {
            setTargetRotation(viewFinderDisplay.rotation)
            setTargetAspectRatio(AspectRatio.RATIO_4_3)
        }.build()

        // Every time the viewfinder is updated, recompute layout
        mPreview.setSurfaceProvider(view_finder.previewSurfaceProvider)

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .build()

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this),
            ImageAnalysis.Analyzer {imageProxy -> //DO image manipulation here
            })

        mCameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()
    }

    private fun startCamera()
    {
        view_finder.animate().
            alpha(1.0f).
            setDuration(Constants.CAMERA_PREVIEW_FADE_DURATION).
            withStartAction {
                view_finder.visibility = VISIBLE
                hint_text.text = "Hover over an item and its price"
                configureCamera()

                mCameraProviderFuture.addListener(Runnable {
                    val cameraProvider = mCameraProviderFuture.get()

                    cameraProvider.bindToLifecycle(
                        this,
                        mCameraSelector,
                        mPreview
                    )
                }, ContextCompat.getMainExecutor(this))

                prestoVM.isCameraRunning = true
            }
            .start()
    }

    private fun stopCamera()
    {
        view_finder.animate().
            alpha(0.0f).
            setDuration(Constants.CAMERA_PREVIEW_FADE_DURATION / 5).
            withEndAction {
                view_finder.visibility = INVISIBLE

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
