package com.github.andreyasadchy.xtra.ui.view.chat

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.MultiAutoCompleteTextView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.databinding.ViewChatBinding
import com.github.andreyasadchy.xtra.model.chat.*
import com.github.andreyasadchy.xtra.ui.common.ChatAdapter
import com.github.andreyasadchy.xtra.ui.view.SlidingLayout
import com.github.andreyasadchy.xtra.util.*
import com.github.andreyasadchy.xtra.util.chat.Command
import com.github.andreyasadchy.xtra.util.chat.PointsEarned
import com.github.andreyasadchy.xtra.util.chat.Raid
import com.github.andreyasadchy.xtra.util.chat.RoomState
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.extensions.LayoutContainer
import kotlin.math.max

class ChatView : ConstraintLayout {

    interface MessageSenderCallback {
        fun send(message: CharSequence)
    }

    interface RaidCallback {
        fun onRaidClicked()
        fun onRaidClose()
    }

    private var _binding: ViewChatBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ChatAdapter

    private var isChatTouched = false
    private var showFlexbox = false

    private var hasRecentEmotes: Boolean? = null
    private var emotesAddedCount = 0

    private var autoCompleteList: MutableList<Any>? = null
    private var autoCompleteAdapter: AutoCompleteAdapter? = null

    private lateinit var fragment: Fragment
    private var messagingEnabled = false

    private var messageCallback: MessageSenderCallback? = null
    private var raidCallback: RaidCallback? = null

