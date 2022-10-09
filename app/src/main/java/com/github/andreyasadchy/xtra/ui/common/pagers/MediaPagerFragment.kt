package com.github.andreyasadchy.xtra.ui.common.pagers

import android.os.Bundle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.github.andreyasadchy.xtra.databinding.FragmentMediaPagerBinding
import com.github.andreyasadchy.xtra.ui.common.BaseNetworkFragment
import com.github.andreyasadchy.xtra.ui.common.Scrollable
import com.github.andreyasadchy.xtra.util.reduceDragSensitivity

abstract class MediaPagerFragment : BaseNetworkFragment(), ItemAwarePagerFragment, Scrollable {

    abstract val pagerBinding: FragmentMediaPagerBinding
    private val binding get() = pagerBinding
    protected lateinit var adapter: FragmentStateAdapter
    private var firstLaunch = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firstLaunch = savedInstanceState == null
    }

    protected fun setAdapter(adapter: FragmentStateAdapter, defaultItem: Int? = null) {
        this.adapter = adapter
        with(binding) {
            viewPager.adapter = adapter
            if (firstLaunch && defaultItem != null) {
                viewPager.setCurrentItem(defaultItem, false)
                firstLaunch = false
            }
            viewPager.offscreenPageLimit = adapter.itemCount
            viewPager.reduceDragSensitivity()
        }
    }

    override fun scrollToTop() {
        (currentFragment as? Scrollable)?.scrollToTop()
    }
}
