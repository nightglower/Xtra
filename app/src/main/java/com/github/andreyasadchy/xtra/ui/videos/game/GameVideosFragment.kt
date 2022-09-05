package com.github.andreyasadchy.xtra.ui.videos.game

import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.model.helix.video.BroadcastType
import com.github.andreyasadchy.xtra.model.helix.video.Period
import com.github.andreyasadchy.xtra.model.helix.video.Sort
import com.github.andreyasadchy.xtra.ui.common.follow.FollowFragment
import com.github.andreyasadchy.xtra.ui.main.MainActivity
import com.github.andreyasadchy.xtra.ui.videos.BaseVideosFragment
import com.github.andreyasadchy.xtra.ui.videos.VideosAdapter
import com.github.andreyasadchy.xtra.ui.videos.VideosSortDialog
import com.github.andreyasadchy.xtra.ui.videos.VideosSortDialogDirections
import com.github.andreyasadchy.xtra.util.C
import com.github.andreyasadchy.xtra.util.prefs
import com.github.andreyasadchy.xtra.util.visible
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GameVideosFragment : BaseVideosFragment(), VideosSortDialog.OnFilter, FollowFragment {

    private val viewModel: GameVideosViewModel by viewModels()
    private lateinit var pagingAdapter: VideosAdapter

    override fun initialize() {
        pagingAdapter = VideosAdapter(this, requireActivity() as MainActivity, {
            lastSelectedItem = it
            showDownloadDialog()
        }, {
            lastSelectedItem = it
            viewModel.saveBookmark(requireContext(), it)
        })
        with(binding) {
            viewModel.setGame(requireContext())
            init(recyclerViewLayout, pagingAdapter, viewModel.flow)
            if (requireContext().prefs().getBoolean(C.PLAYER_USE_VIDEOPOSITIONS, true)) {
                viewModel.positions.observe(viewLifecycleOwner) {
                    pagingAdapter.setVideoPositions(it)
                }
            }
            viewModel.bookmarks.observe(viewLifecycleOwner) {
                pagingAdapter.setBookmarksList(it)
            }
            viewModel.sortText.observe(viewLifecycleOwner) {
                sortBar.sortText.text = it
            }
            sortBar.root.visible()
            sortBar.root.setOnClickListener {
                with(viewModel.filter.value) {
                    findNavController().navigate(VideosSortDialogDirections.actionGlobalVideosSortDialog(
                        sort = sort,
                        period = period,
                        type = broadcastType,
                        languageIndex = languageIndex,
                        saveSort = saveSort,
                        saveDefault = requireContext().prefs().getBoolean(C.SORT_DEFAULT_GAME_VIDEOS, false)
                    ))
                }
            }
            (requireContext().prefs().getString(C.UI_FOLLOW_BUTTON, "0")?.toInt() ?: 0).let { setting ->
                if (setting < 2) {
                    /*parentFragment?.followGame?.let {
                        initializeFollow(
                            fragment = this@GameVideosFragment,
                            viewModel = viewModel,
                            followButton = it,
                            setting = setting
                        )
                    }*/
                }
            }
        }
    }

    override fun onChange(sort: Sort, sortText: CharSequence, period: Period, periodText: CharSequence, type: BroadcastType, languageIndex: Int, saveSort: Boolean, saveDefault: Boolean) {
        //adapter.submitList(null)
        viewModel.filter(
            context = requireContext(),
            sort = sort,
            period = period,
            type = type,
            languageIndex = languageIndex,
            text = getString(R.string.sort_and_period, sortText, periodText),
            saveSort = saveSort,
            saveDefault = saveDefault
        )
    }

    override fun onNetworkRestored() {
        pagingAdapter.retry()
    }
}
