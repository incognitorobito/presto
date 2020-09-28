package fyi.meld.presto.ui

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.androidisland.vita.VitaOwner
import com.androidisland.vita.vita
import fyi.meld.presto.R
import fyi.meld.presto.models.CartItem
import fyi.meld.presto.utils.Constants
import fyi.meld.presto.utils.Constants.ItemTypeToDrawable
import fyi.meld.presto.utils.ItemType
import fyi.meld.presto.viewmodels.PrestoViewModel
import kotlinx.android.synthetic.main.new_item_fragment.*

/**
 * A simple [Fragment] subclass.
 * Use the [NewItemFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class NewItemFragment constructor(val selectedItem: CartItem?) : Fragment() {

    private lateinit var prestoVM : PrestoViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.new_item_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prestoVM = vita.with(VitaOwner.Single(requireActivity())).getViewModel<PrestoViewModel>()

        selectedItem?.let {
            populateFields()
        }

        save_item_btn.setOnClickListener { view ->
            trySaveItem()
        }

        item_type_select.setOnCheckedChangeListener(RadioGroup.OnCheckedChangeListener { group, checkedId ->
            val itemType = getItemTypeFromButton(checkedId)
            item_image.setImageResource(ItemTypeToDrawable.get(itemType)!!)
        })
    }

    private fun populateFields()
    {
        item_name_input.setText(selectedItem?.name)
        item_price_input.setText(selectedItem?.basePrice.toString())
        item_type_select.check(selectedItem?.type!!.ordinal)

        if(selectedItem?.photoUri.isNotBlank())
        {
            item_image.scaleType = ImageView.ScaleType.FIT_CENTER
            item_image.imageTintList = null
            item_image.setImageURI(Uri.parse(selectedItem?.photoUri))
            image_placeholder_hint.visibility = View.INVISIBLE
        }

    }

    private fun getItemTypeFromButton(checkedID : Int) : ItemType
    {
        val checkedButton = view?.findViewById<RadioButton>(checkedID)
        return ItemType.valueOf(checkedButton?.text.toString())
    }

    private fun trySaveItem()
    {
        val checkedID = item_type_select.checkedRadioButtonId

        if(item_price_input.text.isNullOrEmpty() || item_name_input.text.isNullOrEmpty() || checkedID == -1)
        {
            Toast.makeText(requireContext(), "Please enter a price, a name, and select a category.", Toast.LENGTH_SHORT).show()
        }
        else
        {
            val itemType = getItemTypeFromButton(checkedID)

            if(selectedItem != null)
            {
                //TODO Update
            }
            else
            {
                val newCartItem = CartItem(item_name_input.text.toString(), itemType, item_price_input.text.toString().toFloat())
                prestoVM.addToCart(newCartItem)
            }

            prestoVM.switchToCartUI()
        }
    }

    companion object {
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance() = NewItemFragment(null)
        fun newInstance(item: CartItem) = NewItemFragment(item)
    }
}
