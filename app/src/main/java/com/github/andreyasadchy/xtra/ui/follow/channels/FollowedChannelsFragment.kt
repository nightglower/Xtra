package com.github.andreyasadchy.xtra.ui.follow.channels

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.databinding.FragmentStreamsBinding
import com.github.andreyasadchy.xtra.model.helix.follows.Order
import com.github.andreyasadchy.xtra.model.helix.follows.Sort
import com.github.andreyasadchy.xtra.ui.common.PagedListFragment
import com.github.andreyasadchy.xtra.util.C
import com.github.andreyasadchy.xtra.util.prefs
import com.github.andreyasadchy.xtra.util.visible
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FollowedChannelsFragment : PagedListFragment(), FollowedChannelsSortDialog.OnFilter {

    private var _binding: FragmentStreamsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FollowedChannelsViewModel by viewModels()
    private val pagingAdapter = FollowedChannelsAdapter(this)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStreamsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun initialize() {
        with(binding) {
            viewModel.setUser(requireContext())
            init(recyclerViewLayout, pagingAdapter, viewModel.flow)
            viewModel.sortText.observe(viewLifecycleOwner) {
                sortBar.sortText.text = it
            }
            sortBar.root.visible()
            sortBar.root.setOnClickListener {
                with(viewModel.filter.value) {
                    findNavController().navigate(FollowedChannelsSortDialogDirections.actionGlobalFollowedChannelsSortDialog(
                        sort = sort,
                        order = order,
                        saveDefault = requireContext().prefs().getBoolean(C.SORT_DEFAULT_FOLLOWED_CHANNELS, false)
                    ))
                }
            }
        }
    }

    override fun onChange(sort: Sort, sortText: CharSequence, order: Order, orderText: CharSequence, saveDefault: Boolean) {
        //adapter.submitList(null)
        viewModel.filter(
            context = requireContext(),
            sort = sort,
            order = order,
            text = getString(R.string.sort_and_order, sortText, orderText),
            saveDefault = saveDefault
        )
    }

    override fun onNetworkRestored() {
        pagingAdapter.retry()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}