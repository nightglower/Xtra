package com.github.andreyasadchy.xtra.ui.streams.followed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.github.andreyasadchy.xtra.databinding.FragmentStreamsBinding
import com.github.andreyasadchy.xtra.model.User
import com.github.andreyasadchy.xtra.ui.main.MainActivity
import com.github.andreyasadchy.xtra.ui.streams.BaseStreamsFragment
import com.github.andreyasadchy.xtra.ui.streams.StreamsAdapter
import com.github.andreyasadchy.xtra.ui.streams.StreamsCompactAdapter
import com.github.andreyasadchy.xtra.util.C
import com.github.andreyasadchy.xtra.util.TwitchApiHelper
import com.github.andreyasadchy.xtra.util.prefs
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FollowedStreamsFragment : BaseStreamsFragment<FollowedStreamsViewModel>() {

    override val baseBinding get() = binding.recyclerViewLayout
    private var _binding: FragmentStreamsBinding? = null
    private val binding get() = _binding!!
    override val viewModel: FollowedStreamsViewModel by viewModels()
    override val adapter by lazy {
        val activity = requireActivity() as MainActivity
        if (!compactStreams) {
            StreamsAdapter(this, activity, activity, activity)
        } else {
            StreamsCompactAdapter(this, activity, activity, activity)
        }
    }

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
        viewModel.loadStreams(
            userId = User.get(requireContext()).id,
            helixClientId = requireContext().prefs().getString(C.HELIX_CLIENT_ID, ""),
            helixToken = User.get(requireContext()).helixToken,
            gqlClientId = requireContext().prefs().getString(C.GQL_CLIENT_ID, ""),
            gqlToken = User.get(requireContext()).gqlToken,
            apiPref = TwitchApiHelper.listFromPrefs(requireContext().prefs().getString(C.API_PREF_FOLLOWED_STREAMS, ""), TwitchApiHelper.followedStreamsApiDefaults),
            thumbnailsEnabled = !compactStreams
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