    private val rewardList = mutableListOf<Pair<LiveChatMessage?, PubSubPointReward?>>()

    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            toggleEmoteMenu(false)
        }
    }

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context)
    }

    private fun init(context: Context) {
        _binding = ViewChatBinding.inflate(LayoutInflater.from(context), this, true)
    }

    fun init(fragment: Fragment) {
        this.fragment = fragment
        with(binding) {
            adapter = ChatAdapter(
                fragment = fragment,
                emoteSize = context.convertDpToPixels(29.5f),
                badgeSize = context.convertDpToPixels(18.5f),
                randomColor = context.prefs().getBoolean(C.CHAT_RANDOMCOLOR, true),
                boldNames = context.prefs().getBoolean(C.CHAT_BOLDNAMES, false),
                emoteQuality = context.prefs().getString(C.CHAT_IMAGE_QUALITY, "4") ?: "4",
                animateGifs = context.prefs().getBoolean(C.ANIMATED_EMOTES, true),
                enableZeroWidth = context.prefs().getBoolean(C.CHAT_ZEROWIDTH, true),
                enableTimestamps = context.prefs().getBoolean(C.CHAT_TIMESTAMPS, false),
                timestampFormat = context.prefs().getString(C.CHAT_TIMESTAMP_FORMAT, "0"),
                firstMsgVisibility = context.prefs().getString(C.CHAT_FIRSTMSG_VISIBILITY, "0"),
                firstChatMsg = context.getString(R.string.chat_first),
                rewardChatMsg = context.getString(R.string.chat_reward),
                redeemedChatMsg = context.getString(R.string.redeemed),
                redeemedNoMsg = context.getString(R.string.user_redeemed),
                imageLibrary = context.prefs().getString(C.CHAT_IMAGE_LIBRARY, "0")
            )
            recyclerView.let {
                it.adapter = adapter
                it.itemAnimator = null
                it.layoutManager = LinearLayoutManager(context).apply { stackFromEnd = true }
                it.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        super.onScrollStateChanged(recyclerView, newState)
                        isChatTouched = newState == RecyclerView.SCROLL_STATE_DRAGGING
                        btnDown.isVisible = shouldShowButton()
                        if (showFlexbox && flexbox.isGone) {
                            flexbox.visible()
                            flexbox.postDelayed({ flexbox.gone() }, 5000)
                        }
                    }
                })
            }
            btnDown.setOnClickListener {
                post {
                    recyclerView.scrollToPosition(adapter.messages!!.lastIndex)
                    it.toggleVisibility()
                }
            }
        }
    }

    fun submitList(list: MutableList<ChatMessage>) {
        adapter.messages = list
    }

    fun notifyMessageAdded() {
        with(binding) {
            adapter.messages!!.apply {
                adapter.notifyItemInserted(lastIndex)
                val messageLimit = context.prefs().getInt(C.CHAT_LIMIT, 600)
                if (size >= (messageLimit + 1)) {
                    val removeCount = size - messageLimit
                    repeat(removeCount) {
                        removeAt(0)
                    }
                    adapter.notifyItemRangeRemoved(0, removeCount)
                }
                if (!isChatTouched && btnDown.isGone) {
                    recyclerView.scrollToPosition(lastIndex)
                }
            }
        }
    }

    fun notifyEmotesLoaded() {
        adapter.messages?.size?.let { adapter.notifyItemRangeChanged(it - 40, 40) }
    }

    fun notifyRoomState(roomState: RoomState) {
        with(binding) {
            if (roomState.emote != null) {
                when (roomState.emote) {
                    "0" -> textEmote.gone()
                    "1" -> textEmote.visible()
                }
            }
            if (roomState.followers != null) {
                when (roomState.followers) {
                    "-1" -> textFollowers.gone()
                    "0" -> {
                        textFollowers.text = context.getString(R.string.room_followers)
                        textFollowers.visible()
                    }
                    else -> {
                        textFollowers.text = context.getString(R.string.room_followers_min, TwitchApiHelper.getDurationFromSeconds(context, (roomState.followers.toInt() * 60).toString()))
                        textFollowers.visible()
                    }
                }
            }
            if (roomState.unique != null) {
                when (roomState.unique) {
                    "0" -> textUnique.gone()
                    "1" -> textUnique.visible()
                }
            }
            if (roomState.slow != null) {
                when (roomState.slow) {
                    "0" -> textSlow.gone()
                    else -> {
                        textSlow.text = context.getString(R.string.room_slow, TwitchApiHelper.getDurationFromSeconds(context, roomState.slow))
                        textSlow.visible()
                    }
                }
            }
            if (roomState.subs != null) {
                when (roomState.subs) {
                    "0" -> textSubs.gone()
                    "1" -> textSubs.visible()
                }
            }
            if (textEmote.isGone && textFollowers.isGone && textUnique.isGone && textSlow.isGone && textSubs.isGone) {
                showFlexbox = false
                flexbox.gone()
            } else {
                showFlexbox = true
                flexbox.visible()
                flexbox.postDelayed({ flexbox.gone() }, 5000)
            }
        }
    }

    fun notifyCommand(command: Command) {
        val message = when (command.type) {
            "join" -> context.getString(R.string.chat_join, command.message)
            "disconnect" -> context.getString(R.string.chat_disconnect, command.message, command.duration)
            "disconnect_command" -> {
                raidCallback?.onRaidClose()
                hideRaid()
                notifyRoomState(RoomState("0", "-1", "0", "0", "0"))
                adapter.messages?.clear()
                context.getString(R.string.disconnected)
            }
            "send_msg_error" -> context.getString(R.string.chat_send_msg_error, command.message)
            "socket_error" -> context.getString(R.string.chat_socket_error, command.message)
            "notice" -> {
                when (command.duration) { // msg-id
                    "unraid_success" -> hideRaid()
                }
                TwitchApiHelper.getNoticeString(context, command.duration, command.message)
            }
            "clearmsg" -> context.getString(R.string.chat_clearmsg, command.message, command.duration)
            "clearchat" -> context.getString(R.string.chat_clear)
            "timeout" -> context.getString(R.string.chat_timeout, command.message, TwitchApiHelper.getDurationFromSeconds(context, command.duration))
            "ban" -> context.getString(R.string.chat_ban, command.message)
            "stream_live" -> context.getString(R.string.stream_live, command.duration)
            "stream_offline" -> context.getString(R.string.stream_offline, command.duration)
            else -> command.message
        }
        adapter.messages?.add(LiveChatMessage(message = message, color = "#999999", isAction = true, emotes = command.emotes, timestamp = command.timestamp, fullMsg = command.fullMsg))
        notifyMessageAdded()
    }

    fun notifyReward(message: ChatMessage) {
        if (message is LiveChatMessage) {
            val item = rewardList.find { it.second?.id == message.rewardId && it.second?.userId == message.userId }
            if (item != null) {
                message.apply { pointReward = item.second }.let {
                    rewardList.remove(item)
                    adapter.messages?.add(it)
                    notifyMessageAdded()
                }
            } else {
                rewardList.add(Pair(message, null))
            }
        } else {
            if (message is PubSubPointReward) {
                val item = rewardList.find { it.first?.rewardId == message.id && it.first?.userId == message.userId }
                if (item != null) {
                    item.first?.apply { pointReward = message }?.let {
                        rewardList.remove(item)
                        adapter.messages?.add(it)
                        notifyMessageAdded()
                    }
                } else {
                    rewardList.add(Pair(null, message))
                }
            }
        }
    }

    fun notifyPointsEarned(points: PointsEarned) {
        val message = context.getString(R.string.points_earned, points.pointsGained)
        adapter.messages?.add(LiveChatMessage(message = message, color = "#999999", isAction = true, timestamp = points.timestamp, fullMsg = points.fullMsg))
        notifyMessageAdded()
    }

    fun notifyRaid(raid: Raid, newId: Boolean) {
        with(binding) {
            if (newId) {
                raidLayout.visible()
                raidLayout.setOnClickListener { raidCallback?.onRaidClicked() }
                raidImage.visible()
                raidImage.loadImage(fragment, raid.targetLogo, circle = true)
                raidText.visible()
                raidClose.visible()
                raidClose.setOnClickListener {
                    raidCallback?.onRaidClose()
                    hideRaid()
                }
            }
            raidText.text = context.getString(R.string.raid_text, raid.targetName, raid.viewerCount)
        }
    }

    fun hideRaid() {
        with(binding) {
            raidLayout.gone()
            raidImage.gone()
            raidText.gone()
            raidClose.gone()
        }
    }

    fun addRecentMessages(list: List<LiveChatMessage>) {
        adapter.messages?.addAll(0, list)
        adapter.messages?.lastIndex?.let { binding.recyclerView.scrollToPosition(it) }
    }

    fun addGlobalBadges(list: List<TwitchBadge>?) {
        if (list != null) {
            adapter.addGlobalBadges(list)
        }
    }

    fun addChannelBadges(list: List<TwitchBadge>) {
        adapter.addChannelBadges(list)
    }

    fun addCheerEmotes(list: List<CheerEmote>) {
        adapter.addCheerEmotes(list)
    }

    fun addEmotes(list: List<Emote>) {
        with(binding) {
            when (list.firstOrNull()) {
                is BttvEmote, is FfzEmote, is StvEmote -> {
                    adapter.addEmotes(list)
                    if (messagingEnabled) {
                        autoCompleteList!!.addAll(list)
                    }
                }
                is TwitchEmote -> {
                    if (messagingEnabled) {
                        autoCompleteList!!.addAll(list)
                    }
                }
                is RecentEmote -> hasRecentEmotes = true
            }
            if (messagingEnabled && ++emotesAddedCount == 3) { //TODO refactor to not wait
                autoCompleteAdapter = AutoCompleteAdapter(context, fragment, autoCompleteList!!, context.prefs().getString(C.CHAT_IMAGE_QUALITY, "4") ?: "4").apply {
                    setNotifyOnChange(false)
                    editText.setAdapter(this)

                    var previousSize = 0
                    editText.setOnFocusChangeListener { _, hasFocus ->
                        if (hasFocus && count != previousSize) {
                            previousSize = count
                            notifyDataSetChanged()
                        }
                        setNotifyOnChange(hasFocus)
                    }
                }
            }
        }
    }

    fun setUsername(username: String) {
        adapter.setUsername(username)
    }

    fun setChannelId(channelId: String?) {
        adapter.setChannelId(channelId)
    }

    fun setChatters(chatters: Collection<Chatter>?) {
        autoCompleteList = chatters?.toMutableList()
    }

    fun addChatter(chatter: Chatter) {
        autoCompleteAdapter?.add(chatter)
    }

    fun setCallback(callbackMessage: MessageSenderCallback, callbackRaid: RaidCallback?) {
        messageCallback = callbackMessage
        raidCallback = callbackRaid
    }

    fun emoteMenuIsVisible(): Boolean = binding.emoteMenu.isVisible

    fun toggleEmoteMenu(enable: Boolean) {
        if (enable) {
            binding.emoteMenu.visible()
        } else {
            binding.emoteMenu.gone()
        }
        toggleBackPressedCallback(enable)
    }

    fun toggleBackPressedCallback(enable: Boolean) {
        if (enable) {
            fragment.requireActivity().onBackPressedDispatcher.addCallback(fragment, backPressedCallback)
        } else {
            backPressedCallback.remove()
        }
    }

    fun appendEmote(emote: Emote) {
        binding.editText.text.append(emote.name).append(' ')
    }

    @SuppressLint("SetTextI18n")
    fun reply(userName: CharSequence) {
        val text = "@$userName "
        binding.editText.apply {
            setText(text)
            setSelection(text.length)
            showKeyboard()
        }
    }

    fun setMessage(text: CharSequence) {
        binding.editText.setText(text)
    }

    fun enableChatInteraction(enableMessaging: Boolean) {
        with(binding) {
            adapter.setOnClickListener { original, formatted, userId, channelId, fullMsg ->
                editText.hideKeyboard()
                editText.clearFocus()
                MessageClickedDialog.newInstance(enableMessaging, original, formatted, userId, channelId, fullMsg).show(fragment.childFragmentManager, "closeOnPip")
            }
            if (enableMessaging) {
                editText.addTextChangedListener(onTextChanged = { text, _, _, _ ->
                    if (text?.isNotBlank() == true) {
                        send.visible()
                        clear.visible()
                    } else {
                        send.gone()
                        clear.gone()
                    }
                })
                editText.setTokenizer(SpaceTokenizer())
                editText.setOnKeyListener { _, keyCode, event ->
                    if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                        sendMessage()
                    } else {
                        false
                    }
                }
                clear.setOnClickListener {
                    val text = editText.text.toString().trimEnd()
                    editText.setText(text.substring(0, max(text.lastIndexOf(' '), 0)))
                    editText.setSelection(editText.length())
                }
                clear.setOnLongClickListener {
                    editText.text.clear()
                    true
                }
                send.setOnClickListener { sendMessage() }
                if (parent != null && parent.parent is SlidingLayout && !context.prefs().getBoolean(C.KEY_CHAT_BAR_VISIBLE, true)) {
                    messageView.gone()
                } else {
                    messageView.visible()
                }
                viewPager.adapter = object : FragmentStateAdapter(fragment) {
                    override fun getItemCount(): Int = 3

                    override fun createFragment(position: Int): Fragment {
                        return EmotesFragment.newInstance(position)
                    }
                }
                viewPager.offscreenPageLimit = 2
                viewPager.reduceDragSensitivity()
                TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                    tab.text = when (position) {
                        0 -> context.getString(R.string.recent_emotes)
                        1 -> "Twitch"
                        else -> "7TV/BTTV/FFZ"
                    }
                }.attach()
                emotes.setOnClickListener {
                    //TODO add animation
                    if (emoteMenu.isGone) {
                        if (hasRecentEmotes != true && viewPager.currentItem == 0) {
                            viewPager.setCurrentItem(1, false)
                        }
                        toggleEmoteMenu(true)
                    } else {
                        toggleEmoteMenu(false)
                    }
                }
                messagingEnabled = true
            }
        }
    }

    override fun onDetachedFromWindow() {
        binding.recyclerView.adapter = null
        super.onDetachedFromWindow()
    }

    private fun sendMessage(): Boolean {
        with(binding) {
            editText.hideKeyboard()
            editText.clearFocus()
            toggleEmoteMenu(false)
            return messageCallback?.let {
                val text = editText.text.trim()
                editText.text.clear()
                if (text.isNotEmpty()) {
                    it.send(text)
                    adapter.messages?.let { messages -> recyclerView.scrollToPosition(messages.lastIndex) }
                    true
                } else {
                    false
                }
            } == true
        }
    }

    private fun shouldShowButton(): Boolean {
        with(binding) {
            val offset = recyclerView.computeVerticalScrollOffset()
            if (offset < 0) {
                return false
            }
            val extent = recyclerView.computeVerticalScrollExtent()
            val range = recyclerView.computeVerticalScrollRange()
            val percentage = (100f * offset / (range - extent).toFloat())
            return percentage < 100f
        }
    }

    class SpaceTokenizer : MultiAutoCompleteTextView.Tokenizer {

        override fun findTokenStart(text: CharSequence, cursor: Int): Int {
            var i = cursor

            while (i > 0 && text[i - 1] != ' ') {
                i--
            }
            while (i < cursor && text[i] == ' ') {
                i++
            }

            return i
        }

        override fun findTokenEnd(text: CharSequence, cursor: Int): Int {
            var i = cursor
            val len = text.length

            while (i < len) {
                if (text[i] == ' ') {
                    return i
                } else {
                    i++
                }
            }

            return len
        }

        override fun terminateToken(text: CharSequence): CharSequence {
            return "${if (text.startsWith(':')) text.substring(1) else text} "
        }
    }

    class AutoCompleteAdapter(
            context: Context,
            private val fragment: Fragment,
            list: List<Any>,
            private val emoteQuality: String) : ArrayAdapter<Any>(context, 0, list) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val viewHolder: ViewHolder

            val item = getItem(position)!!
            return when (getItemViewType(position)) {
                TYPE_EMOTE -> {
                    if (convertView == null) {
                        val view = LayoutInflater.from(context).inflate(R.layout.auto_complete_emotes_list_item, parent, false)
                        viewHolder = ViewHolder(view).also { view.tag = it }
                    } else {
                        viewHolder = convertView.tag as ViewHolder
                    }
                    viewHolder.containerView.apply {
                        item as Emote
                        findViewById<ImageView>(R.id.image)?.loadImage(fragment, when (emoteQuality) {
                            "4" -> item.url4x ?: item.url3x ?: item.url2x ?: item.url1x
                            "3" -> item.url3x ?: item.url2x ?: item.url1x
                            "2" -> item.url2x ?: item.url1x
                            else -> item.url1x
                        }, diskCacheStrategy = DiskCacheStrategy.DATA)
                        findViewById<TextView>(R.id.name)?.text = item.name
                    }
                }
                else -> {
                    if (convertView == null) {
                        val view = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, parent, false)
                        viewHolder = ViewHolder(view).also { view.tag = it }
                    } else {
                        viewHolder = convertView.tag as ViewHolder
                    }
                    (viewHolder.containerView as TextView).apply {
                        text = (item as Chatter).name
                    }
                }
            }
        }

        override fun getItemViewType(position: Int): Int {
            return if (getItem(position) is Emote) TYPE_EMOTE else TYPE_USERNAME
        }

        override fun getViewTypeCount(): Int = 2

        class ViewHolder(override val containerView: View) : LayoutContainer

        private companion object {
            const val TYPE_EMOTE = 0
            const val TYPE_USERNAME = 1
        }
    }
}
