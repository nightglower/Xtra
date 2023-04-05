package com.github.andreyasadchy.xtra.ui.search.videos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.andreyasadchy.xtra.databinding.CommonRecyclerViewLayoutBinding
import com.github.andreyasadchy.xtra.model.ui.Video
import com.github.andreyasadchy.xtra.ui.search.Searchable
import com.github.andreyasadchy.xtra.ui.videos.BaseVideosAdapter
import com.github.andreyasadchy.xtra.ui.videos.BaseVideosFragment
import com.github.andreyasadchy.xtra.ui.videos.VideosAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class VideoSearchFragment : BaseVideosFragment(), Searchable {

    private var _binding: CommonRecyclerViewLayoutBinding? = null
    private val binding get() = _binding!!
    private val viewModel: VideoSearchViewModel by viewModels()
    private lateinit var pagingAdapter: PagingDataAdapter<Video, out RecyclerView.ViewHolder>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = CommonRecyclerViewLayoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        pagingAdapter = VideosAdapter(this, {
            lastSelectedItem = it
            showDownloadDialog()
        }, {
            lastSelectedItem = it
            viewModel.saveBookmark(requireContext(), it)
        })
        setAdapter(binding.recyclerView, pagingAdapter)
    }

    override fun initialize() {
        with(binding) {
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.flow.collectLatest { pagingData ->
                    pagingAdapter.submitData(pagingData)
                }
            }
            viewLifecycleOwner.lifecycleScope.launch {
                pagingAdapter.loadStateFlow.collectLatest { loadState ->
                    progressBar.isVisible = loadState.refresh is LoadState.Loading && pagingAdapter.itemCount == 0
                    nothingHere.isVisible = loadState.refresh !is LoadState.Loading && pagingAdapter.itemCount == 0 && viewModel.query.value.isNotBlank()
                }
            }
        }
        initializeVideoAdapter(viewModel, pagingAdapter as BaseVideosAdapter)
    }

    override fun search(query: String) {
        viewModel.setQuery(query)
    }

    override fun onNetworkRestored() {
        pagingAdapter.retry()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}