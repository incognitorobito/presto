package fyi.meld.presto.ui

import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.text.isDigitsOnly
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import fyi.meld.presto.R
import fyi.meld.presto.models.CartItem
import fyi.meld.presto.viewmodels.PrestoViewModel
import kotlinx.android.synthetic.main.item_qty_diag.view.*

class CartItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), PopupMenu.OnMenuItemClickListener
{
    private val baseContext = itemView.context

    lateinit var vm : PrestoViewModel

    var cartItem : CartItem? = null

    var itemPrice = itemView.findViewById<TextView>(R.id.cart_item_price_text)
    var itemImage = itemView.findViewById<ImageView>(R.id.cart_item_image)
    var itemType = itemView.findViewById<TextView>(R.id.cart_item_type_text)
    var itemOptions = itemView.findViewById<ImageButton>(R.id.cart_item_menu_btn)
    var itemQuantity = itemView.findViewById<TextView>(R.id.cart_item_quantity_text)

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when(item?.itemId) {
            R.id.changeItemQuantityOption -> openQuantityDialog();
            R.id.editItemOption -> openItemFragment();
            R.id.removeItemOption -> openRemovalDialog();
        }
        return true;
    }

    fun openItemFragment() {
        vm.switchToEditItemUI(cartItem!!)
    }

    fun openQuantityDialog() {
        val inflater = LayoutInflater.from(baseContext)
        val quantityDiagView = inflater.inflate(R.layout.item_qty_diag, null)
        val quantityInput = quantityDiagView.edit_qty_input
        quantityInput.setText(cartItem?.qty.toString())


        var quantityDiag = MaterialAlertDialogBuilder(baseContext)
            .setTitle(baseContext.getString(R.string.edit_quantity_title))
            .setView(quantityDiagView)
            .setNegativeButton("Cancel") { dialog, which ->
                dialog.cancel()
            }
            .setPositiveButton("Save") { dialog, which ->
                if(quantityInput.text.toString().isNotEmpty() && quantityDiagView.edit_qty_input.text.toString().isDigitsOnly())
                {
                    cartItem?.qty = quantityInput.text.toString().toInt()
                    vm.updateCartTotals()
                }
            }
            .create()


        quantityDiag.show();
    }

    fun openRemovalDialog() {
        MaterialAlertDialogBuilder(baseContext)
            .setTitle(baseContext.getString(R.string.remove_item_title))
            .setMessage(baseContext.getString(R.string.remove_item_hint))
            .setNegativeButton("Cancel") { dialog, which ->
                dialog.cancel()
            }
            .setPositiveButton("Remove") { dialog, which ->
                vm.removeFromCart(cartItem!!)
            }
            .show()
    }

}