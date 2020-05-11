package fyi.meld.presto.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import fyi.meld.presto.R
import fyi.meld.presto.models.StoreTrip
import fyi.meld.presto.utils.Constants.ItemTypeToDrawable
import java.lang.ref.WeakReference

class CartItemAdapter(val baseContext : Context, val trip : WeakReference<StoreTrip>) : RecyclerView.Adapter<CartItemViewHolder>() {
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
        holder.itemPrice.text = cartItem.basePrice.toString()
        holder.itemType.text = cartItem.type.toString()
        holder.itemImage.setImageResource(ItemTypeToDrawable[cartItem.type]!!)
    }

}