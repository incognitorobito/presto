package fyi.meld.presto

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.AttributeSet
import android.util.Size
import android.view.Display
import android.view.View
import android.view.View.*
import android.widget.Toast
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import fyi.meld.presto.model.Constants
import fyi.meld.presto.model.StoreTrip
import kotlinx.android.synthetic.main.activity_main.*



class MainActivity : AppCompatActivity(), View.OnClickListener, LifecycleOwner {

    var mCurrentTrip = StoreTrip()
    var mIsCameraActive = false;
    private lateinit var mCameraProviderFuture : ListenableFuture<ProcessCameraProvider>
    private lateinit var mPreview : Preview
    private lateinit var mCameraSelector : CameraSelector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        new_item_btn.setOnClickListener(this)
        swipe_handler_btn.setOnClickListener(this)

        if(!hasPermissions())
        {
            requestPermission()
        }

        mCameraProviderFuture = ProcessCameraProvider.getInstance(this);
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.new_item_btn -> openNewItemActivity()
            R.id.swipe_handler_btn -> toggleCamera()
        }
    }

    override fun onBackPressed() {

        if(mIsCameraActive)
        {
            toggleCamera()
        }

    }

    private fun toggleCamera()
    {
        if(mIsCameraActive == true)
        {
            stopCamera()
        }
        else
        {
            startCamera()
        }

        mIsCameraActive = !mIsCameraActive

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
        configureCamera()

        base_vertical_container.visibility = INVISIBLE
        view_finder.visibility = VISIBLE

        mCameraProviderFuture.addListener(Runnable {
            val cameraProvider = mCameraProviderFuture.get()

            cameraProvider.bindToLifecycle(
                this,
                mCameraSelector,
                mPreview
            )
        }, ContextCompat.getMainExecutor(this))
    }

    private fun stopCamera()
    {
        base_vertical_container.visibility = VISIBLE
        view_finder.visibility = INVISIBLE

        mCameraProviderFuture.addListener(Runnable {
            val cameraProvider = mCameraProviderFuture.get()
            cameraProvider.unbindAll()
        }, ContextCompat.getMainExecutor(this))
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

}
