package fyi.meld.presto

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.*
import android.widget.Toast
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import fyi.meld.presto.model.Constants
import fyi.meld.presto.model.StoreTrip
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity(), View.OnClickListener, LifecycleOwner, MotionLayout.TransitionListener {

    var mCurrentTrip = StoreTrip()
    var mIsCameraRunning = false;
    var mInitialLayoutState : Int = -1;
    var mLastAnticipatedLayoutState : Int = -1;

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

        mCameraProviderFuture = ProcessCameraProvider.getInstance(this);
        mInitialLayoutState = base_container.currentState
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.new_item_btn -> openNewItemActivity()
        }
    }

    override fun onBackPressed() {

        if(mIsCameraRunning)
        {
            base_container.transitionToStart()
        }

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

                configureCamera()

                mCameraProviderFuture.addListener(Runnable {
                    val cameraProvider = mCameraProviderFuture.get()

                    cameraProvider.bindToLifecycle(
                        this,
                        mCameraSelector,
                        mPreview
                    )
                }, ContextCompat.getMainExecutor(this))

                mIsCameraRunning = true
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

                mIsCameraRunning = false
            }
            .start()
    }

    private fun openNewItemActivity()
    {
        val newItemIntent = Intent(this, NewItemActivity::class.java)
        startActivity(newItemIntent)
    }

    private fun hasPermissions(): Boolean{
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }


    private fun requestPermission(){
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA),Constants.PERMISSIONS_REQUEST_CODE)
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

    override fun onTransitionTrigger(p0: MotionLayout?, p1: Int, p2: Boolean, p3: Float) {
    }

    override fun onTransitionStarted(motionLayout: MotionLayout?, startingState: Int, endingState: Int) {

    }

    override fun onTransitionChange(p0: MotionLayout?, p1: Int, p2: Int, p3: Float) {
    }

    override fun onTransitionCompleted(motionLayout: MotionLayout?, currentState: Int) {

        if(currentState != mInitialLayoutState && !mIsCameraRunning)
        {
            startCamera()
        }
        else if(currentState == mInitialLayoutState && mIsCameraRunning)
        {
            stopCamera()
        }
    }


}
