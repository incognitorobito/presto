package fyi.meld.presto.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.androidisland.vita.VitaOwner
import com.androidisland.vita.vita
import fyi.meld.presto.R
import fyi.meld.presto.utils.Presto
import fyi.meld.presto.viewmodels.PrestoViewModel
import kotlinx.android.synthetic.main.cart_fragment.*
import java.lang.ref.WeakReference

class CartFragment : Fragment(), View.OnClickListener, PrestoViewModel.CartUpdatedHandler{

    lateinit var mCartItemAdapter: CartItemAdapter
    private lateinit var prestoVM : PrestoViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.cart_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prestoVM = vita.with(VitaOwner.Single(requireActivity())).getViewModel<PrestoViewModel>()
        prestoVM.cartUpdatedHandler = this

        new_item_btn.setOnClickListener(this)
        setupCartItemsView()
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.new_item_btn -> prestoVM.switchToNewItemUI()
        }
    }

    override fun onResume() {
        super.onResume()
        updateCartUI()
    }

    private fun updateCartUI()
    {
        mCartItemAdapter.notifyDataSetChanged()

        if(prestoVM.storeTrip.value!!.items.isNotEmpty() && empty_cart_text.visibility == View.VISIBLE)
        {
            empty_cart_text.visibility = View.INVISIBLE
            cart_container.setBackgroundColor(Color.WHITE);
        }
        else if(prestoVM.storeTrip.value!!.items.isEmpty())
        {
            empty_cart_text.visibility = View.VISIBLE
            cart_container.setBackgroundResource(R.drawable.popsicle_background);
        }
    }

    private fun setupCartItemsView()
    {
        cart_items_view.layoutManager =
            LinearLayoutManager(requireActivity())

        mCartItemAdapter = CartItemAdapter(
            requireActivity(),
            prestoVM)

        mCartItemAdapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

        cart_items_view.adapter = mCartItemAdapter
    }

    override fun onCartUpdated() {
        updateCartUI()
    }

    companion object {
        @JvmStatic
        fun newInstance() = CartFragment()
    }
}
