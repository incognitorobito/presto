package fyi.meld.presto.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import fyi.meld.presto.models.CartItem
import fyi.meld.presto.models.StoreTrip
import fyi.meld.presto.utils.PriceEngine
import java.text.SimpleDateFormat
import java.util.*

class PrestoViewModel : ViewModel() {
    var storeTrip : MutableLiveData<StoreTrip> = MutableLiveData()
    var isCameraRunning = false;
    var switchUIHandler : SwitchUIHandler? = null
    var cartUpdatedHandler: CartUpdatedHandler? = null

    lateinit var priceEngine : PriceEngine

    private lateinit var mCurrentTrip : StoreTrip


    init {
        mCurrentTrip = StoreTrip(getTripStart())
        notifyCartUpdated()
    }

    fun getTripStart() : String
    {
        val sdf = SimpleDateFormat("EEE, d MMM yyyy, hh:mm", Locale.getDefault())
        return sdf.format(Date())
    }

    fun resetCart()
    {
        mCurrentTrip.clearCart()
        mCurrentTrip.startTime = getTripStart()
        notifyCartUpdated()
    }

    fun updateTaxRate(taxRate: Float)
    {
        mCurrentTrip.localTaxRate = taxRate
        notifyCartUpdated()
    }

    fun shutdownPriceEngine()
    {
        priceEngine.shutdown()
    }

    fun addToCart(item : CartItem)
    {
        mCurrentTrip.addToCart(item)
        notifyCartUpdated()
    }

    fun removeFromCart(item: CartItem)
    {
        mCurrentTrip.removeFromCart(item)
        notifyCartUpdated()
    }

    fun updateCartTotals()
    {
        mCurrentTrip.updateTotals()
        notifyCartUpdated()
    }

    fun switchToNewItemUI()
    {
        switchUIHandler?.onNewItemUIRequested()
    }

    fun switchToCartUI()
    {
        switchUIHandler?.onCartUIRequested()
    }

    fun switchToEditItemUI(item: CartItem)
    {
        switchUIHandler?.onEditItemUIRequested(item)
    }

    private fun notifyCartUpdated()
    {
        storeTrip.value = mCurrentTrip
        cartUpdatedHandler?.onCartUpdated()
    }

    interface SwitchUIHandler
    {
        fun onNewItemUIRequested()
        fun onCartUIRequested()
        fun onEditItemUIRequested(item: CartItem)
    }

    interface CartUpdatedHandler
    {
        fun onCartUpdated()
    }
}