package com.github.andreyasadchy.xtra.ui.streams.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.apollographql.apollo3.ApolloClient
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.github.andreyasadchy.xtra.api.HelixApi
import com.github.andreyasadchy.xtra.model.User
import com.github.andreyasadchy.xtra.model.helix.stream.Sort
import com.github.andreyasadchy.xtra.repository.ApiRepository
import com.github.andreyasadchy.xtra.repository.GraphQLRepository
import com.github.andreyasadchy.xtra.repository.LocalFollowGameRepository
import com.github.andreyasadchy.xtra.repository.datasource.GameStreamsDataSource
import com.github.andreyasadchy.xtra.repository.datasource.StreamsDataSource
import com.github.andreyasadchy.xtra.type.StreamSort
import com.github.andreyasadchy.xtra.ui.common.follow.FollowLiveData
import com.github.andreyasadchy.xtra.ui.common.follow.FollowViewModel
import com.github.andreyasadchy.xtra.util.C
import com.github.andreyasadchy.xtra.util.DownloadUtils
import com.github.andreyasadchy.xtra.util.TwitchApiHelper
import com.github.andreyasadchy.xtra.util.prefs
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class StreamsViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val repository: ApiRepository,
    private val localFollowsGame: LocalFollowGameRepository,
    private val graphQLRepository: GraphQLRepository,
    private val helix: HelixApi,
    private val apolloClient: ApolloClient,
    savedStateHandle: SavedStateHandle) : ViewModel(), FollowViewModel {

    val compactAdapter = context.prefs().getBoolean(C.COMPACT_STREAMS, false)

    private val args = StreamsFragmentArgs.fromSavedStateHandle(savedStateHandle)
    val filter = MutableStateFlow(Filter(Sort.VIEWERS_HIGH))

    @OptIn(ExperimentalCoroutinesApi::class)
    val flow = filter.flatMapLatest { filter ->
        Pager(
            if (compactAdapter) {
                PagingConfig(pageSize = 30, prefetchDistance = 10, initialLoadSize = 30)
            } else {
                PagingConfig(pageSize = 10, prefetchDistance = 3, initialLoadSize = 15)
            }
        ) {
            if (args.gameId == null && args.gameName == null) {
                StreamsDataSource(
                    helixClientId = context.prefs().getString(C.HELIX_CLIENT_ID, ""),
                    helixToken = User.get(context).helixToken,
                    helixApi = helix,
                    gqlClientId = context.prefs().getString(C.GQL_CLIENT_ID, ""),
                    tags = args.tags?.toList(),
                    gqlApi = graphQLRepository,
                    apolloClient = apolloClient,
                    apiPref = TwitchApiHelper.listFromPrefs(context.prefs().getString(C.API_PREF_STREAMS, ""), TwitchApiHelper.streamsApiDefaults))
            } else {
                GameStreamsDataSource(
                    gameId = args.gameId,
                    gameName = args.gameName,
                    helixClientId = context.prefs().getString(C.HELIX_CLIENT_ID, ""),
                    helixToken = User.get(context).helixToken,
                    helixApi = helix,
                    gqlClientId = context.prefs().getString(C.GQL_CLIENT_ID, ""),
                    gqlQuerySort = when (filter.sort) {
                        Sort.VIEWERS_HIGH -> StreamSort.VIEWER_COUNT
                        Sort.VIEWERS_LOW -> StreamSort.VIEWER_COUNT_ASC
                        else -> null },
                    gqlSort = filter.sort,
                    tags = args.tags?.toList(),
                    gqlApi = graphQLRepository,
                    apolloClient = apolloClient,
                    apiPref = TwitchApiHelper.listFromPrefs(context.prefs().getString(C.API_PREF_GAME_STREAMS, ""), TwitchApiHelper.gameStreamsApiDefaults))
            }
        }.flow.cachedIn(viewModelScope)
    }

    fun filter(sort: Sort) {
        filter.value = Filter(sort)
    }

    data class Filter(
        val sort: Sort
    )

    override val userId: String?
        get() { return args.gameId }
    override val userLogin: String?
        get() = null
    override val userName: String?
        get() { return args.gameName }
    override val channelLogo: String?
        get() = null
    override val game: Boolean
        get() = true
    override lateinit var follow: FollowLiveData

    override fun setUser(user: User, helixClientId: String?, gqlClientId: String?, setting: Int) {
        if (!this::follow.isInitialized) {
            follow = FollowLiveData(localFollowsGame = localFollowsGame, repository = repository, userId = userId, userLogin = userLogin, userName = userName, channelLogo = channelLogo, user = user, helixClientId = helixClientId, gqlClientId = gqlClientId, setting = setting, viewModelScope = viewModelScope)
        }
    }

    fun updateLocalGame(context: Context) {
        GlobalScope.launch {
            try {
                if (args.gameId != null) {
                    val get = repository.loadGameBoxArt(args.gameId!!, context.prefs().getString(C.HELIX_CLIENT_ID, ""), User.get(context).helixToken, context.prefs().getString(C.GQL_CLIENT_ID, ""))
                    try {
                        Glide.with(context)
                            .asBitmap()
                            .load(TwitchApiHelper.getTemplateUrl(get, "game"))
                            .into(object: CustomTarget<Bitmap>() {
                                override fun onLoadCleared(placeholder: Drawable?) {

                                }

                                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                    DownloadUtils.savePng(context, "box_art", args.gameId!!, resource)
                                }
                            })
                    } catch (e: Exception) {

                    }
                    val downloadedLogo = File(context.filesDir.toString() + File.separator + "box_art" + File.separator + "${args.gameId}.png").absolutePath
                    localFollowsGame.getFollowById(args.gameId!!)?.let { localFollowsGame.updateFollow(it.apply {
                        game_name = args.gameName
                        boxArt = downloadedLogo }) }
                }
            } catch (e: Exception) {

            }
        }
    }
}
