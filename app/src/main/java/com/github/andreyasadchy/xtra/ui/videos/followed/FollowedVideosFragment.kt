package com.github.andreyasadchy.xtra.ui.videos.followed

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
import com.github.andreyasadchy.xtra.ui.videos.VideosAdapter
import com.github.andreyasadchy.xtra.ui.videos.VideosSortDialog
import com.github.andreyasadchy.xtra.util.C
import com.github.andreyasadchy.xtra.util.TwitchApiHelper
import com.github.andreyasadchy.xtra.util.prefs
import com.github.andreyasadchy.xtra.util.visible
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FollowedVideosFragment : BaseVideosFragment<FollowedVideosViewModel>(), VideosSortDialog.OnFilter {

    override val baseBinding get() = binding.recyclerViewLayout
    private var _binding: FragmentStreamsBinding? = null
    private val binding get() = _binding!!
    override val viewModel: FollowedVideosViewModel by viewModels()
    override val adapter by lazy {
        val activity = requireActivity() as MainActivity
        VideosAdapter(this, activity, activity, activity, {
            lastSelectedItem = it
            showDownloadDialog()
        }, {
            lastSelectedItem = it
            viewModel.saveBookmark(
                context = requireContext(),
                video = it)
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
            viewModel.setUser(
                context = requireContext(),
                user = User.get(requireContext()),
                gqlClientId = requireContext().prefs().getString(C.GQL_CLIENT_ID, ""),
                apiPref = TwitchApiHelper.listFromPrefs(requireContext().prefs().getString(C.API_PREF_FOLLOWED_VIDEOS, ""), TwitchApiHelper.followedVideosApiDefaults)
            )
            sortBar.root.visible()
            sortBar.root.setOnClickListener {
                VideosSortDialog.newInstance(
                    sort = viewModel.sort,
                    period = viewModel.period,
                    type = viewModel.type,
                    saveDefault = requireContext().prefs().getBoolean(C.SORT_DEFAULT_FOLLOWED_VIDEOS, false)
                ).show(childFragmentManager, null)
            }
        }
    }

    override fun onChange(sort: Sort, sortText: CharSequence, period: Period, periodText: CharSequence, type: BroadcastType, languageIndex: Int, saveSort: Boolean, saveDefault: Boolean) {
        adapter.submitList(null)
        viewModel.filter(
            sort = sort,
            period = period,
            type = type,
            text = getString(R.string.sort_and_period, sortText, periodText),
            saveDefault = saveDefault
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
