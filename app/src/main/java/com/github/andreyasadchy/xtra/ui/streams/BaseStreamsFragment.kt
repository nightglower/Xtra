package com.github.andreyasadchy.xtra.ui.streams

import com.github.andreyasadchy.xtra.databinding.CommonRecyclerViewLayoutBinding
import com.github.andreyasadchy.xtra.model.helix.stream.Stream
import com.github.andreyasadchy.xtra.ui.common.PagedListFragment
import com.github.andreyasadchy.xtra.ui.common.PagedListViewModel
import com.github.andreyasadchy.xtra.ui.common.Scrollable

abstract class BaseStreamsFragment<VM : PagedListViewModel<Stream>> : PagedListFragment<Stream, VM>(), Scrollable {

    interface OnStreamSelectedListener {
        fun startStream(stream: Stream)
    }

    override val pagedListBinding get() = binding
    abstract val baseBinding: CommonRecyclerViewLayoutBinding
    private val binding get() = baseBinding

    override fun scrollToTop() {
        binding.recyclerView.scrollToPosition(0)
    }
}