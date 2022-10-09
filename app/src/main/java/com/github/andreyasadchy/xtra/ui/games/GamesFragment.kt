package com.github.andreyasadchy.xtra.ui.games

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.viewModels
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.databinding.FragmentGamesBinding
import com.github.andreyasadchy.xtra.model.NotLoggedIn
import com.github.andreyasadchy.xtra.model.User
import com.github.andreyasadchy.xtra.model.helix.game.Game
import com.github.andreyasadchy.xtra.ui.common.PagedListFragment
import com.github.andreyasadchy.xtra.ui.common.Scrollable
import com.github.andreyasadchy.xtra.ui.login.LoginActivity
import com.github.andreyasadchy.xtra.ui.main.MainActivity
import com.github.andreyasadchy.xtra.ui.settings.SettingsActivity
import com.github.andreyasadchy.xtra.util.*
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GamesFragment : PagedListFragment<Game, GamesViewModel>(), Scrollable {

    interface OnGameSelectedListener {
        fun openGame(id: String? = null, name: String? = null, tags: List<String>? = null, updateLocal: Boolean = false)
    }

    interface OnTagGames {
        fun openTagGames(tags: List<String>?)
    }

    companion object {
        fun newInstance(tags: List<String>?) = GamesFragment().apply {
            arguments = Bundle().apply {
                putStringArray(C.TAGS, tags?.toTypedArray())
            }
        }
    }

    override val pagedListBinding get() = binding.recyclerViewLayout
    private var _binding: FragmentGamesBinding? = null
    private val binding get() = _binding!!
    override val viewModel: GamesViewModel by viewModels()
    override val adapter by lazy { GamesAdapter(this, requireActivity() as MainActivity, requireActivity() as MainActivity) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGamesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (arguments?.getStringArray(C.TAGS).isNullOrEmpty()) {
            binding.recyclerViewLayout.scrollTop.isEnabled = false
        }
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            val activity = requireActivity() as MainActivity
            val user = User.get(activity)
            search.setOnClickListener { activity.openSearch() }
            menu.setOnClickListener { it ->
                PopupMenu(activity, it).apply {
                    inflate(R.menu.top_menu)
                    menu.findItem(R.id.login).title = if (user !is NotLoggedIn) getString(R.string.log_out) else getString(R.string.log_in)
                    setOnMenuItemClickListener {
                        when(it.itemId) {
                            R.id.settings -> { activity.startActivityFromFragment(this@GamesFragment, Intent(activity, SettingsActivity::class.java), 3) }
                            R.id.login -> {
                                if (user is NotLoggedIn) {
                                    activity.startActivityForResult(Intent(activity, LoginActivity::class.java), 1)
                                } else {
                                    AlertDialog.Builder(activity).apply {
                                        setTitle(getString(R.string.logout_title))
                                        user.login?.nullIfEmpty()?.let { user -> setMessage(getString(R.string.logout_msg, user)) }
                                        setNegativeButton(getString(R.string.no)) { dialog, _ -> dialog.dismiss() }
                                        setPositiveButton(getString(R.string.yes)) { _, _ -> activity.startActivityForResult(Intent(activity, LoginActivity::class.java), 2) }
                                    }.show()
                                }
                            }
                            else -> menu.close()
                        }
                        true
                    }
                    show()
                }
            }
            sortBar.root.visible()
            sortBar.root.setOnClickListener { activity.openTagSearch(getGameTags = true) }
        }
    }

    override fun scrollToTop() {
        with(binding) {
            appBar.setExpanded(true, true)
            recyclerViewLayout.recyclerView.scrollToPosition(0)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 3 && resultCode == Activity.RESULT_OK) {
            requireActivity().recreate()
        }
    }

    override fun initialize() {
        super.initialize()
        with(binding) {
            if (!arguments?.getStringArray(C.TAGS).isNullOrEmpty()) {
                recyclerViewLayout.scrollTop.setOnClickListener {
                    scrollToTop()
                    it.gone()
                }
            }
            viewModel.loadGames(
                helixClientId = requireContext().prefs().getString(C.HELIX_CLIENT_ID, ""),
                helixToken = User.get(requireContext()).helixToken,
                gqlClientId = requireContext().prefs().getString(C.GQL_CLIENT_ID, ""),
                tags = arguments?.getStringArray(C.TAGS)?.toList(),
                apiPref = TwitchApiHelper.listFromPrefs(requireContext().prefs().getString(C.API_PREF_GAMES, ""), TwitchApiHelper.gamesApiDefaults)
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}