package com.github.andreyasadchy.xtra.ui.saved

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.databinding.FragmentMediaPagerToolbarBinding
import com.github.andreyasadchy.xtra.ui.common.pagers.MediaPagerToolbarFragment
import com.google.android.material.tabs.TabLayoutMediator

class SavedPagerFragment : MediaPagerToolbarFragment() {

    companion object {
        private const val DEFAULT_ITEM = "default_item"

        fun newInstance(defaultItem: Int?) = SavedPagerFragment().apply {
            arguments = Bundle().apply {
                putInt(DEFAULT_ITEM, defaultItem ?: 0)
            }
        }
    }

    override val pagerToolbarBinding get() = binding
    private var _binding: FragmentMediaPagerToolbarBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMediaPagerToolbarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            val defaultItem = requireArguments().getInt(DEFAULT_ITEM)
            setAdapter(adapter = SavedPagerAdapter(this@SavedPagerFragment), defaultItem = defaultItem)
            TabLayoutMediator(pagerLayout.tabLayout, pagerLayout.viewPager) { tab, position ->
                tab.text = when (position) {
                    0 -> getString(R.string.bookmarks)
                    else -> getString(R.string.downloads)
                }
            }.attach()
        }
    }

    override val currentFragment: Fragment?
        get() = childFragmentManager.findFragmentByTag("f${binding.pagerLayout.viewPager.currentItem}")

    override fun initialize() {}

    override fun onNetworkRestored() {}

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}