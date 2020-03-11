package fyi.meld.presto.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.androidisland.vita.VitaOwner
import com.androidisland.vita.vita
import fyi.meld.presto.R
import fyi.meld.presto.models.CartItem
import fyi.meld.presto.viewmodels.PrestoViewModel

import kotlinx.android.synthetic.main.activity_new_item.*
import kotlinx.android.synthetic.main.critical_info.*

class NewItemActivity : AppCompatActivity() {

    lateinit var prestoVM : PrestoViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_item)

        save_item_btn.setOnClickListener { view ->
            trySaveItem()
        }

        configureViewModel()
    }

    private fun trySaveItem()
    {
        var priceEntered = item_price_input.getValue()

        if(priceEntered.isNaN())
        {
            Toast.makeText(this, "Please enter a valid price.", Toast.LENGTH_SHORT).show()
        }
        else
        {
            prestoVM.addItemToCart(CartItem("Bob", item_price_input.getValue().toFloat()))
        }
    }

    private fun configureViewModel()
    {
        prestoVM = vita.with(VitaOwner.Multiple(this)).getViewModel<PrestoViewModel>()
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

}
