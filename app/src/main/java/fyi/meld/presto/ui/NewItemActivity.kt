package fyi.meld.presto.ui

import android.app.Activity
import android.os.Bundle
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.androidisland.vita.VitaOwner
import com.androidisland.vita.vita
import fyi.meld.presto.R
import fyi.meld.presto.models.CartItem
import fyi.meld.presto.utils.Constants.ItemTypeToDrawable
import fyi.meld.presto.utils.ItemType
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

        item_type_select.setOnCheckedChangeListener(RadioGroup.OnCheckedChangeListener { group, checkedId ->
            val itemType = getItemTypeFromButton(checkedId)
            item_image.setImageResource(ItemTypeToDrawable.get(itemType)!!)
        })

        configureViewModel()
    }

    private fun getItemTypeFromButton(checkedID : Int) : ItemType
    {
        val checkedButton = findViewById<RadioButton>(checkedID)
        return ItemType.valueOf(checkedButton.text.toString())
    }

    private fun trySaveItem()
    {
        val checkedID = item_type_select.checkedRadioButtonId

        if(item_price_input.text.isNullOrEmpty() || item_name_input.text.isNullOrEmpty() || checkedID == -1)
        {
            Toast.makeText(this, "Please enter a price, a name, and select a category.", Toast.LENGTH_SHORT).show()
        }
        else
        {
            val itemType = getItemTypeFromButton(checkedID)
            val newCartItem = CartItem(item_name_input.text.toString(), itemType, item_price_input.text.toString().toFloat())
            prestoVM.addItemToCart(newCartItem)
            Toast.makeText(this, "Item saved successfully.", Toast.LENGTH_SHORT).show()
            setResult(Activity.RESULT_OK)
            finish()
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
