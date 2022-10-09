package com.github.andreyasadchy.xtra.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.databinding.FragmentSearchBinding
import com.github.andreyasadchy.xtra.ui.Utils
import com.github.andreyasadchy.xtra.ui.common.pagers.MediaPagerFragment
import com.github.andreyasadchy.xtra.ui.main.MainActivity
import com.github.andreyasadchy.xtra.util.showKeyboard
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class SearchFragment : MediaPagerFragment() {

    override val pagerBinding get() = binding.pagerLayout
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            val activity = requireActivity() as MainActivity
            setAdapter(adapter = SearchPagerAdapter(this@SearchFragment), defaultItem = 2)
            pagerLayout.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    (currentFragment as? Searchable)?.search(search.query.toString())
                }
            })
            TabLayoutMediator(pagerLayout.tabLayout, pagerLayout.viewPager) { tab, position ->
                tab.text = when (position) {
                    0 -> getString(R.string.videos)
                    1 -> getString(R.string.streams)
                    2 -> getString(R.string.channels)
                    else -> getString(R.string.games)
                }
            }.attach()
            toolbar.apply {
                navigationIcon = Utils.getNavigationIcon(activity)
                setNavigationOnClickListener { activity.popFragment() }
            }
            search.showKeyboard()
        }
    }

    override val currentFragment: Fragment?
        get() = childFragmentManager.findFragmentByTag("f${binding.pagerLayout.viewPager.currentItem}")

    override fun initialize() {
        binding.search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            private var job: Job? = null

            override fun onQueryTextSubmit(query: String): Boolean {
                (currentFragment as? Searchable)?.search(query)
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                job?.cancel()
                if (newText.isNotEmpty()) {
                    job = lifecycleScope.launchWhenResumed {
                        delay(750)
                        (currentFragment as? Searchable)?.search(newText)
                    }
                } else {
                    (currentFragment as? Searchable)?.search(newText) //might be null on rotation, so as?
                }
                return false
            }
        })
    }

    override fun onNetworkRestored() {}

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}