package fyi.meld.presto.ui

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.androidisland.vita.VitaOwner
import com.androidisland.vita.vita

import fyi.meld.presto.R
import fyi.meld.presto.viewmodels.PrestoViewModel
import kotlinx.android.synthetic.main.cart_fragment.*
import java.lang.ref.WeakReference

class CartFragment : Fragment(), View.OnClickListener {

    lateinit var mCartItemAdapter: CartItemAdapter
    private var prestoVM : PrestoViewModel = vita.with(VitaOwner.Multiple(this)).getViewModel<PrestoViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.cart_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        new_item_btn.setOnClickListener(this)
        setupCartItemsView()
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.new_item_btn -> prestoVM.switchToNewItemUI()
        }
    }

    override fun onStart() {
        super.onStart()
        updateCartUI()
    }

    private fun updateCartUI()
    {
        mCartItemAdapter.notifyDataSetChanged()

        if(prestoVM.storeTrip.value!!.items.isNotEmpty() && empty_cart_text.visibility == View.VISIBLE)
        {
            empty_cart_text.visibility = View.INVISIBLE
        }
        else if(prestoVM.storeTrip.value!!.items.isEmpty())
        {
            empty_cart_text.visibility = View.VISIBLE
        }
    }

    private fun setupCartItemsView()
    {
        cart_items_view.layoutManager =
            LinearLayoutManager(requireActivity())

        mCartItemAdapter = CartItemAdapter(
            requireActivity(),
            WeakReference(prestoVM.storeTrip.value!!)
        )

        cart_items_view.adapter = mCartItemAdapter
    }

    companion object {
        @JvmStatic
        fun newInstance() = CartFragment()
    }
}
