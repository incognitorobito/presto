package fyi.meld.presto.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.androidisland.vita.VitaOwner
import com.androidisland.vita.vita
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import fyi.meld.presto.R
import fyi.meld.presto.models.CartItem
import fyi.meld.presto.utils.Constants
import fyi.meld.presto.utils.PriceEngine
import fyi.meld.presto.viewmodels.PrestoViewModel
import kotlinx.android.synthetic.main.critical_info.*
import kotlinx.android.synthetic.main.generic_input_diag.view.*
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity(), LifecycleOwner, PrestoViewModel.SwitchUIHandler {

    lateinit var prestoVM : PrestoViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

        if (currentFragment == null) {
            val viewPagerFrag = ViewPagerFragment.newInstance()
            supportFragmentManager
                .beginTransaction()
                .add(R.id.fragment_container, viewPagerFrag)
                .commit()
        }

        if(!hasPermissions())
        {
            requestPermission()
        }

        configureViewModel()

        tax_rate_container.setOnLongClickListener {
            showTaxRateEditorDiag()
            return@setOnLongClickListener true
        }

        cart_size_container.setOnLongClickListener {
            showClearCartConfirmDiag()
            return@setOnLongClickListener true
        }
    }

    private fun showTaxRateEditorDiag()
    {
        val inflater = LayoutInflater.from(this)
        val changeTaxDiagView = inflater.inflate(R.layout.generic_input_diag, null)
        val taxInput = changeTaxDiagView.basic_edit_input
        taxInput.setText(prestoVM.storeTrip.value?.localTaxRate.toString())

        var changeTaxDiag = MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.change_tax_rate_title))
            .setView(changeTaxDiagView)
            .setNegativeButton("Cancel") { dialog, which ->
                dialog.cancel()
            }
            .setPositiveButton("Save") { dialog, which ->
                if(taxInput.text.toString().isNotEmpty() && changeTaxDiagView.basic_edit_input.text.toString().toFloatOrNull() != null)
                {
                    val newTaxRate = taxInput.text.toString().toFloat()
                    prestoVM.updateTaxRate(newTaxRate)
                }
            }
            .create()


        changeTaxDiag.show();
    }

    private fun showClearCartConfirmDiag()
    {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.clear_cart_title))
            .setTitle(getString(R.string.clear_cart_hint))
            .setNegativeButton("Cancel") { dialog, which ->
                dialog.cancel()
            }
            .setPositiveButton("Clear") { dialog, which ->
                prestoVM.resetCart()
            }
            .show()
    }

    override fun onNewItemUIRequested() {
        //TODO Combine this and other UI request method into a simpler solution.
        val newItemUI = CartItemFragment.newInstance()
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, newItemUI)
            .addToBackStack(null)
            .commit()
    }

    override fun onCartUIRequested() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

        if(currentFragment is ViewPagerFragment)
        {
            currentFragment.fragmentPager?.currentItem = 0
        }
        else
        {
            val swipeUI = ViewPagerFragment.newInstance()
            swipeUI.fragmentPager?.currentItem = 0

            supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragment_container, swipeUI)
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onEditItemUIRequested(item: CartItem) {
        val newItemUI = CartItemFragment.newInstance(item)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, newItemUI)
            .addToBackStack(null)
            .commit()
    }

    override fun onBackPressed() {
        if (fragmentManager.backStackEntryCount > 0) {
            fragmentManager.popBackStack()
        } else if(prestoVM.isCameraRunning)
        {
            prestoVM.switchToCartUI()
        }
        else {
            super.onBackPressed()
        }
    }

    private fun configureViewModel()
    {
        prestoVM = vita.with(VitaOwner.Single(this)).getViewModel<PrestoViewModel>()
        prestoVM.switchUIHandler = this
        prestoVM.priceEngine = PriceEngine(WeakReference(this))
        prestoVM.priceEngine.initialize()

        configureDataObservers()
    }

    private fun configureDataObservers()
    {
        prestoVM.storeTrip.observe(this, Observer {
            cart_size_text.text = it.size.toString()
            cart_total_text.text = "$" + String.format("%.2f", it.totalAfterTax)
            tax_rate_text.text = String.format("%.2f", it.localTaxRate) + "%"
        })
    }

    private fun hasPermissions(): Boolean{
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }


    private fun requestPermission(){
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA),
            Constants.PERMISSIONS_REQUEST_CODE)
    }

    override fun onDestroy() {
        super.onDestroy()

        prestoVM.shutdownPriceEngine()
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
