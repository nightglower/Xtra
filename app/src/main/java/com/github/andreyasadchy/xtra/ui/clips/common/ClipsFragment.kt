package com.github.andreyasadchy.xtra.ui.clips.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.databinding.FragmentStreamsBinding
import com.github.andreyasadchy.xtra.model.helix.clip.Clip
import com.github.andreyasadchy.xtra.model.helix.video.BroadcastType
import com.github.andreyasadchy.xtra.model.helix.video.Period
import com.github.andreyasadchy.xtra.model.helix.video.Sort
import com.github.andreyasadchy.xtra.ui.clips.ClipsAdapter
import com.github.andreyasadchy.xtra.ui.common.PagedListFragment
import com.github.andreyasadchy.xtra.ui.common.follow.FollowFragment
import com.github.andreyasadchy.xtra.ui.download.ClipDownloadDialogDirections
import com.github.andreyasadchy.xtra.ui.download.HasDownloadDialog
import com.github.andreyasadchy.xtra.ui.main.MainActivity
import com.github.andreyasadchy.xtra.ui.videos.VideosSortDialog
import com.github.andreyasadchy.xtra.ui.videos.VideosSortDialogDirections
import com.github.andreyasadchy.xtra.util.C
import com.github.andreyasadchy.xtra.util.prefs
import com.github.andreyasadchy.xtra.util.visible
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ClipsFragment : PagedListFragment(), VideosSortDialog.OnFilter, HasDownloadDialog, FollowFragment {

    interface OnClipSelectedListener {
        fun startClip(clip: Clip)
    }

    private var _binding: FragmentStreamsBinding? = null
    private val binding get() = _binding!!
    private val args: ClipsFragmentArgs by navArgs()
    private val viewModel: ClipsViewModel by viewModels()
    private lateinit var pagingAdapter: PagingDataAdapter<Clip, out RecyclerView.ViewHolder>

    var lastSelectedItem: Clip? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStreamsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun initialize() {
        pagingAdapter = if (args.channelId != null || args.channelLogin != null) {
            ChannelClipsAdapter(this, requireActivity() as MainActivity) {
                lastSelectedItem = it
                showDownloadDialog()
            }
        } else {
            ClipsAdapter(this, requireActivity() as MainActivity) {
                lastSelectedItem = it
                showDownloadDialog()
            }
        }
        with(binding) {
            viewModel.loadClips(requireContext())
            init(recyclerViewLayout, pagingAdapter, viewModel.flow)
            viewModel.sortText.observe(viewLifecycleOwner) {
                sortBar.sortText.text = it
            }
            sortBar.root.visible()
            sortBar.root.setOnClickListener {
                with(viewModel.filter.value) {
                    findNavController().navigate(VideosSortDialogDirections.actionGlobalVideosSortDialog(
                        period = period,
                        languageIndex = languageIndex,
                        clipChannel = args.channelId != null,
                        saveSort = saveSort,
                        saveDefault = if (pagingAdapter is ClipsAdapter) requireContext().prefs().getBoolean(C.SORT_DEFAULT_GAME_CLIPS, false) else requireContext().prefs().getBoolean(C.SORT_DEFAULT_CHANNEL_CLIPS, false)
                    ))
                }
            }
            if (pagingAdapter is ClipsAdapter) {
                (requireContext().prefs().getString(C.UI_FOLLOW_BUTTON, "0")?.toInt() ?: 0).let { setting ->
                    if (setting < 2) {
                        /*parentFragment?.followGame?.let {
                            initializeFollow(
                                fragment = this@ClipsFragment,
                                viewModel = viewModel,
                                followButton = it,
                                setting = setting
                            )
                        }*/
                    }
                }
            }
        }
    }

    override fun onChange(sort: Sort, sortText: CharSequence, period: Period, periodText: CharSequence, type: BroadcastType, languageIndex: Int, saveSort: Boolean, saveDefault: Boolean) {
        //adapter.submitList(null)
        viewModel.filter(
            context = requireContext(),
            period = period,
            languageIndex = languageIndex,
            text = getString(R.string.sort_and_period, sortText, periodText),
            saveSort = saveSort,
            saveDefault = saveDefault
        )
    }

    override fun showDownloadDialog() {
        lastSelectedItem?.let {
            findNavController().navigate(ClipDownloadDialogDirections.actionGlobalClipDownloadDialog(clip = it))
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
