package com.github.andreyasadchy.xtra.ui.follow.games

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.github.andreyasadchy.xtra.databinding.FragmentStreamsBinding
import com.github.andreyasadchy.xtra.model.User
import com.github.andreyasadchy.xtra.model.helix.game.Game
import com.github.andreyasadchy.xtra.ui.common.PagedListFragment
import com.github.andreyasadchy.xtra.ui.common.Scrollable
import com.github.andreyasadchy.xtra.ui.main.MainActivity
import com.github.andreyasadchy.xtra.util.C
import com.github.andreyasadchy.xtra.util.TwitchApiHelper
import com.github.andreyasadchy.xtra.util.prefs
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FollowedGamesFragment : PagedListFragment<Game, FollowedGamesViewModel>(), Scrollable {

    override val pagedListBinding get() = binding.recyclerViewLayout
    private var _binding: FragmentStreamsBinding? = null
    private val binding get() = _binding!!
    override val viewModel: FollowedGamesViewModel by viewModels()
    override val adapter by lazy {
        val activity = requireActivity() as MainActivity
        FollowedGamesAdapter(this, activity, activity)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStreamsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun initialize() {
        super.initialize()
        viewModel.setUser(
            user = User.get(requireContext()),
            gqlClientId = requireContext().prefs().getString(C.GQL_CLIENT_ID, ""),
            apiPref = TwitchApiHelper.listFromPrefs(requireContext().prefs().getString(C.API_PREF_FOLLOWED_GAMES, ""), TwitchApiHelper.followedGamesApiDefaults)
        )
    }

    override fun scrollToTop() {
        binding.recyclerViewLayout.recyclerView.scrollToPosition(0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}