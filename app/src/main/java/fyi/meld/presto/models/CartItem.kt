package fyi.meld.presto.models

import fyi.meld.presto.utils.ItemType

data class CartItem constructor(var name : String, var type : ItemType, var basePrice : Float){
    var photoUri = ""

    fun getPriceAfterTax(localTaxRate : Float) : Float
    {
        return (basePrice * (localTaxRate / 100f)) + basePrice
    }
}
