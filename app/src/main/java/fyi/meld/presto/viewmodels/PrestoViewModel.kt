package fyi.meld.presto.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import fyi.meld.presto.models.CartItem
import fyi.meld.presto.models.StoreTrip

class PrestoViewModel : ViewModel() {
    var storeTrip : MutableLiveData<StoreTrip> = MutableLiveData()
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

enum class ItemType
{
    Groceries,
    Personal,
    Fun,
    Other
}