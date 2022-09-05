package com.github.andreyasadchy.xtra.ui.common

import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.andreyasadchy.xtra.databinding.CommonRecyclerViewLayoutBinding
import com.github.andreyasadchy.xtra.util.C
import com.github.andreyasadchy.xtra.util.gone
import com.github.andreyasadchy.xtra.util.prefs
import com.github.andreyasadchy.xtra.util.visible
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

abstract class PagedListFragment : BaseNetworkFragment() {

    fun <T : Any, VH : RecyclerView.ViewHolder> init(binding: CommonRecyclerViewLayoutBinding, pagingAdapter: PagingDataAdapter<T, VH>, flow: Flow<PagingData<T>>) {
        with(binding) {
            pagingAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {

                override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                    pagingAdapter.unregisterAdapterDataObserver(this)
                    pagingAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
                        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                            try {
                                if (positionStart == 0) {
                                    recyclerView.scrollToPosition(0)
                                }
                            } catch (e: Exception) {

                            }
                        }
                    })
                }
            })
            recyclerView.adapter = pagingAdapter
            viewLifecycleOwner.lifecycleScope.launch {
                flow.collectLatest { pagingData ->
                    pagingAdapter.submitData(pagingData)
                }
            }
            viewLifecycleOwner.lifecycleScope.launch {
                pagingAdapter.loadStateFlow.collectLatest { loadStates ->
                    val isLoading = loadStates.refresh is LoadState.Loading
                    val isListEmpty = pagingAdapter.itemCount == 0
                    if (!isLoading && isListEmpty) {
                        nothingHere.visible()
                    }
                    if (isLoading) {
                        nothingHere.gone()
                    }
                    progressBar.isVisible = isLoading && isListEmpty
                    if (swipeRefresh.isEnabled) {
                        swipeRefresh.isRefreshing = isLoading && !isListEmpty
                    }
                }
            }
            if (swipeRefresh.isEnabled) {
                swipeRefresh.setOnRefreshListener {
                    pagingAdapter.refresh()
                }
            }
            if (scrollTop.isEnabled && requireContext().prefs().getBoolean(C.UI_SCROLLTOP, true)) {
                recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        super.onScrollStateChanged(recyclerView, newState)
                        scrollTop.isVisible = shouldShowButton(recyclerView)
                    }
                })
                scrollTop.setOnClickListener {
                    recyclerView.scrollToPosition(0)
                    it.gone()
                }
            }
        }
    }

    private fun shouldShowButton(recyclerView: RecyclerView): Boolean {
        val offset = recyclerView.computeVerticalScrollOffset()
        if (offset < 0) {
            return false
        }
        val extent = recyclerView.computeVerticalScrollExtent()
        val range = recyclerView.computeVerticalScrollRange()
        val percentage = (100f * offset / (range - extent).toFloat())
        return percentage > 3f
    }
}