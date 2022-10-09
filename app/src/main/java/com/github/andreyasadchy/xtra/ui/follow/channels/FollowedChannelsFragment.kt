package com.github.andreyasadchy.xtra.ui.follow.channels

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.databinding.FragmentStreamsBinding
import com.github.andreyasadchy.xtra.model.User
import com.github.andreyasadchy.xtra.model.helix.follows.Follow
import com.github.andreyasadchy.xtra.model.helix.follows.Order
import com.github.andreyasadchy.xtra.model.helix.follows.Sort
import com.github.andreyasadchy.xtra.ui.common.PagedListFragment
import com.github.andreyasadchy.xtra.ui.common.Scrollable
import com.github.andreyasadchy.xtra.ui.main.MainActivity
import com.github.andreyasadchy.xtra.util.C
import com.github.andreyasadchy.xtra.util.TwitchApiHelper
import com.github.andreyasadchy.xtra.util.prefs
import com.github.andreyasadchy.xtra.util.visible
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FollowedChannelsFragment : PagedListFragment<Follow, FollowedChannelsViewModel>(), FollowedChannelsSortDialog.OnFilter, Scrollable {

    override val pagedListBinding get() = binding.recyclerViewLayout
    private var _binding: FragmentStreamsBinding? = null
    private val binding get() = _binding!!
    override val viewModel: FollowedChannelsViewModel by viewModels()
    override val adapter by lazy {
        val activity = requireActivity() as MainActivity
        FollowedChannelsAdapter(this, activity)
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
                helixClientId = requireContext().prefs().getString(C.HELIX_CLIENT_ID, ""),
                gqlClientId = requireContext().prefs().getString(C.GQL_CLIENT_ID, ""),
                apiPref = TwitchApiHelper.listFromPrefs(requireContext().prefs().getString(C.API_PREF_FOLLOWED_CHANNELS, ""), TwitchApiHelper.followedChannelsApiDefaults),
            )
            sortBar.root.visible()
            sortBar.root.setOnClickListener {
                FollowedChannelsSortDialog.newInstance(
                    sort = viewModel.sort,
                    order = viewModel.order,
                    saveDefault = requireContext().prefs().getBoolean(C.SORT_DEFAULT_FOLLOWED_CHANNELS, false)
                ).show(childFragmentManager, null)
            }
        }
    }

    override fun onChange(sort: Sort, sortText: CharSequence, order: Order, orderText: CharSequence, saveDefault: Boolean) {
        adapter.submitList(null)
        viewModel.filter(
            sort = sort,
            order = order,
            text = getString(R.string.sort_and_order, sortText, orderText),
            saveDefault = saveDefault
        )
    }

    override fun scrollToTop() {
        binding.recyclerViewLayout.recyclerView?.scrollToPosition(0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}