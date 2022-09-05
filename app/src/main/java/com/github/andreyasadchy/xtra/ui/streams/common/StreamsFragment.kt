package com.github.andreyasadchy.xtra.ui.streams.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.andreyasadchy.xtra.databinding.FragmentStreamsBinding
import com.github.andreyasadchy.xtra.model.helix.stream.Sort
import com.github.andreyasadchy.xtra.model.helix.stream.Stream
import com.github.andreyasadchy.xtra.ui.common.PagedListFragment
import com.github.andreyasadchy.xtra.ui.common.follow.FollowFragment
import com.github.andreyasadchy.xtra.ui.main.MainActivity
import com.github.andreyasadchy.xtra.ui.search.tags.TagSearchFragmentDirections
import com.github.andreyasadchy.xtra.ui.streams.StreamsAdapter
import com.github.andreyasadchy.xtra.ui.streams.StreamsCompactAdapter
import com.github.andreyasadchy.xtra.util.C
import com.github.andreyasadchy.xtra.util.prefs
import com.github.andreyasadchy.xtra.util.visible
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StreamsFragment : PagedListFragment(), StreamsSortDialog.OnFilter, FollowFragment {

    interface OnStreamSelectedListener {
        fun startStream(stream: Stream)
    }

    private var _binding: FragmentStreamsBinding? = null
    private val binding get() = _binding!!
    private val args: StreamsFragmentArgs by navArgs()
    private val viewModel: StreamsViewModel by viewModels()
    private lateinit var pagingAdapter: PagingDataAdapter<Stream, out RecyclerView.ViewHolder>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStreamsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun initialize() {
        pagingAdapter = if (!viewModel.compactAdapter) {
            StreamsAdapter(this, requireActivity() as MainActivity, args)
        } else {
            StreamsCompactAdapter(this, requireActivity() as MainActivity, args)
        }
        with(binding) {
            init(recyclerViewLayout, pagingAdapter, viewModel.flow)
            sortBar.root.visible()
            if (args.gameId == null && args.gameName == null) {
                binding.sortBar.root.setOnClickListener { findNavController().navigate(TagSearchFragmentDirections.actionGlobalTagSearchFragment(getGameTags = false)) }
            } else {
                sortBar.root.setOnClickListener {
                    findNavController().navigate(StreamsSortDialogDirections.actionGlobalStreamsSortDialog(
                        sort = viewModel.filter.value.sort
                    ))
                }
                (requireContext().prefs().getString(C.UI_FOLLOW_BUTTON, "0")?.toInt() ?: 0).let { setting ->
                    if (setting < 2) {
                        /*parentFragment?.followGame?.let {
                            initializeFollow(
                                fragment = this@StreamsFragment,
                                viewModel = viewModel,
                                followButton = it,
                                setting = setting
                            )
                        }*/
                    }
                }
                if (args.updateLocal) {
                    viewModel.updateLocalGame(requireContext())
                }
            }
        }
    }

    override fun onChange(sort: Sort) {
        //adapter.submitList(null)
        viewModel.filter(
            sort = sort
        )
    }

    override fun onNetworkRestored() {
        pagingAdapter.retry()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}