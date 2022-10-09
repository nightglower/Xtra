package com.github.andreyasadchy.xtra.ui.search.tags

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.github.andreyasadchy.xtra.databinding.FragmentSearchBinding
import com.github.andreyasadchy.xtra.ui.Utils
import com.github.andreyasadchy.xtra.ui.common.pagers.MediaPagerFragment
import com.github.andreyasadchy.xtra.ui.main.MainActivity
import com.github.andreyasadchy.xtra.ui.search.Searchable
import com.github.andreyasadchy.xtra.util.C
import com.github.andreyasadchy.xtra.util.gone
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class BaseTagSearchFragment : MediaPagerFragment() {

    companion object {
        fun newInstance(getGameTags: Boolean, gameId: String?, gameName: String?) = BaseTagSearchFragment().apply {
            arguments = Bundle().apply {
                putBoolean(C.GET_GAME_TAGS, getGameTags)
                putString(C.GAME_ID, gameId)
                putString(C.GAME_NAME, gameName)
            }
        }
    }

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
            setAdapter(BaseTagSearchPagerAdapter(this@BaseTagSearchFragment))
            pagerLayout.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    (currentFragment as? Searchable)?.search(search.query.toString())
                }
            })
            toolbar.apply {
                navigationIcon = Utils.getNavigationIcon(activity)
                setNavigationOnClickListener { activity.popFragment() }
            }
            pagerLayout.tabLayout.gone()
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