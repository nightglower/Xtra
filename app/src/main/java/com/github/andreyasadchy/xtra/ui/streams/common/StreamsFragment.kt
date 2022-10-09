package com.github.andreyasadchy.xtra.ui.streams.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.viewModels
import com.github.andreyasadchy.xtra.databinding.FragmentStreamsBinding
import com.github.andreyasadchy.xtra.model.User
import com.github.andreyasadchy.xtra.model.helix.stream.Sort
import com.github.andreyasadchy.xtra.ui.common.follow.FollowFragment
import com.github.andreyasadchy.xtra.ui.main.MainActivity
import com.github.andreyasadchy.xtra.ui.streams.BaseStreamsFragment
import com.github.andreyasadchy.xtra.ui.streams.StreamsAdapter
import com.github.andreyasadchy.xtra.ui.streams.StreamsCompactAdapter
import com.github.andreyasadchy.xtra.util.C
import com.github.andreyasadchy.xtra.util.TwitchApiHelper
import com.github.andreyasadchy.xtra.util.prefs
import com.github.andreyasadchy.xtra.util.visible
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StreamsFragment : BaseStreamsFragment<StreamsViewModel>(), StreamsSortDialog.OnFilter, FollowFragment {

    override val baseBinding get() = binding.recyclerViewLayout
    private var _binding: FragmentStreamsBinding? = null
    private val binding get() = _binding!!
    override val viewModel: StreamsViewModel by viewModels()
    override val adapter by lazy {
        val activity = requireActivity() as MainActivity
        if (!compactStreams) {
            StreamsAdapter(this, activity, activity, activity)
        } else {
            StreamsCompactAdapter(this, activity, activity, activity)
        }
    }
    var followButton: ImageButton? = null
    private var compactStreams = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        compactStreams = requireContext().prefs().getBoolean(C.COMPACT_STREAMS, false)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStreamsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun initialize() {
        super.initialize()
        with(binding) {
            viewModel.loadStreams(
                gameId = arguments?.getString(C.GAME_ID),
                gameName = arguments?.getString(C.GAME_NAME),
                helixClientId = requireContext().prefs().getString(C.HELIX_CLIENT_ID, ""),
                helixToken = User.get(requireContext()).helixToken,
                gqlClientId = requireContext().prefs().getString(C.GQL_CLIENT_ID, ""),
                tags = arguments?.getStringArray(C.TAGS)?.toList(),
                apiPref = TwitchApiHelper.listFromPrefs(requireContext().prefs().getString(C.API_PREF_STREAMS, ""), TwitchApiHelper.streamsApiDefaults),
                gameApiPref = TwitchApiHelper.listFromPrefs(requireContext().prefs().getString(C.API_PREF_GAME_STREAMS, ""), TwitchApiHelper.gameStreamsApiDefaults),
                thumbnailsEnabled = !compactStreams
            )
            val activity = requireActivity() as MainActivity
            sortBar.root.visible()
            if (arguments?.getString(C.GAME_ID) != null && arguments?.getString(C.GAME_NAME) != null) {
                sortBar.root.setOnClickListener {
                    StreamsSortDialog.newInstance(
                        sort = viewModel.sort
                    ).show(childFragmentManager, null)
                }
                if ((requireContext().prefs().getString(C.UI_FOLLOW_BUTTON, "0")?.toInt() ?: 0) < 2) {
                    followButton?.let {
                        initializeFollow(
                            fragment = this@StreamsFragment,
                            viewModel = viewModel,
                            followButton = it,
                            setting = requireContext().prefs().getString(C.UI_FOLLOW_BUTTON, "0")?.toInt() ?: 0,
                            user = User.get(activity),
                            helixClientId = requireContext().prefs().getString(C.HELIX_CLIENT_ID, ""),
                            gqlClientId = requireContext().prefs().getString(C.GQL_CLIENT_ID, "")
                        )
                    }
                }
                if (arguments?.getBoolean(C.CHANNEL_UPDATELOCAL) == true) {
                    viewModel.updateLocalGame(requireContext())
                }
            } else {
                sortBar.root.setOnClickListener { activity.openTagSearch() }
            }
        }
    }

    override fun onChange(sort: Sort) {
        adapter.submitList(null)
        viewModel.filter(
            sort = sort
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}