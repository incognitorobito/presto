package fyi.meld.presto.model

data class CartItem constructor(var id : Int, var name : String, var basePrice : Float, var finalPrice : Float, var photoUri : String){}