package fyi.meld.presto.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import fyi.meld.presto.models.CartItem
import fyi.meld.presto.models.StoreTrip
import fyi.meld.presto.utils.PriceEngine

class PrestoViewModel : ViewModel() {
    var storeTrip : MutableLiveData<StoreTrip> = MutableLiveData()
    var isCameraRunning = false;
    var switchUIHandler : SwitchUIHandler? = null

    lateinit var priceEngine : PriceEngine

    private var mCurrentTrip = StoreTrip()


    init {
        storeTrip.value = mCurrentTrip
    }

    fun addToCart(item : CartItem)
    {
        mCurrentTrip.addToCart(item)
        storeTrip.value = mCurrentTrip
    }

    fun switchToNewItemUI()
    {
        switchUIHandler?.onNewItemUIRequested()
    }

    fun switchToCartUI()
    {
        switchUIHandler?.onCartUIRequested()
    }

    interface SwitchUIHandler
    {
        fun onNewItemUIRequested()
        fun onCartUIRequested()
    }

}