package com.github.andreyasadchy.xtra.ui.chat

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.andreyasadchy.xtra.model.Account
import com.github.andreyasadchy.xtra.model.chat.BttvEmote
import com.github.andreyasadchy.xtra.model.chat.ChatMessage
import com.github.andreyasadchy.xtra.model.chat.Chatter
import com.github.andreyasadchy.xtra.model.chat.CheerEmote
import com.github.andreyasadchy.xtra.model.chat.Emote
import com.github.andreyasadchy.xtra.model.chat.FfzEmote
import com.github.andreyasadchy.xtra.model.chat.LiveChatMessage
import com.github.andreyasadchy.xtra.model.chat.PubSubPointReward
import com.github.andreyasadchy.xtra.model.chat.RecentEmote
import com.github.andreyasadchy.xtra.model.chat.StvEmote
import com.github.andreyasadchy.xtra.model.chat.TwitchBadge
import com.github.andreyasadchy.xtra.model.chat.TwitchEmote
import com.github.andreyasadchy.xtra.repository.ApiRepository
import com.github.andreyasadchy.xtra.repository.PlayerRepository
import com.github.andreyasadchy.xtra.ui.player.ChatReplayManager
import com.github.andreyasadchy.xtra.ui.view.chat.ChatView
import com.github.andreyasadchy.xtra.util.SingleLiveEvent
import com.github.andreyasadchy.xtra.util.TwitchApiHelper
import com.github.andreyasadchy.xtra.util.chat.Command
import com.github.andreyasadchy.xtra.util.chat.LiveChatListener
import com.github.andreyasadchy.xtra.util.chat.LiveChatThread
import com.github.andreyasadchy.xtra.util.chat.LoggedInChatThread
import com.github.andreyasadchy.xtra.util.chat.OnChatMessageReceivedListener
import com.github.andreyasadchy.xtra.util.chat.PointsEarned
import com.github.andreyasadchy.xtra.util.chat.PubSubListener
import com.github.andreyasadchy.xtra.util.chat.PubSubWebSocket
import com.github.andreyasadchy.xtra.util.chat.Raid
import com.github.andreyasadchy.xtra.util.chat.RoomState
import com.github.andreyasadchy.xtra.util.nullIfEmpty
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlin.collections.set

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ApiRepository,
    private val playerRepository: PlayerRepository,
    private val okHttpClient: OkHttpClient) : ViewModel(), ChatView.ChatViewCallback {

    val recentEmotes: LiveData<List<Emote>> by lazy {
        MediatorLiveData<List<Emote>>().apply {
            addSource(userEmotes) { user ->
                removeSource(userEmotes)
                addSource(_otherEmotes) { other ->
                    removeSource(_otherEmotes)
                    addSource(playerRepository.loadRecentEmotes()) { recent ->
                        value = recent.mapNotNull { emote -> (user + other).find { it.name == emote.name } }
                    }
                }
            }
        }
    }
    val userEmotes = MutableLiveData<List<Emote>>()
    private val _otherEmotes = MutableLiveData<List<Emote>>()
    val otherEmotes: LiveData<List<Emote>>
        get() = _otherEmotes

    val globalStvEmotes = MutableLiveData<List<Emote>>()
    val channelStvEmotes = MutableLiveData<List<Emote>>()
    val globalBttvEmotes = MutableLiveData<List<Emote>>()
    val channelBttvEmotes = MutableLiveData<List<Emote>>()
    val globalFfzEmotes = MutableLiveData<List<Emote>>()
    val channelFfzEmotes = MutableLiveData<List<Emote>>()
    val globalBadges = MutableLiveData<List<TwitchBadge>>()
    val channelBadges = MutableLiveData<List<TwitchBadge>>()
    val cheerEmotes = MutableLiveData<List<CheerEmote>>()
    val roomState = MutableLiveData<RoomState>()
    private var showRaids = false
    val raid = MutableLiveData<Raid>()
    val raidClicked = MutableLiveData<Boolean>()
    var raidAutoSwitch = false
    var raidNewId = true
    var raidClosed = false
    val viewerCount = MutableLiveData<Int?>()
    var streamId: String? = null
    private val rewardList = mutableListOf<Pair<LiveChatMessage?, PubSubPointReward?>>()

    private val _reloadMessages by lazy { SingleLiveEvent<Boolean>() }
    val reloadMessages: LiveData<Boolean>
        get() = _reloadMessages
    private val _scrollDown by lazy { SingleLiveEvent<Boolean>() }
    val scrollDown: LiveData<Boolean>
        get() = _scrollDown
    private val _command by lazy { SingleLiveEvent<Command>() }
    val command: LiveData<Command>
        get() = _command
    private val _pointsEarned by lazy { SingleLiveEvent<PointsEarned>() }
    val pointsEarned: LiveData<PointsEarned>
        get() = _pointsEarned

    private var messageLimit = 600
    private val _chatMessages by lazy {
        MutableLiveData<MutableList<ChatMessage>>().apply { value = Collections.synchronizedList(ArrayList(messageLimit + 1)) }
    }
    val chatMessages: LiveData<MutableList<ChatMessage>>
        get() = _chatMessages
    private val _newMessage by lazy { MutableLiveData<ChatMessage>() }
    val newMessage: LiveData<ChatMessage>
        get() = _newMessage

    var chat: ChatController? = null

    private val _newChatter by lazy { SingleLiveEvent<Chatter>() }
    val newChatter: LiveData<Chatter>
        get() = _newChatter

    val chatters: Collection<Chatter>?
        get() = (chat as? LiveChatController)?.chatters?.values

    fun startLive(useSSL: Boolean, usePubSub: Boolean, account: Account, isLoggedIn: Boolean, helixClientId: String?, gqlClientId: String?, gqlClientId2: String?, channelId: String?, channelLogin: String?, channelName: String?, streamId: String?, messageLimit: Int, emoteQuality: String, animateGifs: Boolean, showUserNotice: Boolean, showClearMsg: Boolean, showClearChat: Boolean, collectPoints: Boolean, notifyPoints: Boolean, showRaids: Boolean, autoSwitchRaids: Boolean, enableRecentMsg: Boolean, recentMsgLimit: String, enableStv: Boolean, enableBttv: Boolean, enableFfz: Boolean, useApiCommands: Boolean) {
        if (chat == null && channelLogin != null) {
            this.messageLimit = messageLimit
            this.streamId = streamId
            this.showRaids = showRaids
            raidAutoSwitch = autoSwitchRaids
            chat = LiveChatController(
                useSSL = useSSL,
                usePubSub = usePubSub,
                account = account,
                isLoggedIn = isLoggedIn,
                helixClientId = helixClientId,
                gqlClientId = gqlClientId,
                gqlClientId2 = gqlClientId2,
                channelId = channelId,
                channelLogin = channelLogin,
                channelName = channelName,
                animateGifs = animateGifs,
                showUserNotice = showUserNotice,
                showClearMsg = showClearMsg,
                showClearChat = showClearChat,
                collectPoints = collectPoints,
                notifyPoints = notifyPoints,
                useApiCommands = useApiCommands
            )
            chat?.start()
            loadEmotes(
                helixClientId = helixClientId,
                helixToken = account.helixToken,
                gqlClientId = gqlClientId,
                channelId = channelId,
                channelLogin = channelLogin,
                emoteQuality = emoteQuality,
                animateGifs = animateGifs,
                enableStv = enableStv,
                enableBttv = enableBttv,
                enableFfz = enableFfz
            )
            if (enableRecentMsg) {
                loadRecentMessages(channelLogin, recentMsgLimit)
            }
            if (isLoggedIn) {
                (chat as? LiveChatController)?.loadUserEmotes()
            }
        }
    }

    fun startReplay(account: Account, helixClientId: String?, gqlClientId: String?, channelId: String?, channelLogin: String?, videoId: String, startTime: Double, getCurrentPosition: () -> Double, messageLimit: Int, emoteQuality: String, animateGifs: Boolean, enableStv: Boolean, enableBttv: Boolean, enableFfz: Boolean) {
        if (chat == null) {
            this.messageLimit = messageLimit
            chat = VideoChatController(
                clientId = gqlClientId,
                videoId = videoId,
                startTime = startTime,
                getCurrentPosition = getCurrentPosition
            )
            chat?.start()
            loadEmotes(
                helixClientId = helixClientId,
                helixToken = account.helixToken,
                gqlClientId = gqlClientId,
                channelId = channelId,
                channelLogin = channelLogin,
                emoteQuality = emoteQuality,
                animateGifs = animateGifs,
                enableStv = enableStv,
                enableBttv = enableBttv,
                enableFfz = enableFfz
            )
        }
    }

    fun start() {
        chat?.start()
    }

    fun stop() {
        chat?.pause()
    }

    override fun send(message: CharSequence) {
        chat?.send(message)
    }

    override fun onRaidClicked() {
        raidAutoSwitch = false
        raidClicked.postValue(true)
    }

    override fun onRaidClose() {
        raidAutoSwitch = false
        raidClosed = true
    }

    override fun onCleared() {
        chat?.stop()
        super.onCleared()
    }

    private fun loadEmotes(helixClientId: String?, helixToken: String?, gqlClientId: String?, channelId: String?, channelLogin: String?, emoteQuality: String, animateGifs: Boolean, enableStv: Boolean, enableBttv: Boolean, enableFfz: Boolean) {
        val list = mutableListOf<Emote>()
        savedGlobalBadges.also { saved ->
            if (!saved.isNullOrEmpty()) {
                globalBadges.value = saved
                _reloadMessages.value = true
            } else {
                viewModelScope.launch {
                    try {
                        repository.loadGlobalBadges(helixClientId, helixToken, gqlClientId, emoteQuality).let { badges ->
                            if (badges.isNotEmpty()) {
                                savedGlobalBadges = badges
                                globalBadges.value = badges
                                _reloadMessages.value = true
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to load global badges", e)
                    }
                }
            }
        }
        if (enableStv) {
            savedGlobalStvEmotes.also { saved ->
                if (!saved.isNullOrEmpty()) {
                    (chat as? LiveChatController)?.addEmotes(saved)
                    list.addAll(saved)
                    _otherEmotes.value = list.sortedBy { emote -> emote is StvEmote }.sortedBy { emote -> emote is BttvEmote }.sortedBy { emote -> emote is FfzEmote }
                    globalStvEmotes.postValue(saved)
                    _reloadMessages.value = true
                } else {
                    viewModelScope.launch {
                        try {
                            playerRepository.loadGlobalStvEmotes().body()?.emotes?.let { emotes ->
                                if (emotes.isNotEmpty()) {
                                    savedGlobalStvEmotes = emotes
                                    (chat as? LiveChatController)?.addEmotes(emotes)
                                    list.addAll(emotes)
                                    _otherEmotes.value = list.sortedBy { emote -> emote is StvEmote }.sortedBy { emote -> emote is BttvEmote }.sortedBy { emote -> emote is FfzEmote }
                                    globalStvEmotes.postValue(emotes)
                                    _reloadMessages.value = true
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to load global 7tv emotes", e)
                        }
                    }
                }
            }
            if (!channelId.isNullOrBlank()) {
                viewModelScope.launch {
                    try {
                        playerRepository.loadStvEmotes(channelId).body()?.emotes?.let {
                            if (it.isNotEmpty()) {
                                (chat as? LiveChatController)?.addEmotes(it)
                                list.addAll(it)
                                _otherEmotes.value = list.sortedBy { emote -> emote is StvEmote }.sortedBy { emote -> emote is BttvEmote }.sortedBy { emote -> emote is FfzEmote }
                                channelStvEmotes.postValue(it)
                                _reloadMessages.value = true
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to load 7tv emotes for channel $channelId", e)
                    }
                }
            }
        }
        if (enableBttv) {
            savedGlobalBttvEmotes.also { saved ->
                if (!saved.isNullOrEmpty()) {
                    (chat as? LiveChatController)?.addEmotes(saved)
                    list.addAll(saved)
                    _otherEmotes.value = list.sortedBy { emote -> emote is StvEmote }.sortedBy { emote -> emote is BttvEmote }.sortedBy { emote -> emote is FfzEmote }
                    globalBttvEmotes.postValue(saved)
                    _reloadMessages.value = true
                } else {
                    viewModelScope.launch {
                        try {
                            playerRepository.loadGlobalBttvEmotes().body()?.emotes?.let { emotes ->
                                if (emotes.isNotEmpty()) {
                                    savedGlobalBttvEmotes = emotes
                                    (chat as? LiveChatController)?.addEmotes(emotes)
                                    list.addAll(emotes)
                                    _otherEmotes.value = list.sortedBy { emote -> emote is StvEmote }.sortedBy { emote -> emote is BttvEmote }.sortedBy { emote -> emote is FfzEmote }
                                    globalBttvEmotes.postValue(emotes)
                                    _reloadMessages.value = true
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to load global BTTV emotes", e)
                        }
                    }
                }
            }
            if (!channelId.isNullOrBlank()) {
                viewModelScope.launch {
                    try {
                        playerRepository.loadBttvEmotes(channelId).body()?.emotes?.let {
                            if (it.isNotEmpty()) {
                                (chat as? LiveChatController)?.addEmotes(it)
                                list.addAll(it)
                                _otherEmotes.value = list.sortedBy { emote -> emote is StvEmote }.sortedBy { emote -> emote is BttvEmote }.sortedBy { emote -> emote is FfzEmote }
                                channelBttvEmotes.postValue(it)
                                _reloadMessages.value = true
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to load BTTV emotes for channel $channelId", e)
                    }
                }
            }
        }
        if (enableFfz) {
            savedGlobalFfzEmotes.also { saved ->
                if (!saved.isNullOrEmpty()) {
                    (chat as? LiveChatController)?.addEmotes(saved)
                    list.addAll(saved)
                    _otherEmotes.value = list.sortedBy { emote -> emote is StvEmote }.sortedBy { emote -> emote is BttvEmote }.sortedBy { emote -> emote is FfzEmote }
                    globalFfzEmotes.postValue(saved)
                    _reloadMessages.value = true
                } else {
                    viewModelScope.launch {
                        try {
                            playerRepository.loadGlobalFfzEmotes().body()?.emotes?.let { emotes ->
                                if (emotes.isNotEmpty()) {
                                    savedGlobalFfzEmotes = emotes
                                    (chat as? LiveChatController)?.addEmotes(emotes)
                                    list.addAll(emotes)
                                    _otherEmotes.value = list.sortedBy { emote -> emote is StvEmote }.sortedBy { emote -> emote is BttvEmote }.sortedBy { emote -> emote is FfzEmote }
                                    globalFfzEmotes.postValue(emotes)
                                    _reloadMessages.value = true
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to load global FFZ emotes", e)
                        }
                    }
                }
            }
            if (!channelId.isNullOrBlank()) {
                viewModelScope.launch {
                    try {
                        playerRepository.loadFfzEmotes(channelId).body()?.emotes?.let {
                            if (it.isNotEmpty()) {
                                (chat as? LiveChatController)?.addEmotes(it)
                                list.addAll(it)
                                _otherEmotes.value = list.sortedBy { emote -> emote is StvEmote }.sortedBy { emote -> emote is BttvEmote }.sortedBy { emote -> emote is FfzEmote }
                                channelFfzEmotes.postValue(it)
                                _reloadMessages.value = true
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to load FFZ emotes for channel $channelId", e)
                    }
                }
            }
        }
        if (!channelId.isNullOrBlank() || !channelLogin.isNullOrBlank()) {
            viewModelScope.launch {
                try {
                    repository.loadChannelBadges(helixClientId, helixToken, gqlClientId, channelId, channelLogin, emoteQuality).let {
                        if (it.isNotEmpty()) {
                            channelBadges.postValue(it)
                            _reloadMessages.value = true
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load badges for channel $channelId", e)
                }
            }
            viewModelScope.launch {
                try {
                    repository.loadCheerEmotes(helixClientId, helixToken, gqlClientId, channelId, channelLogin, animateGifs).let {
                        if (it.isNotEmpty()) {
                            cheerEmotes.postValue(it)
                            _reloadMessages.value = true
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load cheermotes for channel $channelId", e)
                }
            }
        }
    }

    fun loadRecentMessages(channelLogin: String, recentMsgLimit: String) {
        viewModelScope.launch {
            try {
                playerRepository.loadRecentMessages(channelLogin, recentMsgLimit).body()?.messages?.let {
                    if (it.isNotEmpty()) {
                        _chatMessages.postValue(_chatMessages.value?.apply { addAll(0, it) })
                        _scrollDown.postValue(true)
                        _reloadMessages.value = true
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load recent messages for channel $channelLogin", e)
            }
        }
    }

    fun reloadEmotes(helixClientId: String?, helixToken: String?, gqlClientId: String?, channelId: String?, channelLogin: String?, emoteQuality: String, animateGifs: Boolean, enableStv: Boolean, enableBttv: Boolean, enableFfz: Boolean) {
        savedGlobalBadges = null
        savedGlobalStvEmotes = null
        savedGlobalBttvEmotes = null
        savedGlobalFfzEmotes = null
        loadEmotes(helixClientId, helixToken, gqlClientId, channelId, channelLogin, emoteQuality, animateGifs, enableStv, enableBttv, enableFfz)
    }

    inner class LiveChatController(
        private val useSSL: Boolean,
        private val usePubSub: Boolean,
        private val account: Account,
        private val isLoggedIn: Boolean,
        private val helixClientId: String?,
        private val gqlClientId: String?,
        private val gqlClientId2: String?,
        private val channelId: String?,
        private val channelLogin: String,
        channelName: String?,
        private val animateGifs: Boolean,
        private val showUserNotice: Boolean,
        private val showClearMsg: Boolean,
        private val showClearChat: Boolean,
        private val collectPoints: Boolean,
        private val notifyPoints: Boolean,
        private val useApiCommands: Boolean) : ChatController(), LiveChatListener, PubSubListener {

        private var chat: LiveChatThread? = null
        private var loggedInChat: LoggedInChatThread? = null
        private var pubSub: PubSubWebSocket? = null
        private val allEmotes = mutableListOf<Emote>()
        private var usedRaidId: String? = null

        val chatters = ConcurrentHashMap<String?, Chatter>()

        init {
            addChatter(channelName)
        }

        private fun addChatter(displayName: String?) {
            if (displayName != null && !chatters.containsKey(displayName)) {
                val chatter = Chatter(displayName)
                chatters[displayName] = chatter
                _newChatter.postValue(chatter)
            }
        }

        override fun send(message: CharSequence) {
            if (useApiCommands) {
                if (message.toString().startsWith("/")) {
                    sendCommand(message)
                } else {
                    sendMessage(message)
                }
            } else {
                if (message.toString() == "/dc" || message.toString() == "/disconnect") {
                    if (chat?.isActive == true) {
                        disconnect()
                    }
                } else {
                    loggedInChat?.send(message)
                    val usedEmotes = hashSetOf<RecentEmote>()
                    val currentTime = System.currentTimeMillis()
                    message.split(' ').forEach { word ->
                        allEmotes.find { it.name == word }?.let { usedEmotes.add(RecentEmote(word, currentTime)) }
                    }
                    if (usedEmotes.isNotEmpty()) {
                        playerRepository.insertRecentEmotes(usedEmotes)
                    }
                }
            }
        }

        override fun start() {
            pause()
            chat = TwitchApiHelper.startChat(useSSL, isLoggedIn, channelLogin, showUserNotice, showClearMsg, showClearChat, usePubSub, this, this)
            if (isLoggedIn) {
                loggedInChat = TwitchApiHelper.startLoggedInChat(useSSL, account.login, account.gqlToken?.nullIfEmpty() ?: account.helixToken, channelLogin, showUserNotice, showClearMsg, showClearChat, usePubSub, this, this)
            }
            if (usePubSub && !channelId.isNullOrBlank()) {
                pubSub = TwitchApiHelper.startPubSub(channelId, account.id, account.gqlToken, collectPoints, notifyPoints, showRaids, okHttpClient, viewModelScope, this, this)
            }
        }

        override fun pause() {
            chat?.disconnect()
            loggedInChat?.disconnect()
            pubSub?.disconnect()
        }

        override fun stop() {
            pause()
        }

        override fun onMessage(message: ChatMessage) {
            super.onMessage(message)
            addChatter(message.userName)
        }

        override fun onCommand(list: Command) {
            _command.postValue(list)
        }

        override fun onRoomState(list: RoomState) {
            roomState.postValue(list)
        }

        fun loadUserEmotes() {
            savedUserEmotes.also { saved ->
                if (!saved.isNullOrEmpty()) {
                    addEmotes(saved)
                    userEmotes.postValue(saved)
                } else {
                    viewModelScope.launch {
                        try {
                            when {
                                !gqlClientId.isNullOrBlank() && !account.gqlToken.isNullOrBlank() -> repository.loadUserEmotes(gqlClientId, account.gqlToken, channelId)
                                !savedEmoteSets.isNullOrEmpty() && !helixClientId.isNullOrBlank() && !account.helixToken.isNullOrBlank() -> {
                                    val emotes = mutableListOf<TwitchEmote>()
                                    savedEmoteSets?.chunked(25)?.forEach { list ->
                                        repository.loadEmotesFromSet(helixClientId, account.helixToken, list, animateGifs)
                                    }
                                    emotes
                                }
                                else -> null
                            }?.let { emotes ->
                                if (emotes.isNotEmpty()) {
                                    val sorted = emotes.sortedByDescending { it.ownerId == channelId }
                                    savedUserEmotes = sorted
                                    addEmotes(sorted)
                                    userEmotes.postValue(sorted)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to load user emotes", e)
                        }
                    }
                }
            }
        }

        override fun onUserState(emoteSets: List<String>?) {
            if (savedEmoteSets != emoteSets) {
                savedEmoteSets = emoteSets
                if (savedUserEmotes == null) {
                    loadUserEmotes()
                }
            }
        }

        override fun onPlaybackMessage(live: Boolean?, viewers: Int?) {
            live?.let {
                if (it) {
                    _command.postValue(Command(duration = channelLogin, type = "stream_live"))
                } else {
                    _command.postValue(Command(duration = channelLogin, type = "stream_offline"))
                }
            }
            viewerCount.postValue(viewers)
        }

        override fun onRewardMessage(message: ChatMessage) {
            when (message) {
                is LiveChatMessage -> {
                    val item = rewardList.find { it.second?.id == message.rewardId && it.second?.userId == message.userId }
                    if (item != null) {
                        message.apply { pointReward = item.second }.let {
                            rewardList.remove(item)
                            onMessage(it)
                        }
                    } else {
                        rewardList.add(Pair(message, null))
                    }
                }
                is PubSubPointReward -> {
                    val item = rewardList.find { it.first?.rewardId == message.id && it.first?.userId == message.userId }
                    if (item != null) {
                        item.first?.apply { pointReward = message }?.let {
                            rewardList.remove(item)
                            onMessage(it)
                        }
                    } else {
                        rewardList.add(Pair(null, message))
                    }
                }
            }
        }

        override fun onPointsEarned(message: PointsEarned) {
            _pointsEarned.postValue(message)
        }

        override fun onClaimAvailable() {
            if (!gqlClientId2.isNullOrBlank() && !account.gqlToken2.isNullOrBlank()) {
                viewModelScope.launch {
                    repository.loadClaimPoints(gqlClientId2, account.gqlToken2, channelId, channelLogin)
                }
            }
        }

        override fun onMinuteWatched() {
            if (!streamId.isNullOrBlank()) {
                viewModelScope.launch {
                    repository.loadMinuteWatched(account.id, streamId, channelId, channelLogin)
                }
            }
        }

        override fun onRaidUpdate(message: Raid) {
            raidNewId = message.raidId != usedRaidId
            raid.postValue(message)
            if (raidNewId) {
                usedRaidId = message.raidId
                if (collectPoints && !gqlClientId2.isNullOrBlank() && !account.gqlToken2.isNullOrBlank()) {
                    viewModelScope.launch {
                        repository.loadJoinRaid(gqlClientId2, account.gqlToken2, message.raidId)
                    }
                }
            }
        }

        fun addEmotes(list: List<Emote>) {
            allEmotes.addAll(list.filter { it !in allEmotes })
        }

        fun isActive(): Boolean? {
            return chat?.isActive
        }

        fun disconnect() {
            if (chat?.isActive == true) {
                chat?.disconnect()
                loggedInChat?.disconnect()
                pubSub?.disconnect()
                usedRaidId = null
                onRaidClose()
                _chatMessages.postValue(null)
                _command.postValue(Command(type = "disconnect_command"))
            }
        }

        private fun sendMessage(message: CharSequence) {
            loggedInChat?.send(message)
            val usedEmotes = hashSetOf<RecentEmote>()
            val currentTime = System.currentTimeMillis()
            message.split(' ').forEach { word ->
                allEmotes.find { it.name == word }?.let { usedEmotes.add(RecentEmote(word, currentTime)) }
            }
            if (usedEmotes.isNotEmpty()) {
                playerRepository.insertRecentEmotes(usedEmotes)
            }
        }

        private fun sendCommand(message: CharSequence) {
            val command = message.toString().substringBefore(" ")
            when {
                command.startsWith("/announce", true) -> {
                    val splits = message.split(" ", limit = 2)
                    if (splits.size >= 2) {
                        viewModelScope.launch {
                            repository.sendAnnouncement(
                                helixClientId = helixClientId,
                                helixToken = account.helixToken,
                                userId = account.id,
                                gqlClientId = gqlClientId2,
                                gqlToken = account.gqlToken2,
                                channelId = channelId,
                                message = splits[1],
                                color = splits[0].substringAfter("/announce", "").ifBlank { null }
                            )?.let { onMessage(LiveChatMessage(message = it, color = "#999999", isAction = true)) }
                        }
                    }
                }
                command.equals("/ban", true) -> {
                    val splits = message.split(" ", limit = 3)
                    if (splits.size >= 2) {
                        viewModelScope.launch {
                            repository.banUser(
                                helixClientId = helixClientId,
                                helixToken = account.helixToken,
                                userId = account.id,
                                gqlClientId = gqlClientId2,
                                gqlToken = account.gqlToken2,
                                channelId = channelId,
                                targetLogin = splits[1],
                                reason = if (splits.size >= 3) splits[2] else null
                            )?.let { onMessage(LiveChatMessage(message = it, color = "#999999", isAction = true)) }
                        }
                    }
                }
                command.equals("/unban", true) -> {
                    val splits = message.split(" ")
                    if (splits.size >= 2) {
                        viewModelScope.launch {
                            repository.unbanUser(
                                helixClientId = helixClientId,
                                helixToken = account.helixToken,
                                userId = account.id,
                                gqlClientId = gqlClientId2,
                                gqlToken = account.gqlToken2,
                                channelId = channelId,
                                targetLogin = splits[1]
                            )?.let { onMessage(LiveChatMessage(message = it, color = "#999999", isAction = true)) }
                        }
                    }
                }
                command.equals("/clear", true) -> {
                    if (!account.helixToken.isNullOrBlank()) {
                        viewModelScope.launch {
                            repository.deleteMessages(
                                helixClientId = helixClientId,
                                helixToken = account.helixToken,
                                channelId = channelId,
                                userId = account.id
                            )?.let { onMessage(LiveChatMessage(message = it, color = "#999999", isAction = true)) }
                        }
                    } else sendMessage(message)
                }
                command.equals("/color", true) -> {
                    val splits = message.split(" ")
                    viewModelScope.launch {
                        if (splits.size >= 2) {
                            repository.updateChatColor(
                                helixClientId = helixClientId,
                                helixToken = account.helixToken,
                                userId = account.id,
                                gqlClientId = gqlClientId2,
                                gqlToken = account.gqlToken2,
                                color = splits[1]
                            )
                        } else {
                            repository.getChatColor(
                                helixClientId = helixClientId,
                                helixToken = account.helixToken,
                                userId = account.id
                            )
                        }?.let { onMessage(LiveChatMessage(message = it, color = "#999999", isAction = true)) }
                    }
                }
                command.equals("/commercial", true) -> {
                    if (!account.helixToken.isNullOrBlank()) {
                        val splits = message.split(" ")
                        if (splits.size >= 2) {
                            viewModelScope.launch {
                                repository.startCommercial(
                                    helixClientId = helixClientId,
                                    helixToken = account.helixToken,
                                    channelId = channelId,
                                    length = splits[1]
                                )?.let { onMessage(LiveChatMessage(message = it, color = "#999999", isAction = true)) }
                            }
                        }
                    } else sendMessage(message)
                }
                command.equals("/delete", true) -> {
                    if (!account.helixToken.isNullOrBlank()) {
                        val splits = message.split(" ")
                        if (splits.size >= 2) {
                            viewModelScope.launch {
                                repository.deleteMessages(
                                    helixClientId = helixClientId,
                                    helixToken = account.helixToken,
                                    channelId = channelId,
                                    userId = account.id,
                                    messageId = splits[1]
                                )?.let { onMessage(LiveChatMessage(message = it, color = "#999999", isAction = true)) }
                            }
                        }
                    } else sendMessage(message)
                }
                command.equals("/disconnect", true) -> disconnect()
                command.equals("/emoteonly", true) -> {
                    if (!account.helixToken.isNullOrBlank()) {
                        viewModelScope.launch {
                            repository.updateChatSettings(
                                helixClientId = helixClientId,
                                helixToken = account.helixToken,
                                channelId = channelId,
                                userId = account.id,
                                emote = true
                            )?.let { onMessage(LiveChatMessage(message = it, color = "#999999", isAction = true)) }
                        }
                    } else sendMessage(message)
                }
                command.equals("/emoteonlyoff", true) -> {
                    if (!account.helixToken.isNullOrBlank()) {
                        viewModelScope.launch {
                            repository.updateChatSettings(
                                helixClientId = helixClientId,
                                helixToken = account.helixToken,
                                channelId = channelId,
                                userId = account.id,
                                emote = false
                            )?.let { onMessage(LiveChatMessage(message = it, color = "#999999", isAction = true)) }
                        }
                    } else sendMessage(message)
                }
                command.equals("/followers", true) -> {
                    if (!account.helixToken.isNullOrBlank()) {
                        val splits = message.split(" ")
                        viewModelScope.launch {
                            repository.updateChatSettings(
                                helixClientId = helixClientId,
                                helixToken = account.helixToken,
                                channelId = channelId,
                                userId = account.id,
                                followers = true,
                                followersDuration = if (splits.size >= 2) splits[1] else null
                            )?.let { onMessage(LiveChatMessage(message = it, color = "#999999", isAction = true)) }
                        }
                    } else sendMessage(message)
                }
                command.equals("/followersoff", true) -> {
                    if (!account.helixToken.isNullOrBlank()) {
                        viewModelScope.launch {
                            repository.updateChatSettings(
                                helixClientId = helixClientId,
                                helixToken = account.helixToken,
                                channelId = channelId,
                                userId = account.id,
                                followers = false
                            )?.let { onMessage(LiveChatMessage(message = it, color = "#999999", isAction = true)) }
                        }
                    } else sendMessage(message)
                }
                command.equals("/marker", true) -> {
                    val splits = message.split(" ", limit = 2)
                    viewModelScope.launch {
                        repository.createStreamMarker(
                            helixClientId = helixClientId,
                            helixToken = account.helixToken,
                            channelId = channelId,
                            gqlClientId = gqlClientId2,
                            gqlToken = account.gqlToken2,
                            channelLogin = channelLogin,
                            description = if (splits.size >= 2) splits[1] else null
                        )?.let { onMessage(LiveChatMessage(message = it, color = "#999999", isAction = true)) }
                    }
                }
                command.equals("/mod", true) -> {
                    val splits = message.split(" ")
                    if (splits.size >= 2) {
                        viewModelScope.launch {
                            repository.addModerator(
                                helixClientId = helixClientId,
                                helixToken = account.helixToken,
                                gqlClientId = gqlClientId2,
                                gqlToken = account.gqlToken2,
                                channelId = channelId,
                                targetLogin = splits[1]
                            )?.let { onMessage(LiveChatMessage(message = it, color = "#999999", isAction = true)) }
                        }
                    }
                }
                command.equals("/unmod", true) -> {
                    val splits = message.split(" ")
                    if (splits.size >= 2) {
                        viewModelScope.launch {
                            repository.removeModerator(
                                helixClientId = helixClientId,
                                helixToken = account.helixToken,
                                gqlClientId = gqlClientId2,
                                gqlToken = account.gqlToken2,
                                channelId = channelId,
                                targetLogin = splits[1]
                            )?.let { onMessage(LiveChatMessage(message = it, color = "#999999", isAction = true)) }
                        }
                    }
                }
                command.equals("/mods", true) -> {
                    viewModelScope.launch {
                        repository.getModerators(
                            helixClientId = helixClientId,
                            helixToken = account.helixToken,
                            channelId = channelId,
                            gqlClientId = gqlClientId,
                            channelLogin = channelLogin,
                        )?.let { onMessage(LiveChatMessage(message = it, color = "#999999", isAction = true)) }
                    }
                }
                command.equals("/raid", true) -> {
                    val splits = message.split(" ")
                    if (splits.size >= 2) {
                        viewModelScope.launch {
                            repository.startRaid(
                                helixClientId = helixClientId,
                                helixToken = account.helixToken,
                                gqlClientId = gqlClientId2,
                                gqlToken = account.gqlToken2,
                                channelId = channelId,
                                targetLogin = splits[1]
                            )?.let { onMessage(LiveChatMessage(message = it, color = "#999999", isAction = true)) }
                        }
                    }
                }
                command.equals("/unraid", true) -> {
                    viewModelScope.launch {
                        repository.cancelRaid(
                            helixClientId = helixClientId,
                            helixToken = account.helixToken,
                            gqlClientId = gqlClientId2,
                            gqlToken = account.gqlToken2,
                            channelId = channelId
                        )?.let { onMessage(LiveChatMessage(message = it, color = "#999999", isAction = true)) }
                    }
                }
                command.equals("/slow", true) -> {
                    if (!account.helixToken.isNullOrBlank()) {
                        val splits = message.split(" ")
                        viewModelScope.launch {
                            repository.updateChatSettings(
                                helixClientId = helixClientId,
                                helixToken = account.helixToken,
                                channelId = channelId,
                                userId = account.id,
                                slow = true,
                                slowDuration = if (splits.size >= 2) splits[1].toIntOrNull() else null
                            )?.let { onMessage(LiveChatMessage(message = it, color = "#999999", isAction = true)) }
                        }
                    } else sendMessage(message)
                }
                command.equals("/slowoff", true) -> {
                    if (!account.helixToken.isNullOrBlank()) {
                        viewModelScope.launch {
                            repository.updateChatSettings(
                                helixClientId = helixClientId,
                                helixToken = account.helixToken,
                                channelId = channelId,
                                userId = account.id,
                                slow = false
                            )?.let { onMessage(LiveChatMessage(message = it, color = "#999999", isAction = true)) }
                        }
                    } else sendMessage(message)
                }
                command.equals("/subscribers", true) -> {
                    if (!account.helixToken.isNullOrBlank()) {
                        viewModelScope.launch {
                            repository.updateChatSettings(
                                helixClientId = helixClientId,
                                helixToken = account.helixToken,
                                channelId = channelId,
                                userId = account.id,
                                subs = true,
                            )?.let { onMessage(LiveChatMessage(message = it, color = "#999999", isAction = true)) }
                        }
                    } else sendMessage(message)
                }
                command.equals("/subscribersoff", true) -> {
                    if (!account.helixToken.isNullOrBlank()) {
                        viewModelScope.launch {
                            repository.updateChatSettings(
                                helixClientId = helixClientId,
                                helixToken = account.helixToken,
                                channelId = channelId,
                                userId = account.id,
                                subs = false,
                            )?.let { onMessage(LiveChatMessage(message = it, color = "#999999", isAction = true)) }
                        }
                    } else sendMessage(message)
                }
                command.equals("/timeout", true) -> {
                    val splits = message.split(" ", limit = 4)
                    if (splits.size >= 2) {
                        viewModelScope.launch {
                            repository.banUser(
                                helixClientId = helixClientId,
                                helixToken = account.helixToken,
                                userId = account.id,
                                gqlClientId = gqlClientId2,
                                gqlToken = account.gqlToken2,
                                channelId = channelId,
                                targetLogin = splits[1],
                                duration = if (splits.size >= 3) splits[2] else null ?: if (!account.gqlToken2.isNullOrBlank()) "10m" else "600",
                                reason = if (splits.size >= 4) splits[3] else null,
                            )?.let { onMessage(LiveChatMessage(message = it, color = "#999999", isAction = true)) }
                        }
                    }
                }
                command.equals("/untimeout", true) -> {
                    val splits = message.split(" ")
                    if (splits.size >= 2) {
                        viewModelScope.launch {
                            repository.unbanUser(
                                helixClientId = helixClientId,
                                helixToken = account.helixToken,
                                userId = account.id,
                                gqlClientId = gqlClientId2,
                                gqlToken = account.gqlToken2,
                                channelId = channelId,
                                targetLogin = splits[1]
                            )?.let { onMessage(LiveChatMessage(message = it, color = "#999999", isAction = true)) }
                        }
                    }
                }
                command.equals("/uniquechat", true) -> {
                    if (!account.helixToken.isNullOrBlank()) {
                        viewModelScope.launch {
                            repository.updateChatSettings(
                                helixClientId = helixClientId,
                                helixToken = account.helixToken,
                                channelId = channelId,
                                userId = account.id,
                                unique = true,
                            )?.let { onMessage(LiveChatMessage(message = it, color = "#999999", isAction = true)) }
                        }
                    } else sendMessage(message)
                }
                command.equals("/uniquechatoff", true) -> {
                    if (!account.helixToken.isNullOrBlank()) {
                        viewModelScope.launch {
                            repository.updateChatSettings(
                                helixClientId = helixClientId,
                                helixToken = account.helixToken,
                                channelId = channelId,
                                userId = account.id,
                                unique = false,
                            )?.let { onMessage(LiveChatMessage(message = it, color = "#999999", isAction = true)) }
                        }
                    } else sendMessage(message)
                }
                command.equals("/vip", true) -> {
                    val splits = message.split(" ")
                    if (splits.size >= 2) {
                        viewModelScope.launch {
                            repository.addVip(
                                helixClientId = helixClientId,
                                helixToken = account.helixToken,
                                gqlClientId = gqlClientId2,
                                gqlToken = account.gqlToken2,
                                channelId = channelId,
                                targetLogin = splits[1]
                            )?.let { onMessage(LiveChatMessage(message = it, color = "#999999", isAction = true)) }
                        }
                    }
                }
                command.equals("/unvip", true) -> {
                    val splits = message.split(" ")
                    if (splits.size >= 2) {
                        viewModelScope.launch {
                            repository.removeVip(
                                helixClientId = helixClientId,
                                helixToken = account.helixToken,
                                gqlClientId = gqlClientId2,
                                gqlToken = account.gqlToken2,
                                channelId = channelId,
                                targetLogin = splits[1]
                            )?.let { onMessage(LiveChatMessage(message = it, color = "#999999", isAction = true)) }
                        }
                    }
                }
                command.equals("/vips", true) -> {
                    viewModelScope.launch {
                        repository.getVips(
                            helixClientId = helixClientId,
                            helixToken = account.helixToken,
                            channelId = channelId,
                            gqlClientId = gqlClientId,
                            channelLogin = channelLogin,
                        )?.let { onMessage(LiveChatMessage(message = it, color = "#999999", isAction = true)) }
                    }
                }
                command.equals("/w", true) -> {
                    val splits = message.split(" ", limit = 3)
                    if (splits.size >= 3) {
                        viewModelScope.launch {
                            repository.sendWhisper(
                                helixClientId = helixClientId,
                                helixToken = account.helixToken,
                                userId = account.id,
                                targetLogin = splits[1],
                                message = splits[2]
                            )?.let { onMessage(LiveChatMessage(message = it, color = "#999999", isAction = true)) }
                        }
                    }
                }
                else -> sendMessage(message)
            }
        }
    }

    private inner class VideoChatController(
            private val clientId: String?,
            private val videoId: String,
            private val startTime: Double,
            private val getCurrentPosition: () -> Double) : ChatController() {

        private var chatReplayManager: ChatReplayManager? = null

        override fun send(message: CharSequence) {

        }

        override fun start() {
            stop()
            chatReplayManager = ChatReplayManager(clientId, repository, videoId, startTime, getCurrentPosition, this, { _chatMessages.postValue(ArrayList()) }, viewModelScope)
        }

        override fun pause() {
            chatReplayManager?.stop()
        }

        override fun stop() {
            chatReplayManager?.stop()
        }
    }

    abstract inner class ChatController : OnChatMessageReceivedListener {
        abstract fun send(message: CharSequence)
        abstract fun start()
        abstract fun pause()
        abstract fun stop()

        override fun onMessage(message: ChatMessage) {
            _chatMessages.value!!.add(message)
            _newMessage.postValue(message)
        }
    }

    companion object {
        private const val TAG = "ChatViewModel"

        private var savedEmoteSets: List<String>? = null
        private var savedUserEmotes: List<TwitchEmote>? = null
        private var savedGlobalBadges: List<TwitchBadge>? = null
        private var savedGlobalStvEmotes: List<StvEmote>? = null
        private var savedGlobalBttvEmotes: List<BttvEmote>? = null
        private var savedGlobalFfzEmotes: List<FfzEmote>? = null
    }
}