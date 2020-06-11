package fyi.meld.presto.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.transition.MaterialSharedAxis

import fyi.meld.presto.R

/**
 * A simple [Fragment] subclass.
 * Use the [ViewPagerFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
private const val NUM_PAGES = 2

class ViewPagerFragment : Fragment() {

    var fragmentPager: ViewPager2? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val backward = MaterialSharedAxis(MaterialSharedAxis.Y, false)
        val forward = MaterialSharedAxis(MaterialSharedAxis.Y, true)

        reenterTransition = backward
        exitTransition = forward
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_view_pager, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fragmentPager = requireActivity().findViewById(R.id.fragment_pager)
        fragmentPager?.adapter = ScreenSlidePagerAdapter(requireActivity())

    }

    companion object {
        @JvmStatic
        fun newInstance() =
            ViewPagerFragment()
    }

    private inner class ScreenSlidePagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = NUM_PAGES

        override fun createFragment(position: Int): Fragment =
            when(position)
            {
                1 -> PriceEngineFragment()
                else -> CartFragment()
            }
    }
}
