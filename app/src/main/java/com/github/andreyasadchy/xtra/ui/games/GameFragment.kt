package com.github.andreyasadchy.xtra.ui.games

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.databinding.FragmentMediaPagerBinding
import com.github.andreyasadchy.xtra.ui.clips.common.ClipsFragment
import com.github.andreyasadchy.xtra.ui.main.MainActivity
import com.github.andreyasadchy.xtra.ui.streams.common.StreamsFragment
import com.github.andreyasadchy.xtra.ui.videos.game.GameVideosFragment
import com.google.android.material.tabs.TabLayoutMediator

class GameFragment : Fragment() {

    private var _binding: FragmentMediaPagerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMediaPagerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val activity = requireActivity() as MainActivity
        with(binding) {
            viewPager.adapter = PagerAdapter(this@GameFragment)
            TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                tab.text = when (position) {
                    0 -> getString(R.string.live)
                    1 -> getString(R.string.videos)
                    else -> getString(R.string.clips)
                }
            }.attach()
/*            toolbar.apply {
                title = args.gameName
                navigationIcon = Utils.getNavigationIcon(activity)
                //setNavigationOnClickListener { activity.popFragment() }
            }*/
        }
    }

    private inner class PagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> StreamsFragment()
                1 -> GameVideosFragment()
                else -> ClipsFragment()
            }.also { it.arguments = arguments }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}