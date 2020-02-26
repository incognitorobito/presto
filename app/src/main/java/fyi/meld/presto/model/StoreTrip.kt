package fyi.meld.presto.model

import fyi.meld.presto.model.CartItem

class StoreTrip constructor()
{
    val items = arrayOf<CartItem>()
    val runningTotal : Float = 0.00f;
    val localTaxRate : Float = 0.00f;
}