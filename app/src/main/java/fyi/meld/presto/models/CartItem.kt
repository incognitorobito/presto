package fyi.meld.presto.models

import fyi.meld.presto.viewmodels.ItemType

class CartItem constructor(var type : ItemType, var basePrice : Float){
    var photoUri = ""

    fun getPriceAfterTax(localTaxRate : Float) : Float
    {
        return (basePrice * (localTaxRate / 100f)) + basePrice
    }
}