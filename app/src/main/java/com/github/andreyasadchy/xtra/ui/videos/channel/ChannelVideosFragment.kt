package com.github.andreyasadchy.xtra.ui.videos.channel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.databinding.FragmentStreamsBinding
import com.github.andreyasadchy.xtra.model.User
import com.github.andreyasadchy.xtra.model.helix.video.BroadcastType
import com.github.andreyasadchy.xtra.model.helix.video.Period
import com.github.andreyasadchy.xtra.model.helix.video.Sort
import com.github.andreyasadchy.xtra.ui.main.MainActivity
import com.github.andreyasadchy.xtra.ui.videos.BaseVideosFragment
import com.github.andreyasadchy.xtra.ui.videos.VideosSortDialog
import com.github.andreyasadchy.xtra.util.C
import com.github.andreyasadchy.xtra.util.TwitchApiHelper
import com.github.andreyasadchy.xtra.util.prefs
import com.github.andreyasadchy.xtra.util.visible
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChannelVideosFragment : BaseVideosFragment<ChannelVideosViewModel>(), VideosSortDialog.OnFilter {

    override val baseBinding get() = binding.recyclerViewLayout
    private var _binding: FragmentStreamsBinding? = null
    private val binding get() = _binding!!
    override val viewModel: ChannelVideosViewModel by viewModels()
    override val adapter by lazy {
        ChannelVideosAdapter(this, requireActivity() as MainActivity, requireActivity() as MainActivity, {
            lastSelectedItem = it
            showDownloadDialog()
        }, {
            lastSelectedItem = it
            viewModel.saveBookmark(requireContext(), it)
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStreamsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun initialize() {
        super.initialize()
        with(binding) {
            viewModel.sortText.observe(viewLifecycleOwner) {
                sortBar.sortText.text = it
            }
            viewModel.setChannelId(
                context = requireContext(),
                channelId = requireArguments().getString(C.CHANNEL_ID),
                channelLogin = requireArguments().getString(C.CHANNEL_LOGIN),
                helixClientId = requireContext().prefs().getString(C.HELIX_CLIENT_ID, ""),
                helixToken = User.get(requireContext()).helixToken,
                gqlClientId = requireContext().prefs().getString(C.GQL_CLIENT_ID, ""),
                apiPref = TwitchApiHelper.listFromPrefs(requireContext().prefs().getString(C.API_PREF_CHANNEL_VIDEOS, ""), TwitchApiHelper.channelVideosApiDefaults)
            )
            sortBar.root.visible()
            sortBar.root.setOnClickListener {
                VideosSortDialog.newInstance(
                    sort = viewModel.sort,
                    period = viewModel.period,
                    type = viewModel.type,
                    saveSort = viewModel.saveSort,
                    saveDefault = requireContext().prefs().getBoolean(C.SORT_DEFAULT_CHANNEL_VIDEOS, false)
                ).show(childFragmentManager, null)
            }
        }
    }

    override fun onChange(sort: Sort, sortText: CharSequence, period: Period, periodText: CharSequence, type: BroadcastType, languageIndex: Int, saveSort: Boolean, saveDefault: Boolean) {
        adapter.submitList(null)
        viewModel.filter(
            sort = sort,
            type = type,
            text = getString(R.string.sort_and_period, sortText, periodText),
            saveSort = saveSort,
            saveDefault = saveDefault
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}