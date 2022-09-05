package com.github.andreyasadchy.xtra.ui.saved.bookmarks

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.databinding.FragmentSavedBinding
import com.github.andreyasadchy.xtra.model.User
import com.github.andreyasadchy.xtra.ui.download.VideoDownloadDialogDirections
import com.github.andreyasadchy.xtra.ui.main.MainActivity
import com.github.andreyasadchy.xtra.util.C
import com.github.andreyasadchy.xtra.util.prefs
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BookmarksFragment : Fragment() {

    private var _binding: FragmentSavedBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BookmarksViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSavedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            val activity = requireActivity() as MainActivity
            val adapter = BookmarksAdapter(this@BookmarksFragment, activity, {
                viewModel.loadVideo(
                    context = requireContext(),
                    helixClientId = requireContext().prefs().getString(C.HELIX_CLIENT_ID, ""),
                    helixToken = User.get(requireContext()).helixToken,
                    gqlClientId = requireContext().prefs().getString(C.GQL_CLIENT_ID, ""),
                    videoId = it
                )
            }, {
                findNavController().navigate(VideoDownloadDialogDirections.actionGlobalVideoDownloadDialog(video = it))
            }, {
                viewModel.vodIgnoreUser(it)
            }, {
                val delete = getString(R.string.delete)
                AlertDialog.Builder(activity)
                    .setTitle(delete)
                    .setMessage(getString(R.string.are_you_sure))
                    .setPositiveButton(delete) { _, _ -> viewModel.delete(requireContext(), it) }
                    .setNegativeButton(getString(android.R.string.cancel), null)
                    .show()
            })
            recyclerView.adapter = adapter
            (recyclerView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
            viewModel.bookmarks.observe(viewLifecycleOwner) {
                adapter.submitList(it.reversed())
                nothingHere.isVisible = it.isEmpty()
            }
            if (requireContext().prefs().getBoolean(C.PLAYER_USE_VIDEOPOSITIONS, true)) {
                viewModel.positions.observe(viewLifecycleOwner) {
                    adapter.setVideoPositions(it)
                }
            }
            if (requireContext().prefs().getBoolean(C.UI_BOOKMARK_TIME_LEFT, true)) {
                viewModel.ignoredUsers.observe(viewLifecycleOwner) {
                    adapter.setIgnoredUsers(it)
                }
                viewModel.loadUsers(
                    helixClientId = requireContext().prefs().getString(C.HELIX_CLIENT_ID, ""),
                    helixToken = User.get(requireContext()).helixToken,
                    gqlClientId = requireContext().prefs().getString(C.GQL_CLIENT_ID, ""),
                )
            }
            if (!User.get(requireContext()).helixToken.isNullOrBlank()) {
                viewModel.loadVideos(
                    context = requireContext(),
                    helixClientId = requireContext().prefs().getString(C.HELIX_CLIENT_ID, ""),
                    helixToken = User.get(requireContext()).helixToken,
                )
            }
            adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
                override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                    adapter.unregisterAdapterDataObserver(this)
                    adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
                        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                            if (positionStart == 0) {
                                recyclerView.smoothScrollToPosition(0)
                            }
                        }
                    })
                }
            })
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 3 && resultCode == Activity.RESULT_OK) {
            requireActivity().recreate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
