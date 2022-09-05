package com.github.andreyasadchy.xtra.ui.search.videos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.databinding.CommonRecyclerViewLayoutBinding
import com.github.andreyasadchy.xtra.model.helix.video.Video
import com.github.andreyasadchy.xtra.ui.main.MainActivity
import com.github.andreyasadchy.xtra.ui.search.Searchable
import com.github.andreyasadchy.xtra.ui.search.channels.ChannelSearchAdapter
import com.github.andreyasadchy.xtra.ui.search.channels.ChannelSearchViewModel
import com.github.andreyasadchy.xtra.ui.videos.BaseVideosAdapter
import com.github.andreyasadchy.xtra.ui.videos.BaseVideosFragment
import com.github.andreyasadchy.xtra.ui.videos.VideosAdapter
import com.github.andreyasadchy.xtra.util.C
import com.github.andreyasadchy.xtra.util.TwitchApiHelper
import com.github.andreyasadchy.xtra.util.gone
import com.github.andreyasadchy.xtra.util.prefs
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class VideoSearchFragment : BaseVideosFragment(), Searchable {

    private val viewModel: VideoSearchViewModel by viewModels()
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
            recyclerViewLayout.swipeRefresh.isEnabled = false
            init(recyclerViewLayout, pagingAdapter, viewModel.flow)
            if (requireContext().prefs().getBoolean(C.PLAYER_USE_VIDEOPOSITIONS, true)) {
                viewModel.positions.observe(viewLifecycleOwner) {
                    pagingAdapter.setVideoPositions(it)
                }
            }
            viewModel.bookmarks.observe(viewLifecycleOwner) {
                pagingAdapter.setBookmarksList(it)
            }
        }
    }

    override fun search(query: String) {
        if (query.isNotEmpty()) {
            viewModel.setQuery(query = query)
        } else {
            binding.recyclerViewLayout.nothingHere.gone()
        }
    }

    override fun onNetworkRestored() {
        pagingAdapter.retry()
    }
}