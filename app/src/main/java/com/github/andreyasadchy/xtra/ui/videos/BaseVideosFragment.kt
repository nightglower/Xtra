package com.github.andreyasadchy.xtra.ui.videos

import com.github.andreyasadchy.xtra.databinding.CommonRecyclerViewLayoutBinding
import com.github.andreyasadchy.xtra.model.helix.video.Video
import com.github.andreyasadchy.xtra.ui.common.PagedListFragment
import com.github.andreyasadchy.xtra.ui.common.Scrollable
import com.github.andreyasadchy.xtra.ui.download.HasDownloadDialog
import com.github.andreyasadchy.xtra.ui.download.VideoDownloadDialog
import com.github.andreyasadchy.xtra.util.C
import com.github.andreyasadchy.xtra.util.prefs

abstract class BaseVideosFragment<VM : BaseVideosViewModel> : PagedListFragment<Video, VM>(), Scrollable, HasDownloadDialog {

    interface OnVideoSelectedListener {
        fun startVideo(video: Video, offset: Double? = null)
    }

    override val pagedListBinding get() = binding
    abstract val baseBinding: CommonRecyclerViewLayoutBinding
    private val binding get() = baseBinding
    var lastSelectedItem: Video? = null

    override fun initialize() {
        super.initialize()
        if (requireContext().prefs().getBoolean(C.PLAYER_USE_VIDEOPOSITIONS, true)) {
            viewModel.positions.observe(viewLifecycleOwner) {
                (adapter as? BaseVideosAdapter)?.setVideoPositions(it)
            }
        }
        viewModel.bookmarks.observe(viewLifecycleOwner) {
            (adapter as? BaseVideosAdapter)?.setBookmarksList(it)
        }
    }

    override fun scrollToTop() {
        binding.recyclerView.scrollToPosition(0)
    }

    override fun showDownloadDialog() {
        lastSelectedItem?.let {
            VideoDownloadDialog.newInstance(it).show(childFragmentManager, null)
        }
    }
}