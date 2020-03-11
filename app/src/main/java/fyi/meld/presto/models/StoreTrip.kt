package fyi.meld.presto.models

class StoreTrip ()
{
    var items = arrayListOf<CartItem>()
    var runningTotal : Float = 0.00f;
    var localTaxRate : Float = 8.26f;

    fun getTotalAfterTax() : Float
    {
        return (runningTotal * (localTaxRate / 100f)) + runningTotal
    }

    fun addToCart(item : CartItem)
    {
        items.add(item)
        runningTotal += item.basePrice
    }
}