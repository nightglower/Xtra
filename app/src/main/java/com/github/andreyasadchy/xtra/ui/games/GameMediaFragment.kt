package com.github.andreyasadchy.xtra.ui.games

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.databinding.FragmentMediaBinding
import com.github.andreyasadchy.xtra.model.Account
import com.github.andreyasadchy.xtra.model.NotLoggedIn
import com.github.andreyasadchy.xtra.ui.Utils
import com.github.andreyasadchy.xtra.ui.clips.common.ClipsFragment
import com.github.andreyasadchy.xtra.ui.common.MediaFragment
import com.github.andreyasadchy.xtra.ui.common.Scrollable
import com.github.andreyasadchy.xtra.ui.login.LoginActivity
import com.github.andreyasadchy.xtra.ui.main.MainActivity
import com.github.andreyasadchy.xtra.ui.search.SearchPagerFragmentDirections
import com.github.andreyasadchy.xtra.ui.settings.SettingsActivity
import com.github.andreyasadchy.xtra.ui.streams.common.StreamsFragment
import com.github.andreyasadchy.xtra.ui.videos.game.GameVideosFragment
import com.github.andreyasadchy.xtra.util.*
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GameMediaFragment : MediaFragment(), Scrollable {

    private var _binding: FragmentMediaBinding? = null
    private val binding get() = _binding!!
    private val args: GamePagerFragmentArgs by navArgs()
    private val viewModel: GamePagerViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMediaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            val activity = requireActivity() as MainActivity
            val account = Account.get(activity)
            if ((requireContext().prefs().getString(C.UI_FOLLOW_BUTTON, "0")?.toInt() ?: 0) < 2) {
                followButton.visible()
                var initialized = false
                viewModel.follow.observe(viewLifecycleOwner) { pair ->
                    val following = pair.first
                    val errorMessage = pair.second
                    if (initialized) {
                        if (!errorMessage.isNullOrBlank()) {
                            requireContext().shortToast(errorMessage)
                        } else {
                            requireContext().shortToast(requireContext().getString(if (following) R.string.now_following else R.string.unfollowed, args.gameName))
                        }
                    } else {
                        initialized = true
                    }
                    if (errorMessage.isNullOrBlank()) {
                        followButton.setOnClickListener {
                            if (!following) {
                                viewModel.saveFollowGame(requireContext(), args.gameId, args.gameName)
                            } else {
                                FragmentUtils.showUnfollowDialog(requireContext(), args.gameName) {
                                    viewModel.deleteFollowGame(requireContext(), args.gameId)
                                }
                            }
                        }
                        followButton.setImageResource(if (following) R.drawable.baseline_favorite_black_24 else R.drawable.baseline_favorite_border_black_24)
                    }
                }
            }
            search.setOnClickListener { findNavController().navigate(SearchPagerFragmentDirections.actionGlobalSearchPagerFragment()) }
            menu.setOnClickListener { it ->
                PopupMenu(activity, it).apply {
                    inflate(R.menu.top_menu)
                    menu.findItem(R.id.login).title = if (account !is NotLoggedIn) getString(R.string.log_out) else getString(R.string.log_in)
                    setOnMenuItemClickListener {
                        when(it.itemId) {
                            R.id.settings -> { activity.startActivityFromFragment(this@GameMediaFragment, Intent(activity, SettingsActivity::class.java), 3) }
                            R.id.login -> {
                                if (account is NotLoggedIn) {
                                    activity.startActivityForResult(Intent(activity, LoginActivity::class.java), 1)
                                } else {
                                    AlertDialog.Builder(activity).apply {
                                        setTitle(getString(R.string.logout_title))
                                        account.login?.nullIfEmpty()?.let { user -> setMessage(getString(R.string.logout_msg, user)) }
                                        setNegativeButton(getString(R.string.no)) { dialog, _ -> dialog.dismiss() }
                                        setPositiveButton(getString(R.string.yes)) { _, _ -> activity.startActivityForResult(
                                            Intent(activity, LoginActivity::class.java), 2) }
                                    }.show()
                                }
                            }
                        }
                        true
                    }
                    show()
                }
            }
            if (!args.gameId.isNullOrBlank() || !args.gameName.isNullOrBlank()) {
                toolbar.apply {
                    title = args.gameName
                    navigationIcon = Utils.getNavigationIcon(activity)
                    setNavigationOnClickListener { activity.popFragment() }
                }
                spinner.visible()
                spinner.adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_dropdown_item, resources.getStringArray(R.array.spinnerMedia))
                spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        currentFragment = if (position != previousItem) {
                            val newFragment = onSpinnerItemSelected(position)
                            childFragmentManager.beginTransaction().replace(R.id.fragmentContainer, newFragment).commit()
                            previousItem = position
                            newFragment
                        } else {
                            childFragmentManager.findFragmentById(R.id.fragmentContainer)
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            } else {
                currentFragment = if (previousItem != 0) {
                    val newFragment = onSpinnerItemSelected(0)
                    childFragmentManager.beginTransaction().replace(R.id.fragmentContainer, newFragment).commit()
                    previousItem = 0
                    newFragment
                } else {
                    childFragmentManager.findFragmentById(R.id.fragmentContainer)
                }
            }
        }
        if ((requireContext().prefs().getString(C.UI_FOLLOW_BUTTON, "0")?.toInt() ?: 0) < 2) {
            viewModel.isFollowingGame(requireContext(), args.gameId, args.gameName)
        }
        if (args.updateLocal) {
            viewModel.updateLocalGame(requireContext(), args.gameId, args.gameName)
        }
    }

    override fun onSpinnerItemSelected(position: Int): Fragment {
        val fragment: Fragment = when (position) {
            0 -> StreamsFragment()
            1 -> GameVideosFragment()
            else -> ClipsFragment()
        }
        return fragment.also { it.arguments = requireArguments() }
    }

    override fun scrollToTop() {
        binding.appBar.setExpanded(true, true)
        (currentFragment as? Scrollable)?.scrollToTop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}