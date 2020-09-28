package fyi.meld.presto.models

import fyi.meld.presto.utils.ItemType

data class CartItem constructor(var name : String, var type : ItemType, var basePrice : Float){
    var photoUri = ""
    var qty = 1
    var uid = this.hashCode()

    fun getPriceAfterTax(localTaxRate : Float) : Float
    {
        return (getPrice() * (localTaxRate / 100f)) + getPrice()
    }

    fun getPrice() : Float
    {
        return qty * basePrice
    }
}
