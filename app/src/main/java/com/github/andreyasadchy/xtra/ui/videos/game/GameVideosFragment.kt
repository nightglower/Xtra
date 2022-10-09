package com.github.andreyasadchy.xtra.ui.videos.game

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.viewModels
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.databinding.FragmentStreamsBinding
import com.github.andreyasadchy.xtra.model.User
import com.github.andreyasadchy.xtra.model.helix.video.BroadcastType
import com.github.andreyasadchy.xtra.model.helix.video.Period
import com.github.andreyasadchy.xtra.model.helix.video.Sort
import com.github.andreyasadchy.xtra.ui.common.follow.FollowFragment
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
class GameVideosFragment : BaseVideosFragment<GameVideosViewModel>(), VideosSortDialog.OnFilter, FollowFragment {

    override val baseBinding get() = binding.recyclerViewLayout
    private var _binding: FragmentStreamsBinding? = null
    private val binding get() = _binding!!
    override val viewModel: GameVideosViewModel by viewModels()
    override val adapter by lazy {
        val activity = requireActivity() as MainActivity
        VideosAdapter(this, activity, activity, activity, {
            lastSelectedItem = it
            showDownloadDialog()
        }, {
            lastSelectedItem = it
            viewModel.saveBookmark(requireContext(), it)
        })
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
            viewModel.setGame(
                context = requireContext(),
                gameId = arguments?.getString(C.GAME_ID),
                gameName = arguments?.getString(C.GAME_NAME),
                helixClientId = requireContext().prefs().getString(C.HELIX_CLIENT_ID, ""),
                helixToken = User.get(requireContext()).helixToken,
                gqlClientId = requireContext().prefs().getString(C.GQL_CLIENT_ID, ""),
                apiPref = TwitchApiHelper.listFromPrefs(requireContext().prefs().getString(C.API_PREF_GAME_VIDEOS, ""), TwitchApiHelper.gameVideosApiDefaults)
            )
            sortBar.root.visible()
            sortBar.root.setOnClickListener {
                VideosSortDialog.newInstance(
                    sort = viewModel.sort,
                    period = viewModel.period,
                    type = viewModel.type,
                    languageIndex = viewModel.languageIndex,
                    saveSort = viewModel.saveSort,
                    saveDefault = requireContext().prefs().getBoolean(C.SORT_DEFAULT_GAME_VIDEOS, false)
                ).show(childFragmentManager, null)
            }
            val activity = requireActivity() as MainActivity
            if ((requireContext().prefs().getString(C.UI_FOLLOW_BUTTON, "0")?.toInt() ?: 0) < 2) {
                followButton?.let {
                    initializeFollow(
                        fragment = this@GameVideosFragment,
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
            sort = sort,
            period = period,
            type = type,
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
