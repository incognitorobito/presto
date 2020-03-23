package fyi.meld.presto.ui

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import fyi.meld.presto.R

class CartItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
{
    var itemName = itemView.findViewById<TextView>(R.id.cart_item_name_text)
    var itemPrice = itemView.findViewById<TextView>(R.id.cart_item_price_text)
    var itemImage = itemView.findViewById<ImageView>(R.id.cart_item_image)
    var itemType = itemView.findViewById<TextView>(R.id.cart_item_type_text)
    var itemCreated = itemView.findViewById<TextView>(R.id.cart_item_added_text)

}