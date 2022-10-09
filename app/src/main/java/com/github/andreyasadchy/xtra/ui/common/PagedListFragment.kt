package com.github.andreyasadchy.xtra.ui.common

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.github.andreyasadchy.xtra.databinding.CommonRecyclerViewLayoutBinding
import com.github.andreyasadchy.xtra.repository.LoadingState
import com.github.andreyasadchy.xtra.ui.follow.FollowMediaFragment
import com.github.andreyasadchy.xtra.ui.follow.FollowPagerFragment
import com.github.andreyasadchy.xtra.ui.saved.SavedMediaFragment
import com.github.andreyasadchy.xtra.ui.saved.SavedPagerFragment
import com.github.andreyasadchy.xtra.ui.search.SearchFragment
import com.github.andreyasadchy.xtra.ui.search.tags.BaseTagSearchFragment
import com.github.andreyasadchy.xtra.ui.top.TopFragment
import com.github.andreyasadchy.xtra.util.C
import com.github.andreyasadchy.xtra.util.gone
import com.github.andreyasadchy.xtra.util.prefs

abstract class PagedListFragment<T, VM : PagedListViewModel<T>> : BaseNetworkFragment() {

    abstract val pagedListBinding: CommonRecyclerViewLayoutBinding
    private val binding get() = pagedListBinding
    protected abstract val viewModel: VM
    protected abstract val adapter: BasePagedListAdapter<T, out RecyclerView.ViewHolder?>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {

                override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                    adapter.unregisterAdapterDataObserver(this)
                    adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
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
            if (!requireContext().prefs().getBoolean(C.UI_SCROLLTOP, true) || parentFragment is TopFragment || parentFragment is FollowMediaFragment || parentFragment is FollowPagerFragment || parentFragment is SavedMediaFragment || parentFragment is SavedPagerFragment || parentFragment is SearchFragment || parentFragment is BaseTagSearchFragment) {
                scrollTop.isEnabled = false
            }
            recyclerView.let {
                it.adapter = adapter
                if (scrollTop.isEnabled) {
                    it.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                            super.onScrollStateChanged(recyclerView, newState)
                            scrollTop.isVisible = shouldShowButton()
                        }
                    })
                }
            }
        }
    }

    private fun shouldShowButton(): Boolean {
        with(binding) {
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

    override fun initialize() {
        with(binding) {
            viewModel.list.observe(viewLifecycleOwner) {
                adapter.submitList(it)
                nothingHere.isVisible = it.isEmpty()
            }
            viewModel.loadingState.observe(viewLifecycleOwner) {
                val isLoading = it == LoadingState.LOADING
                val isListEmpty = adapter.currentList.isNullOrEmpty()
                if (isLoading) {
                    nothingHere.gone()
                }
                progressBar.isVisible = isLoading && isListEmpty
                if (swipeRefresh.isEnabled) {
                    swipeRefresh.isRefreshing = isLoading && !isListEmpty
                }
            }
            viewModel.pagingState.observe(viewLifecycleOwner, Observer(adapter::setPagingState))
            if (swipeRefresh.isEnabled) {
                swipeRefresh.setOnRefreshListener { viewModel.refresh() }
            }
            if (scrollTop.isEnabled) {
                scrollTop.setOnClickListener {
                    (parentFragment as? Scrollable)?.scrollToTop()
                    it.gone()
                }
            }
        }
    }

    override fun onNetworkRestored() {
        viewModel.retry()
    }
}