package com.github.andreyasadchy.xtra.ui.follow

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.model.User
import com.github.andreyasadchy.xtra.ui.common.MediaFragment
import com.github.andreyasadchy.xtra.ui.follow.channels.FollowedChannelsFragment
import com.github.andreyasadchy.xtra.ui.follow.games.FollowedGamesFragment
import com.github.andreyasadchy.xtra.ui.main.MainActivity
import com.github.andreyasadchy.xtra.ui.streams.followed.FollowedStreamsFragment
import com.github.andreyasadchy.xtra.ui.videos.followed.FollowedVideosFragment
import com.github.andreyasadchy.xtra.util.C
import com.github.andreyasadchy.xtra.util.prefs
import com.github.andreyasadchy.xtra.util.visible
import kotlinx.android.synthetic.main.fragment_media.*

class FollowMediaFragment : MediaFragment() {

    private var pagerFragment = true
    private var defaultItem = 0
    private var loggedIn = false
    private var firstLaunch = true

    override val spinnerItems: Array<String>
        get() = resources.getStringArray(if (loggedIn) R.array.spinnerFollowedEntries else R.array.spinnerFollowedEntriesNotLoggedIn)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity() as MainActivity
        pagerFragment = requireContext().prefs().getBoolean(C.UI_FOLLOWPAGER, true)
        defaultItem = requireContext().prefs().getString(C.UI_FOLLOW_DEFAULT_PAGE, "0")?.toInt() ?: 0
        loggedIn = !User.get(activity).gqlToken.isNullOrBlank()
        if (pagerFragment) {
            currentFragment = if (previousItem != -2) {
                val newFragment = FollowPagerFragment.newInstance(defaultItem, loggedIn)
                childFragmentManager.beginTransaction().replace(R.id.fragmentContainer, newFragment).commit()
                previousItem = -2
                newFragment
            } else {
                childFragmentManager.findFragmentById(R.id.fragmentContainer)
            }
        } else {
            spinner.visible()
            spinner.adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_dropdown_item, spinnerItems)
            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    currentFragment = if (position != previousItem) {
                        val newFragment = onSpinnerItemSelected(position)
                        childFragmentManager.beginTransaction().replace(R.id.fragmentContainer, newFragment).commit()
                        previousItem = position
                        newFragment
                    } else {
                        childFragmentManager.findFragmentById(R.id.fragmentContainer)
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    override fun onSpinnerItemSelected(position: Int): Fragment {
        if (firstLaunch) {
            spinner.setSelection(
                if (loggedIn) {
                    defaultItem
                } else {
                    when (defaultItem) {
                        2 -> 1
                        3 -> 2
                        else -> 0
                    }
                }
            )
            firstLaunch = false
        }
        return if (loggedIn) {
            when (position) {
                0 -> FollowedStreamsFragment()
                1 -> FollowedVideosFragment()
                2 -> FollowedChannelsFragment()
                else -> FollowedGamesFragment()
            }
        } else {
            when (position) {
                0 -> FollowedStreamsFragment()
                1 -> FollowedChannelsFragment()
                else -> FollowedGamesFragment()
            }
        }
    }
}