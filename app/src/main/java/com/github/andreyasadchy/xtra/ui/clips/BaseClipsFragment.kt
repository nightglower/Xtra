package com.github.andreyasadchy.xtra.ui.clips

import com.github.andreyasadchy.xtra.databinding.CommonRecyclerViewLayoutBinding
import com.github.andreyasadchy.xtra.model.helix.clip.Clip
import com.github.andreyasadchy.xtra.ui.common.PagedListFragment
import com.github.andreyasadchy.xtra.ui.common.PagedListViewModel
import com.github.andreyasadchy.xtra.ui.common.Scrollable
import com.github.andreyasadchy.xtra.ui.download.ClipDownloadDialog
import com.github.andreyasadchy.xtra.ui.download.HasDownloadDialog

abstract class BaseClipsFragment<VM : PagedListViewModel<Clip>> : PagedListFragment<Clip, VM>(), Scrollable, HasDownloadDialog {

    interface OnClipSelectedListener {
        fun startClip(clip: Clip)
    }

    override val pagedListBinding get() = binding
    abstract val baseBinding: CommonRecyclerViewLayoutBinding
    private val binding get() = baseBinding
    var lastSelectedItem: Clip? = null

    override fun scrollToTop() {
        binding.recyclerView.scrollToPosition(0)
    }

    override fun showDownloadDialog() {
        lastSelectedItem?.let {
            ClipDownloadDialog.newInstance(it).show(childFragmentManager, null)
        }
    }
}
