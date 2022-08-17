package com.github.andreyasadchy.xtra.ui.games

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.github.andreyasadchy.xtra.ui.Utils
import com.github.andreyasadchy.xtra.ui.clips.common.ClipsFragment
import com.github.andreyasadchy.xtra.ui.common.MediaFragment
import com.github.andreyasadchy.xtra.ui.main.MainActivity
import com.github.andreyasadchy.xtra.ui.streams.common.StreamsFragment
import com.github.andreyasadchy.xtra.ui.videos.game.GameVideosFragment
import com.github.andreyasadchy.xtra.util.C
import com.github.andreyasadchy.xtra.util.visible
import kotlinx.android.synthetic.main.fragment_media.*

class GameFragment : MediaFragment() {

    private val args: GameFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        args.let {
            arguments = Bundle().apply {
                putString(C.GAME_ID, it.gameId)
                putString(C.GAME_NAME, it.gameName)
                putStringArray(C.TAGS, it.tags)
                putBoolean(C.CHANNEL_UPDATELOCAL, it.updateLocal)
            }
        }
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity() as MainActivity
        if (!requireArguments().getString(C.GAME_ID).isNullOrBlank() || !requireArguments().getString(C.GAME_NAME).isNullOrBlank()) {
            toolbar.apply {
                title = requireArguments().getString(C.GAME_NAME)
                navigationIcon = Utils.getNavigationIcon(activity)
                setNavigationOnClickListener { activity.popFragment() }
            }
            spinner.visible()
            spinner.adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_dropdown_item, spinnerItems)
            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    currentFragment = if (position != previousItem && isResumed) {
                        val newFragment = onSpinnerItemSelected(position)
                        childFragmentManager.beginTransaction().replace(com.github.andreyasadchy.xtra.R.id.fragmentContainer, newFragment).commit()
                        previousItem = position
                        newFragment
                    } else {
                        childFragmentManager.findFragmentById(com.github.andreyasadchy.xtra.R.id.fragmentContainer)
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        } else {
            currentFragment = if (previousItem != 0) {
                val newFragment = onSpinnerItemSelected(0)
                childFragmentManager.beginTransaction().replace(com.github.andreyasadchy.xtra.R.id.fragmentContainer, newFragment).commit()
                previousItem = 0
                newFragment
            } else {
                childFragmentManager.findFragmentById(com.github.andreyasadchy.xtra.R.id.fragmentContainer)
            }
        }
    }

    override fun onSpinnerItemSelected(position: Int): Fragment {
        val fragment: Fragment = when (position) {
            0 -> StreamsFragment()
            1 -> GameVideosFragment()
            else -> ClipsFragment()
        }
        return fragment.also { it.arguments = requireArguments() }
    }
}