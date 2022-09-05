package com.github.andreyasadchy.xtra.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.databinding.FragmentSearchBinding
import com.github.andreyasadchy.xtra.ui.Utils
import com.github.andreyasadchy.xtra.ui.main.MainActivity
import com.github.andreyasadchy.xtra.ui.search.channels.ChannelSearchFragment
import com.github.andreyasadchy.xtra.ui.search.games.GameSearchFragment
import com.github.andreyasadchy.xtra.ui.search.streams.StreamSearchFragment
import com.github.andreyasadchy.xtra.ui.search.videos.VideoSearchFragment
import com.github.andreyasadchy.xtra.util.showKeyboard
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class SearchFragment : Fragment() {

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
            pagerLayout.viewPager.adapter = PagerAdapter(this@SearchFragment)
            TabLayoutMediator(pagerLayout.tabLayout, pagerLayout.viewPager) { tab, position ->
                tab.text = when (position) {
                    0 -> getString(R.string.videos)
                    1 -> getString(R.string.streams)
                    2 -> getString(R.string.channels)
                    else -> getString(R.string.games)
                }
            }.attach()
            search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                private var job: Job? = null

                override fun onQueryTextSubmit(query: String): Boolean {
                    (pagerLayout.viewPager[0] as? Searchable)?.search(query)
                    return false
                }

                override fun onQueryTextChange(newText: String): Boolean {
                    job?.cancel()
                    if (newText.isNotEmpty()) {
                        job = lifecycleScope.launchWhenResumed {
                            delay(750)
                            (pagerLayout.viewPager[0] as? Searchable)?.search(newText)
                        }
                    } else {
                        (pagerLayout.viewPager[0] as? Searchable)?.search(newText) //might be null on rotation, so as?
                    }
                    return false
                }
            })
            toolbar.apply {
                navigationIcon = Utils.getNavigationIcon(activity)
                setNavigationOnClickListener { activity.popFragment() }
            }
            search.showKeyboard()
        }
    }

    private inner class PagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 4

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> VideoSearchFragment()
                1 -> StreamSearchFragment()
                2 -> ChannelSearchFragment()
                else -> GameSearchFragment()
            }.also { it.arguments = arguments }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}