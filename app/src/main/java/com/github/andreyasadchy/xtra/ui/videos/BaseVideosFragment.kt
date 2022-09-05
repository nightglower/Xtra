package com.github.andreyasadchy.xtra.ui.videos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.github.andreyasadchy.xtra.databinding.FragmentStreamsBinding
import com.github.andreyasadchy.xtra.model.helix.video.Video
import com.github.andreyasadchy.xtra.ui.common.PagedListFragment
import com.github.andreyasadchy.xtra.ui.download.HasDownloadDialog
import com.github.andreyasadchy.xtra.ui.download.VideoDownloadDialogDirections

abstract class BaseVideosFragment : PagedListFragment(), HasDownloadDialog {

    interface OnVideoSelectedListener {
        fun startVideo(video: Video, offset: Double? = null)
    }

    private var _binding: FragmentStreamsBinding? = null
    protected val binding get() = _binding!!

    var lastSelectedItem: Video? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStreamsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun showDownloadDialog() {
        lastSelectedItem?.let {
            findNavController().navigate(VideoDownloadDialogDirections.actionGlobalVideoDownloadDialog(video = it))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}