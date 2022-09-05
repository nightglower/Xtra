package com.github.andreyasadchy.xtra.ui.search.games

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.github.andreyasadchy.xtra.databinding.FragmentStreamsBinding
import com.github.andreyasadchy.xtra.ui.common.PagedListFragment
import com.github.andreyasadchy.xtra.ui.games.GamesAdapter
import com.github.andreyasadchy.xtra.ui.search.Searchable
import com.github.andreyasadchy.xtra.util.gone
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GameSearchFragment : PagedListFragment(), Searchable {

    private var _binding: FragmentStreamsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: GameSearchViewModel by viewModels()
    private val pagingAdapter = GamesAdapter(this)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStreamsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun initialize() {
        with(binding) {
            recyclerViewLayout.swipeRefresh.isEnabled = false
            init(recyclerViewLayout, pagingAdapter, viewModel.flow)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}