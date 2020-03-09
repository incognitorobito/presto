package fyi.meld.presto

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import fyi.meld.presto.models.CartItem
import fyi.meld.presto.viewmodels.PrestoViewModel
import kotlinx.android.synthetic.main.activity_main.*

import kotlinx.android.synthetic.main.activity_new_item.*
import kotlinx.android.synthetic.main.critical_info.*

class NewItemActivity : AppCompatActivity() {

    lateinit var prestoVM : PrestoViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_item)

        save_item_btn.setOnClickListener { view ->
            prestoVM.addItemToCart(CartItem("Bob", 7.35f))
        }

        configureViewModel()
    }

    private fun configureViewModel()
    {
        prestoVM = ViewModelProviders.of(this).get(PrestoViewModel::class.java)
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
