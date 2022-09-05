package com.github.andreyasadchy.xtra.ui.channel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.github.andreyasadchy.xtra.model.User
import com.github.andreyasadchy.xtra.model.helix.stream.Stream
import com.github.andreyasadchy.xtra.repository.ApiRepository
import com.github.andreyasadchy.xtra.repository.BookmarksRepository
import com.github.andreyasadchy.xtra.repository.LocalFollowChannelRepository
import com.github.andreyasadchy.xtra.repository.OfflineRepository
import com.github.andreyasadchy.xtra.ui.common.follow.FollowLiveData
import com.github.andreyasadchy.xtra.ui.common.follow.FollowViewModel
import com.github.andreyasadchy.xtra.util.C
import com.github.andreyasadchy.xtra.util.DownloadUtils
import com.github.andreyasadchy.xtra.util.prefs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ChannelPagerViewModel @Inject constructor(
    private val repository: ApiRepository,
    private val localFollowsChannel: LocalFollowChannelRepository,
    private val offlineRepository: OfflineRepository,
    private val bookmarksRepository: BookmarksRepository,
    savedStateHandle: SavedStateHandle) : ViewModel(), FollowViewModel {

    private val args = ChannelPagerFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _stream = MutableLiveData<Stream?>()
    val stream: MutableLiveData<Stream?>
        get() = _stream
    private val _user = MutableLiveData<com.github.andreyasadchy.xtra.model.helix.user.User?>()
    val user: MutableLiveData<com.github.andreyasadchy.xtra.model.helix.user.User?>
        get() = _user

    override val userId: String?
        get() { return args.channelId }
    override val userLogin: String?
        get() { return args.channelLogin }
    override val userName: String?
        get() { return args.channelName }
    override val channelLogo: String?
        get() { return args.channelLogo }
    override lateinit var follow: FollowLiveData

    override fun setUser(user: User, helixClientId: String?, gqlClientId: String?, setting: Int) {
        if (!this::follow.isInitialized) {
            follow = FollowLiveData(localFollowsChannel = localFollowsChannel, repository = repository, userId = userId, userLogin = userLogin, userName = userName, channelLogo = channelLogo, user = user, helixClientId = helixClientId, gqlClientId = gqlClientId, setting = setting, viewModelScope = viewModelScope)
        }
    }

    fun loadStream(context: Context) {
        viewModelScope.launch {
            try {
                repository.loadUserChannelPage(
                    channelId = args.channelId,
                    channelLogin = args.channelLogin,
                    helixClientId = context.prefs().getString(C.HELIX_CLIENT_ID, ""),
                    helixToken = User.get(context).helixToken,
                    gqlClientId = context.prefs().getString(C.GQL_CLIENT_ID, "")
                )?.let { _stream.postValue(it) }
            } catch (e: Exception) {}
        }
    }

    fun loadUser(context: Context) {
        val helixToken = User.get(context).helixToken
        if (!helixToken.isNullOrBlank()) {
            viewModelScope.launch {
                try {
                    repository.loadUser(
                        channelId = args.channelId,
                        channelLogin = args.channelLogin,
                        helixClientId = context.prefs().getString(C.HELIX_CLIENT_ID, ""),
                        helixToken = helixToken
                    )?.let { _user.postValue(it) }
                } catch (e: Exception) {}
            }
        }
    }

    fun retry(context: Context) {
        if (_stream.value == null) {
            loadStream(context)
        } else {
            if (_stream.value?.channelUser == null && _user.value == null) {
                loadUser(context)
            }
        }
    }

    fun updateLocalUser(context: Context, user: com.github.andreyasadchy.xtra.model.helix.user.User) {
        GlobalScope.launch {
            try {
                if (user.id != null) {
                    try {
                        Glide.with(context)
                            .asBitmap()
                            .load(user.channelLogo)
                            .into(object: CustomTarget<Bitmap>() {
                                override fun onLoadCleared(placeholder: Drawable?) {

                                }

                                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                    DownloadUtils.savePng(context, "profile_pics", user.id, resource)
                                }
                            })
                    } catch (e: Exception) {

                    }
                    val downloadedLogo = File(context.filesDir.toString() + File.separator + "profile_pics" + File.separator + "${user.id}.png").absolutePath
                    localFollowsChannel.getFollowById(user.id)?.let { localFollowsChannel.updateFollow(it.apply {
                        user_login = user.login
                        user_name = user.display_name
                        channelLogo = downloadedLogo }) }
                    for (i in offlineRepository.getVideosByUserId(user.id.toInt())) {
                        offlineRepository.updateVideo(i.apply {
                            channelLogin = user.login
                            channelName = user.display_name
                            channelLogo = downloadedLogo })
                    }
                    for (i in bookmarksRepository.getBookmarksByUserId(user.id)) {
                        bookmarksRepository.updateBookmark(i.apply {
                            userLogin = user.login
                            userName = user.display_name
                            userLogo = downloadedLogo })
                    }
                }
            } catch (e: Exception) {

            }
        }
    }
}
