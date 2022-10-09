package com.github.andreyasadchy.xtra.ui.follow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.databinding.FragmentMediaPagerToolbarBinding
import com.github.andreyasadchy.xtra.ui.common.pagers.MediaPagerToolbarFragment
import com.google.android.material.tabs.TabLayoutMediator

class FollowPagerFragment : MediaPagerToolbarFragment() {

    companion object {
        private const val DEFAULT_ITEM = "default_item"
        private const val LOGGED_IN = "logged_in"

        fun newInstance(defaultItem: Int?, loggedIn: Boolean) = FollowPagerFragment().apply {
            arguments = Bundle().apply {
                putInt(DEFAULT_ITEM, defaultItem ?: 0)
                putBoolean(LOGGED_IN, loggedIn)
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
            val loggedIn = requireArguments().getBoolean(LOGGED_IN)
            setAdapter(adapter = FollowPagerAdapter(this@FollowPagerFragment, loggedIn),
                defaultItem = if (loggedIn) {
                    when (defaultItem) {
                        1 -> 2
                        2 -> 3
                        3 -> 0
                        else -> 1
                    }
                } else {
                    when (defaultItem) {
                        2 -> 2
                        3 -> 0
                        else -> 1
                    }
                })
            TabLayoutMediator(pagerLayout.tabLayout, pagerLayout.viewPager) { tab, position ->
                tab.text = if (loggedIn) {
                    when (position) {
                        0 -> getString(R.string.games)
                        1 -> getString(R.string.live)
                        2 -> getString(R.string.videos)
                        else -> getString(R.string.channels)
                    }
                } else {
                    when (position) {
                        0 -> getString(R.string.games)
                        1 -> getString(R.string.live)
                        else -> getString(R.string.channels)
                    }
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