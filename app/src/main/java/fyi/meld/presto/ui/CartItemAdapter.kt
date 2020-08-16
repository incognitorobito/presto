package fyi.meld.presto.ui

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import fyi.meld.presto.R
import fyi.meld.presto.models.StoreTrip
import fyi.meld.presto.utils.Constants
import fyi.meld.presto.utils.Constants.ItemTypeToDrawable
import fyi.meld.presto.utils.Constants.TAG
import java.lang.ref.WeakReference

class CartItemAdapter(val baseContext : Context, val trip : WeakReference<StoreTrip>) : RecyclerView.Adapter<CartItemViewHolder>(), PopupMenu.OnMenuItemClickListener {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartItemViewHolder {
        var cartItemLayout = LayoutInflater.from(baseContext).inflate(R.layout.cart_item, parent, false)
        var cartItemView =
            CartItemViewHolder(cartItemLayout)
        return cartItemView
    }

    override fun getItemCount(): Int {
        return trip.get()?.items!!.size
    }

    override fun onBindViewHolder(holder: CartItemViewHolder, position: Int) {
        val cartItem = trip.get()?.items!![position]
        holder.itemPrice.text = "$" + cartItem.basePrice.toString()
        holder.itemType.text = if (cartItem.name.isNullOrEmpty()) cartItem.type.toString() else cartItem.name.toString() + " - " + cartItem.type.toString()
        holder.itemImage.setImageResource(ItemTypeToDrawable[cartItem.type]!!)
        holder.itemOptions.setOnClickListener {
            val popup = PopupMenu(baseContext, it)
            popup.inflate(R.menu.cart_item_options)
            popup.setOnMenuItemClickListener(this)
            popup.show()
        }
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        Log.d(Constants.TAG, item?.itemId.toString())
        return true;
    }
}