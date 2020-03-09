package fyi.meld.presto.models

class CartItem constructor(var name : String, var basePrice : Float){
    var photoUri = ""

    fun getPriceAfterTax(localTaxRate : Float) : Float
    {
        return (basePrice * (localTaxRate / 100f)) + basePrice
    }
}