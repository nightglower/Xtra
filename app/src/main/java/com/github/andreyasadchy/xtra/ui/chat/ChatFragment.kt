package com.github.andreyasadchy.xtra.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.github.andreyasadchy.xtra.databinding.FragmentChatBinding
import com.github.andreyasadchy.xtra.model.User
import com.github.andreyasadchy.xtra.model.chat.Emote
import com.github.andreyasadchy.xtra.model.helix.stream.Stream
import com.github.andreyasadchy.xtra.ui.common.BaseNetworkFragment
import com.github.andreyasadchy.xtra.ui.main.MainActivity
import com.github.andreyasadchy.xtra.ui.player.BasePlayerFragment
import com.github.andreyasadchy.xtra.ui.player.stream.StreamPlayerFragment
import com.github.andreyasadchy.xtra.ui.view.chat.MessageClickedDialog
import com.github.andreyasadchy.xtra.util.C
import com.github.andreyasadchy.xtra.util.LifecycleListener
import com.github.andreyasadchy.xtra.util.chat.Raid
import com.github.andreyasadchy.xtra.util.hideKeyboard
import com.github.andreyasadchy.xtra.util.prefs
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChatFragment : BaseNetworkFragment(), LifecycleListener, MessageClickedDialog.OnButtonClickListener {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ChatViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun initialize() {
        with(binding) {
            val args = requireArguments()
            val channelId = args.getString(KEY_CHANNEL_ID)
            val channelLogin = args.getString(KEY_CHANNEL_LOGIN)
            val channelName = args.getString(KEY_CHANNEL_NAME)
            val streamId = args.getString(KEY_STREAM_ID)
            val user = User.get(requireContext())
            val isLoggedIn = !user.login.isNullOrBlank() && (!user.gqlToken.isNullOrBlank() || !user.helixToken.isNullOrBlank())
            val useSSl = requireContext().prefs().getBoolean(C.CHAT_USE_SSL, true)
            val usePubSub = requireContext().prefs().getBoolean(C.CHAT_PUBSUB_ENABLED, true)
            val helixClientId = requireContext().prefs().getString(C.HELIX_CLIENT_ID, "")
            val gqlClientId = requireContext().prefs().getString(C.GQL_CLIENT_ID, "")
            val showUserNotice = requireContext().prefs().getBoolean(C.CHAT_SHOW_USERNOTICE, true)
            val showClearMsg = requireContext().prefs().getBoolean(C.CHAT_SHOW_CLEARMSG, true)
            val showClearChat = requireContext().prefs().getBoolean(C.CHAT_SHOW_CLEARCHAT, true)
            val collectPoints = requireContext().prefs().getBoolean(C.CHAT_POINTS_COLLECT, true)
            val notifyPoints = requireContext().prefs().getBoolean(C.CHAT_POINTS_NOTIFY, false)
            val showRaids = requireContext().prefs().getBoolean(C.CHAT_RAIDS_SHOW, true)
            val autoSwitchRaids = requireContext().prefs().getBoolean(C.CHAT_RAIDS_AUTO_SWITCH, true)
            val enableRecentMsg = requireContext().prefs().getBoolean(C.CHAT_RECENT, true)
            val recentMsgLimit = requireContext().prefs().getInt(C.CHAT_RECENT_LIMIT, 100)
            val disableChat = requireContext().prefs().getBoolean(C.CHAT_DISABLE, false)
            val isLive = args.getBoolean(KEY_IS_LIVE)
            val enableChat = if (disableChat) {
                false
            } else {
                if (isLive) {
                    viewModel.startLive(useSSl, usePubSub, user, isLoggedIn, helixClientId, gqlClientId, channelId, channelLogin, channelName, streamId, showUserNotice, showClearMsg, showClearChat, collectPoints, notifyPoints, showRaids, autoSwitchRaids, enableRecentMsg, recentMsgLimit.toString())
                    chatView.init(this@ChatFragment)
                    chatView.setCallback(viewModel, (viewModel.chat as? ChatViewModel.LiveChatController))
                    chatView.setChannelId(channelId)
                    if (isLoggedIn) {
                        user.login?.let { chatView.setUsername(it) }
                        chatView.setChatters(viewModel.chatters)
                        val emotesObserver = Observer(chatView::addEmotes)
                        viewModel.userEmotes.observe(viewLifecycleOwner, emotesObserver)
                        viewModel.recentEmotes.observe(viewLifecycleOwner, emotesObserver)
                        viewModel.newChatter.observe(viewLifecycleOwner, Observer(chatView::addChatter))
                    }
                    true
                } else {
                    args.getString(KEY_VIDEO_ID).let {
                        if (it != null && !args.getBoolean(KEY_START_TIME_EMPTY)) {
                            chatView.init(this@ChatFragment)
                            val getCurrentPosition = (parentFragment as ChatReplayPlayerFragment)::getCurrentPosition
                            viewModel.startReplay(user, helixClientId, gqlClientId, channelId, it, args.getDouble(KEY_START_TIME), getCurrentPosition)
                            chatView.setChannelId(channelId)
                            true
                        } else {
                            chatView.setChatReplayUnavailableText(true)
                            false
                        }
                    }
                }
            }
            if (enableChat) {
                chatView.enableChatInteraction(isLive && isLoggedIn)
                viewModel.chatMessages.observe(viewLifecycleOwner, Observer(chatView::submitList))
                viewModel.newMessage.observe(viewLifecycleOwner) { chatView.notifyMessageAdded() }
                viewModel.recentMessages.observe(viewLifecycleOwner) { chatView.addRecentMessages(it) }
                viewModel.globalBadges.observe(viewLifecycleOwner, Observer(chatView::addGlobalBadges))
                viewModel.channelBadges.observe(viewLifecycleOwner, Observer(chatView::addChannelBadges))
                viewModel.otherEmotes.observe(viewLifecycleOwner, Observer(chatView::addEmotes))
                viewModel.cheerEmotes.observe(viewLifecycleOwner, Observer(chatView::addCheerEmotes))
                viewModel.reloadMessages.observe(viewLifecycleOwner) { chatView.notifyEmotesLoaded() }
                viewModel.roomState.observe(viewLifecycleOwner) { chatView.notifyRoomState(it) }
                viewModel.command.observe(viewLifecycleOwner) { chatView.notifyCommand(it) }
                viewModel.reward.observe(viewLifecycleOwner) { chatView.notifyReward(it) }
                viewModel.pointsEarned.observe(viewLifecycleOwner) { chatView.notifyPointsEarned(it) }
                viewModel.raid.observe(viewLifecycleOwner) { onRaidUpdate(it) }
                viewModel.raidClicked.observe(viewLifecycleOwner) { onRaidClicked() }
                viewModel.host.observe(viewLifecycleOwner) { onHost(it) }
                viewModel.hostClicked.observe(viewLifecycleOwner) { onHostClicked() }
                viewModel.viewerCount.observe(viewLifecycleOwner) { (parentFragment as? StreamPlayerFragment)?.updateViewerCount(it) }
            }
        }
    }

    fun isActive(): Boolean? {
        return (viewModel.chat as? ChatViewModel.LiveChatController)?.isActive()
    }

    fun disconnect() {
        (viewModel.chat as? ChatViewModel.LiveChatController)?.disconnect()
    }

    fun reconnect() {
        (viewModel.chat as? ChatViewModel.LiveChatController)?.start()
        val channelLogin = requireArguments().getString(KEY_CHANNEL_LOGIN)
        val enableRecentMsg = requireContext().prefs().getBoolean(C.CHAT_RECENT, true)
        val recentMsgLimit = requireContext().prefs().getInt(C.CHAT_RECENT_LIMIT, 100)
        if (channelLogin != null && enableRecentMsg) {
            viewModel.loadRecentMessages(channelLogin, recentMsgLimit.toString())
        }
    }

    fun reloadEmotes() {
        val channelId = requireArguments().getString(KEY_CHANNEL_ID)
        val helixClientId = requireContext().prefs().getString(C.HELIX_CLIENT_ID, "")
        val helixToken = User.get(requireContext()).helixToken
        val gqlClientId = requireContext().prefs().getString(C.GQL_CLIENT_ID, "")
        viewModel.reloadEmotes(helixClientId, helixToken, gqlClientId, channelId)
    }

    private fun onRaidUpdate(raid: Raid) {
        with(binding) {
            if (viewModel.raidClosed && viewModel.raidNewId) {
                viewModel.raidAutoSwitch = requireContext().prefs().getBoolean(C.CHAT_RAIDS_AUTO_SWITCH, true)
                viewModel.raidClosed = false
            }
            if (raid.openStream) {
                if (!viewModel.raidClosed) {
                    if (viewModel.raidAutoSwitch) {
                        if (parentFragment is BasePlayerFragment && (parentFragment as? BasePlayerFragment)?.isSleepTimerActive() != true) {
                            onRaidClicked()
                        }
                    } else {
                        viewModel.raidAutoSwitch = requireContext().prefs().getBoolean(C.CHAT_RAIDS_AUTO_SWITCH, true)
                    }
                    chatView.hideRaid()
                } else {
                    viewModel.raidAutoSwitch = requireContext().prefs().getBoolean(C.CHAT_RAIDS_AUTO_SWITCH, true)
                    viewModel.raidClosed = false
                }
            } else {
                if (!viewModel.raidClosed) {
                    chatView.notifyRaid(raid, viewModel.raidNewId)
                }
            }
        }
    }

    private fun onRaidClicked() {
        viewModel.raid.value?.let {
            (requireActivity() as MainActivity).startStream(Stream(
                user_id = it.targetId,
                user_login = it.targetLogin,
                user_name = it.targetName,
                profileImageURL = it.targetProfileImage,
            ))
        }
    }

    private fun onHost(stream: Stream) {
        with(binding) {
            if (viewModel.showRaids) {
                chatView.notifyHost(stream)
            }
        }
    }

    override fun onHostClicked() {
        viewModel.host.value?.let {
            (requireActivity() as MainActivity).startStream(Stream(
                user_id = it.user_id,
                user_login = it.user_login,
                user_name = it.user_name,
                profileImageURL = it.channelLogo,
            ))
        }
    }

    fun hideKeyboard() {
        with(binding) {
            chatView.hideKeyboard()
            chatView.clearFocus()
        }
    }

    fun hideEmotesMenu() = binding.chatView.hideEmotesMenu()

    fun appendEmote(emote: Emote) {
        binding.chatView.appendEmote(emote)
    }

    override fun onReplyClicked(userName: String) {
        binding.chatView.reply(userName)
    }

    override fun onCopyMessageClicked(message: String) {
        binding.chatView.setMessage(message)
    }

    override fun onViewProfileClicked(id: String?, login: String?, name: String?, channelLogo: String?) {
        (requireActivity() as MainActivity).viewChannel(id, login, name, channelLogo)
        (parentFragment as? BasePlayerFragment)?.minimize()
    }

    override fun onNetworkRestored() {
        if (isResumed) {
            viewModel.start()
        }
    }

    override fun onMovedToBackground() {
        if (!requireArguments().getBoolean(KEY_IS_LIVE) || !requireContext().prefs().getBoolean(C.PLAYER_KEEP_CHAT_OPEN, false) || requireContext().prefs().getBoolean(C.CHAT_DISABLE, false)) {
            viewModel.stop()
        }
    }

    override fun onMovedToForeground() {
        if (!requireArguments().getBoolean(KEY_IS_LIVE) || !requireContext().prefs().getBoolean(C.PLAYER_KEEP_CHAT_OPEN, false) || requireContext().prefs().getBoolean(C.CHAT_DISABLE, false)) {
            viewModel.start()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val KEY_IS_LIVE = "isLive"
        private const val KEY_CHANNEL_ID = "channel_id"
        private const val KEY_CHANNEL_LOGIN = "channel_login"
        private const val KEY_CHANNEL_NAME = "channel_name"
        private const val KEY_STREAM_ID = "streamId"
        private const val KEY_VIDEO_ID = "videoId"
        private const val KEY_START_TIME_EMPTY = "startTime_empty"
        private const val KEY_START_TIME = "startTime"

        fun newInstance(channelId: String?, channelLogin: String?, channelName: String?, streamId: String?) = ChatFragment().apply {
            arguments = Bundle().apply {
                putBoolean(KEY_IS_LIVE, true)
                putString(KEY_CHANNEL_ID, channelId)
                putString(KEY_CHANNEL_LOGIN, channelLogin)
                putString(KEY_CHANNEL_NAME, channelName)
                putString(KEY_STREAM_ID, streamId)
            }
        }

        fun newInstance(channelId: String?, videoId: String?, startTime: Double?) = ChatFragment().apply {
            arguments = Bundle().apply {
                putBoolean(KEY_IS_LIVE, false)
                putString(KEY_CHANNEL_ID, channelId)
                putString(KEY_VIDEO_ID, videoId)
                if (startTime != null) {
                    putBoolean(KEY_START_TIME_EMPTY, false)
                    putDouble(KEY_START_TIME, startTime)
                } else {
                    putBoolean(KEY_START_TIME_EMPTY, true)
                }
            }
        }
    }
}