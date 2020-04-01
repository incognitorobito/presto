package fyi.meld.presto.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import fyi.meld.presto.models.CartItem
import fyi.meld.presto.models.StoreTrip
import fyi.meld.presto.utils.PriceEngine

class PrestoViewModel : ViewModel() {
    var storeTrip : MutableLiveData<StoreTrip> = MutableLiveData()
    lateinit var priceEngine : PriceEngine
    var isCameraRunning = false;
    var initialLayoutState : Int = -1;

    private var mCurrentTrip = StoreTrip()

    init {
        storeTrip.value = mCurrentTrip
    }

    fun addItemToCart(item : CartItem)
    {
        mCurrentTrip.addToCart(item)
        storeTrip.value = mCurrentTrip
    }
}