package fyi.meld.presto.ui

import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import fyi.meld.presto.R

class CartItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
{
    var itemPrice = itemView.findViewById<TextView>(R.id.cart_item_price_text)
    var itemImage = itemView.findViewById<ImageView>(R.id.cart_item_image)
    var itemType = itemView.findViewById<TextView>(R.id.cart_item_type_text)
    var itemOptions = itemView.findViewById<ImageButton>(R.id.cart_item_menu_btn)
    var itemQuantity = itemView.findViewById<TextView>(R.id.cart_item_quantity_text)
}