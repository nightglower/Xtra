package com.github.andreyasadchy.xtra.ui.search.tags

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.github.andreyasadchy.xtra.databinding.CommonRecyclerViewLayoutBinding
import com.github.andreyasadchy.xtra.model.helix.tag.Tag
import com.github.andreyasadchy.xtra.ui.common.PagedListFragment
import com.github.andreyasadchy.xtra.ui.main.MainActivity
import com.github.andreyasadchy.xtra.ui.search.Searchable
import com.github.andreyasadchy.xtra.util.C
import com.github.andreyasadchy.xtra.util.prefs
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TagSearchFragment : PagedListFragment<Tag, TagSearchViewModel>(), Searchable {

    override val pagedListBinding get() = binding
    private var _binding: CommonRecyclerViewLayoutBinding? = null
    private val binding get() = _binding!!
    override val viewModel: TagSearchViewModel by viewModels()
    override val adapter by lazy { TagSearchAdapter(this, requireActivity() as MainActivity, requireActivity() as MainActivity) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = CommonRecyclerViewLayoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.swipeRefresh.isEnabled = false
        viewModel.loadTags(clientId = requireContext().prefs().getString(C.GQL_CLIENT_ID, ""), getGameTags = parentFragment?.arguments?.getBoolean(C.GET_GAME_TAGS) ?: false, gameId = parentFragment?.arguments?.getString(C.GAME_ID), gameName = parentFragment?.arguments?.getString(C.GAME_NAME))
    }

    override fun search(query: String) {
        viewModel.setQuery(query)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}