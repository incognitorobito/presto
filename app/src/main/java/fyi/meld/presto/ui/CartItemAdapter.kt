package fyi.meld.presto.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.text.isDigitsOnly
import androidx.recyclerview.widget.RecyclerView
import com.androidisland.vita.VitaOwner
import com.androidisland.vita.vita
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import fyi.meld.presto.R
import fyi.meld.presto.models.CartItem
import fyi.meld.presto.models.StoreTrip
import fyi.meld.presto.utils.Constants
import fyi.meld.presto.utils.Constants.ItemTypeToDrawable
import fyi.meld.presto.viewmodels.PrestoViewModel
import kotlinx.android.synthetic.main.generic_input_diag.view.*
import java.lang.ref.WeakReference

class CartItemAdapter(val baseContext : Context, private val vm : PrestoViewModel) : RecyclerView.Adapter<CartItemViewHolder>() {

    private var trip = vm.storeTrip.value!!

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartItemViewHolder {
        var cartItemLayout = LayoutInflater.from(baseContext).inflate(R.layout.cart_item, parent, false)
        var cartItemView =
            CartItemViewHolder(cartItemLayout)
        return cartItemView
    }

    override fun getItemCount(): Int {
        return trip.items.size
    }

    override fun onBindViewHolder(holder: CartItemViewHolder, position: Int) {
        val cartItem: CartItem = trip.items[position]
        holder.vm = vm
        holder.cartItem = cartItem
        holder.itemPrice.text = "$" + cartItem.basePrice.toString()
        holder.itemType.text = if (cartItem.name.isNullOrEmpty()) cartItem.type.toString() else cartItem.name.toString() + " - " + cartItem.type.toString()
        holder.itemImage.setImageResource(ItemTypeToDrawable[cartItem.type]!!)
        holder.itemQuantity.text = "x" + cartItem.qty.toString()
        holder.itemOptions.setOnClickListener {
            val popup = PopupMenu(baseContext, it)
            popup.inflate(R.menu.cart_item_options)
            popup.setOnMenuItemClickListener(holder)
            popup.show()
        }
    }
}