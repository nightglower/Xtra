package com.github.andreyasadchy.xtra.ui.search.tags

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.github.andreyasadchy.xtra.databinding.FragmentStreamsBinding
import com.github.andreyasadchy.xtra.ui.common.PagedListFragment
import com.github.andreyasadchy.xtra.ui.search.Searchable
import com.github.andreyasadchy.xtra.util.gone
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TagSearchFragment : PagedListFragment(), Searchable {

    private var _binding: FragmentStreamsBinding? = null
    private val binding get() = _binding!!
    private val args: TagSearchFragmentArgs by navArgs()
    private val viewModel: TagSearchViewModel by viewModels()
    private val pagingAdapter = TagSearchAdapter(this, args)

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