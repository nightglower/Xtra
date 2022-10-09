package com.github.andreyasadchy.xtra.ui.clips.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.viewModels
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.databinding.FragmentStreamsBinding
import com.github.andreyasadchy.xtra.model.User
import com.github.andreyasadchy.xtra.model.helix.clip.Clip
import com.github.andreyasadchy.xtra.model.helix.video.BroadcastType
import com.github.andreyasadchy.xtra.model.helix.video.Period
import com.github.andreyasadchy.xtra.model.helix.video.Sort
import com.github.andreyasadchy.xtra.ui.clips.BaseClipsFragment
import com.github.andreyasadchy.xtra.ui.clips.ClipsAdapter
import com.github.andreyasadchy.xtra.ui.common.follow.FollowFragment
import com.github.andreyasadchy.xtra.ui.main.MainActivity
import com.github.andreyasadchy.xtra.ui.videos.VideosSortDialog
import com.github.andreyasadchy.xtra.util.C
import com.github.andreyasadchy.xtra.util.TwitchApiHelper
import com.github.andreyasadchy.xtra.util.prefs
import com.github.andreyasadchy.xtra.util.visible
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ClipsFragment : BaseClipsFragment<ClipsViewModel>(), VideosSortDialog.OnFilter, FollowFragment {

    override val baseBinding get() = binding.recyclerViewLayout
    private var _binding: FragmentStreamsBinding? = null
    private val binding get() = _binding!!
    override val viewModel: ClipsViewModel by viewModels()
    override val adapter by lazy {
        val activity = requireActivity() as MainActivity
        val showDialog: (Clip) -> Unit = {
            lastSelectedItem = it
            showDownloadDialog()
        }
        if (arguments?.getString(C.CHANNEL_ID) != null || arguments?.getString(C.CHANNEL_LOGIN) != null) {
            ChannelClipsAdapter(this, activity, activity, showDialog)
        } else {
            ClipsAdapter(this, activity, activity, activity, showDialog)
        }
    }
    var followButton: ImageButton? = null

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
            viewModel.loadClips(
                context = requireContext(),
                channelId = arguments?.getString(C.CHANNEL_ID),
                channelLogin = arguments?.getString(C.CHANNEL_LOGIN),
                gameId = arguments?.getString(C.GAME_ID),
                gameName = arguments?.getString(C.GAME_NAME),
                helixClientId = requireContext().prefs().getString(C.HELIX_CLIENT_ID, ""),
                helixToken = User.get(requireContext()).helixToken,
                gqlClientId = requireContext().prefs().getString(C.GQL_CLIENT_ID, ""),
                channelApiPref = TwitchApiHelper.listFromPrefs(requireContext().prefs().getString(C.API_PREF_CHANNEL_CLIPS, ""), TwitchApiHelper.channelClipsApiDefaults),
                gameApiPref = TwitchApiHelper.listFromPrefs(requireContext().prefs().getString(C.API_PREF_GAME_CLIPS, ""), TwitchApiHelper.gameClipsApiDefaults)
            )
            sortBar.root.visible()
            sortBar.root.setOnClickListener {
                VideosSortDialog.newInstance(
                    period = viewModel.period,
                    languageIndex = viewModel.languageIndex,
                    clipChannel = arguments?.getString(C.CHANNEL_ID) != null,
                    saveSort = viewModel.saveSort,
                    saveDefault = if (adapter is ClipsAdapter) requireContext().prefs().getBoolean(C.SORT_DEFAULT_GAME_CLIPS, false) else requireContext().prefs().getBoolean(C.SORT_DEFAULT_CHANNEL_CLIPS, false)
                ).show(childFragmentManager, null)
            }
            val activity = requireActivity() as MainActivity
            if (adapter is ClipsAdapter && (requireContext().prefs().getString(C.UI_FOLLOW_BUTTON, "0")?.toInt() ?: 0) < 2) {
                followButton?.let {
                    initializeFollow(
                        fragment = this@ClipsFragment,
                        viewModel = viewModel,
                        followButton = it,
                        setting = requireContext().prefs().getString(C.UI_FOLLOW_BUTTON, "0")?.toInt() ?: 0,
                        user = User.get(activity),
                        helixClientId = requireContext().prefs().getString(C.HELIX_CLIENT_ID, ""),
                        gqlClientId = requireContext().prefs().getString(C.GQL_CLIENT_ID, "")
                    )
                }
            }
        }
    }

    override fun onChange(sort: Sort, sortText: CharSequence, period: Period, periodText: CharSequence, type: BroadcastType, languageIndex: Int, saveSort: Boolean, saveDefault: Boolean) {
        adapter.submitList(null)
        viewModel.filter(
            period = period,
            languageIndex = languageIndex,
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
