package com.github.andreyasadchy.xtra.ui.view.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.databinding.DialogChatMessageClickBinding
import com.github.andreyasadchy.xtra.model.helix.user.User
import com.github.andreyasadchy.xtra.ui.common.ExpandingBottomSheetDialogFragment
import com.github.andreyasadchy.xtra.util.*
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MessageClickedDialog : ExpandingBottomSheetDialogFragment() {

    interface OnButtonClickListener {
        fun onReplyClicked(userName: String)
        fun onCopyMessageClicked(message: String)
        fun onViewProfileClicked(id: String?, login: String?, name: String?, channelLogo: String?)
        fun onHostClicked()
    }

    companion object {
        private val savedUsers = mutableListOf<Pair<User, String?>>()
    }

    private var _binding: DialogChatMessageClickBinding? = null
    private val binding get() = _binding!!
    private val args: MessageClickedDialogArgs by navArgs()
    private val viewModel: MessageClickedViewModel by viewModels()

    private lateinit var listener: OnButtonClickListener

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = parentFragment as OnButtonClickListener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogChatMessageClickBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            message.text = args.formattedMessage
            val msg = args.originalMessage
            val userId = args.userId
            val targetId = args.channelId
            val fullMsg = args.fullMsg
            val clipboard = getSystemService(requireContext(), ClipboardManager::class.java)
            if (userId != null) {
                val item = savedUsers.find { it.first.id == userId && it.second == targetId }
                if (item != null) {
                    updateUserLayout(item.first)
                } else {
                    viewModel.loadUser(
                        channelId = userId,
                        targetId = if (userId != targetId) targetId else null,
                        context = requireContext()
                    ).observe(viewLifecycleOwner) { user ->
                        if (user != null) {
                            savedUsers.add(Pair(user, targetId))
                            updateUserLayout(user)
                        } else {
                            viewProfile.visible()
                        }
                    }
                }
                if (args.enableMessaging) {
                    reply.visible()
                    reply.setOnClickListener {
                        msg?.let { listener.onReplyClicked(extractUserName(msg)) }
                        dismiss()
                    }
                    copyMessage.visible()
                    copyMessage.setOnClickListener {
                        msg?.let { listener.onCopyMessageClicked(msg.substring(msg.indexOf(':') + 2)) }
                        dismiss()
                    }
                } else {
                    reply.gone()
                    copyMessage.gone()
                }
            }
            copyClip.setOnClickListener {
                clipboard?.setPrimaryClip(ClipData.newPlainText("label", if (userId != null) msg?.substring(msg.indexOf(':') + 2) else msg))
                dismiss()
            }
            if (requireContext().prefs().getBoolean(C.DEBUG_CHAT_FULLMSG, false) && fullMsg != null) {
                copyFullMsg.visible()
                copyFullMsg.setOnClickListener {
                    clipboard?.setPrimaryClip(ClipData.newPlainText("label", fullMsg))
                    dismiss()
                }
            }
            if (args.host) {
                watchHost.visible()
                watchHost.setOnClickListener {
                    listener.onHostClicked()
                    dismiss()
                }
            }
        }
    }

    private fun updateUserLayout(user: User) {
        with(binding) {
            if (user.bannerImageURL != null) {
                userLayout.visible()
                bannerImage.visible()
                bannerImage.loadImage(requireParentFragment(), user.bannerImageURL)
            } else {
                bannerImage.gone()
            }
            if (user.channelLogo != null) {
                userLayout.visible()
                userImage.visible()
                userImage.loadImage(requireParentFragment(), user.channelLogo, circle = true)
                userImage.setOnClickListener {
                    listener.onViewProfileClicked(user.id, user.login, user.display_name, user.channelLogo)
                    dismiss()
                }
            } else {
                userImage.gone()
            }
            if (user.display_name != null) {
                userLayout.visible()
                userName.visible()
                userName.text = user.display_name
                userName.setOnClickListener {
                    listener.onViewProfileClicked(user.id, user.login, user.display_name, user.channelLogo)
                    dismiss()
                }
                if (user.bannerImageURL != null) {
                    userName.setShadowLayer(4f, 0f, 0f, Color.BLACK)
                }
            } else {
                userName.gone()
            }
            if (user.created_at != null) {
                userLayout.visible()
                userCreated.visible()
                userCreated.text = requireContext().getString(R.string.created_at, TwitchApiHelper.formatTimeString(requireContext(), user.created_at))
                if (user.bannerImageURL != null) {
                    userCreated.setTextColor(Color.LTGRAY)
                    userCreated.setShadowLayer(4f, 0f, 0f, Color.BLACK)
                }
            } else {
                userCreated.gone()
            }
            if (user.followedAt != null) {
                userLayout.visible()
                userFollowed.visible()
                userFollowed.text = requireContext().getString(R.string.followed_at, TwitchApiHelper.formatTimeString(requireContext(), user.followedAt))
                if (user.bannerImageURL != null) {
                    userFollowed.setTextColor(Color.LTGRAY)
                    userFollowed.setShadowLayer(4f, 0f, 0f, Color.BLACK)
                }
            } else {
                userFollowed.gone()
            }
            if (!userImage.isVisible && !userName.isVisible) {
                viewProfile.visible()
            }
        }
    }

    private fun extractUserName(text: String): String {
        val userName = StringBuilder()
        for (c in text) {
            if (!c.isWhitespace()) {
                if (c != ':') {
                    userName.append(c)
                } else {
                    break
                }
            }
        }
        return userName.toString()
    }
}