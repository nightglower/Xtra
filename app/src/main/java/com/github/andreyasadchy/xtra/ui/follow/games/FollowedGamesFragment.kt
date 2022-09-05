package com.github.andreyasadchy.xtra.ui.follow.games

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.github.andreyasadchy.xtra.databinding.FragmentStreamsBinding
import com.github.andreyasadchy.xtra.ui.common.PagedListFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FollowedGamesFragment : PagedListFragment() {

    private var _binding: FragmentStreamsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FollowedGamesViewModel by viewModels()
    private val pagingAdapter = FollowedGamesAdapter(this)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStreamsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun initialize() {
        with(binding) {
            init(recyclerViewLayout, pagingAdapter, viewModel.flow)
        }
    }

    override fun onNetworkRestored() {
        pagingAdapter.retry()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}