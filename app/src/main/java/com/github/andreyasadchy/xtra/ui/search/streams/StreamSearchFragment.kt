package com.github.andreyasadchy.xtra.ui.search.streams

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.andreyasadchy.xtra.databinding.FragmentStreamsBinding
import com.github.andreyasadchy.xtra.model.helix.stream.Stream
import com.github.andreyasadchy.xtra.ui.common.PagedListFragment
import com.github.andreyasadchy.xtra.ui.main.MainActivity
import com.github.andreyasadchy.xtra.ui.search.Searchable
import com.github.andreyasadchy.xtra.ui.streams.StreamsAdapter
import com.github.andreyasadchy.xtra.ui.streams.StreamsCompactAdapter
import com.github.andreyasadchy.xtra.util.gone
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StreamSearchFragment : PagedListFragment(), Searchable {

    private var _binding: FragmentStreamsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: StreamSearchViewModel by viewModels()
    private lateinit var pagingAdapter: PagingDataAdapter<Stream, out RecyclerView.ViewHolder>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStreamsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun initialize() {
        pagingAdapter = if (!viewModel.compactAdapter) {
            StreamsAdapter(this, requireActivity() as MainActivity)
        } else {
            StreamsCompactAdapter(this, requireActivity() as MainActivity)
        }
        with(binding) {
            recyclerViewLayout.swipeRefresh.isEnabled = false
            init(recyclerViewLayout, pagingAdapter, viewModel.flow)
        }
    }

    override fun search(query: String) {
        if (query.isNotEmpty()) { //TODO same query doesn't fire
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