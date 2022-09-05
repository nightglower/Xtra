package com.github.andreyasadchy.xtra.ui.videos.game

import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.apollographql.apollo3.ApolloClient
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.api.HelixApi
import com.github.andreyasadchy.xtra.model.User
import com.github.andreyasadchy.xtra.model.helix.video.BroadcastType
import com.github.andreyasadchy.xtra.model.helix.video.Period
import com.github.andreyasadchy.xtra.model.helix.video.Sort
import com.github.andreyasadchy.xtra.model.offline.SortGame
import com.github.andreyasadchy.xtra.repository.*
import com.github.andreyasadchy.xtra.repository.datasource.GameVideosDataSource
import com.github.andreyasadchy.xtra.type.VideoSort
import com.github.andreyasadchy.xtra.ui.common.follow.FollowLiveData
import com.github.andreyasadchy.xtra.ui.common.follow.FollowViewModel
import com.github.andreyasadchy.xtra.ui.videos.BaseVideosViewModel
import com.github.andreyasadchy.xtra.util.C
import com.github.andreyasadchy.xtra.util.TwitchApiHelper
import com.github.andreyasadchy.xtra.util.prefs
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class GameVideosViewModel @Inject constructor(
    @ApplicationContext context: Context,
    playerRepository: PlayerRepository,
    bookmarksRepository: BookmarksRepository,
    private val repository: ApiRepository,
    private val localFollowsGame: LocalFollowGameRepository,
    private val sortGameRepository: SortGameRepository,
    private val graphQLRepository: GraphQLRepository,
    private val helix: HelixApi,
    private val apolloClient: ApolloClient,
    savedStateHandle: SavedStateHandle) : BaseVideosViewModel(playerRepository, bookmarksRepository, repository), FollowViewModel {

    private val _sortText = MutableLiveData<CharSequence>()
    val sortText: LiveData<CharSequence>
        get() = _sortText
    private val args = GameVideosFragmentArgs.fromSavedStateHandle(savedStateHandle)
    val filter = MutableStateFlow(Filter(
        saveSort = true,
        sort = Sort.VIEWS,
        period = Period.WEEK,
        broadcastType = BroadcastType.ALL,
        languageIndex = 0
    ))

    @OptIn(ExperimentalCoroutinesApi::class)
    val flow = filter.flatMapLatest { filter ->
        Pager(
            PagingConfig(pageSize = 10, prefetchDistance = 3, initialLoadSize = 15)
        ) {
            with(filter) {
                val langValues = context.resources.getStringArray(R.array.gqlUserLanguageValues).toList()
                val language = if (languageIndex != 0) {
                    langValues.elementAt(languageIndex)
                } else null
                GameVideosDataSource(
                    gameId = args.gameId,
                    gameName = args.gameName,
                    helixClientId = context.prefs().getString(C.HELIX_CLIENT_ID, ""),
                    helixToken = User.get(context).helixToken,
                    helixPeriod = period,
                    helixBroadcastTypes = broadcastType,
                    helixLanguage = language?.lowercase(),
                    helixSort = sort,
                    helixApi = helix,
                    gqlClientId = context.prefs().getString(C.GQL_CLIENT_ID, ""),
                    gqlQueryLanguages = if (language != null) {
                        listOf(language)
                    } else null,
                    gqlQueryType = when (broadcastType) {
                        BroadcastType.ARCHIVE -> com.github.andreyasadchy.xtra.type.BroadcastType.ARCHIVE
                        BroadcastType.HIGHLIGHT -> com.github.andreyasadchy.xtra.type.BroadcastType.HIGHLIGHT
                        BroadcastType.UPLOAD -> com.github.andreyasadchy.xtra.type.BroadcastType.UPLOAD
                        else -> null },
                    gqlQuerySort = when (sort) { Sort.TIME -> VideoSort.TIME else -> VideoSort.VIEWS },
                    gqlType = if (broadcastType == BroadcastType.ALL) { null }
                    else { broadcastType.value.uppercase() },
                    gqlSort = sort.value.uppercase(),
                    gqlApi = graphQLRepository,
                    apolloClient = apolloClient,
                    apiPref = TwitchApiHelper.listFromPrefs(context.prefs().getString(C.API_PREF_GAME_VIDEOS, ""), TwitchApiHelper.gameVideosApiDefaults))
            }
        }.flow.cachedIn(viewModelScope)
    }

    fun setGame(context: Context) {
        var sortValues = args.gameId?.let { runBlocking { sortGameRepository.getById(it) } }
        if (sortValues?.saveSort != true) {
            sortValues = runBlocking { sortGameRepository.getById("default") }
        }
        filter.value = Filter(
            saveSort = sortValues?.saveSort ?: true,
            sort = when (sortValues?.videoSort) {
                Sort.TIME.value -> Sort.TIME
                else -> Sort.VIEWS
            },
            period = if (User.get(context).helixToken.isNullOrBlank()) {
                Period.WEEK
            } else {
                when (sortValues?.videoPeriod) {
                    Period.DAY.value -> Period.DAY
                    Period.MONTH.value -> Period.MONTH
                    Period.ALL.value -> Period.ALL
                    else -> Period.WEEK
                }
            },
            broadcastType = when (sortValues?.videoType) {
                BroadcastType.ARCHIVE.value -> BroadcastType.ARCHIVE
                BroadcastType.HIGHLIGHT.value -> BroadcastType.HIGHLIGHT
                BroadcastType.UPLOAD.value -> BroadcastType.UPLOAD
                else -> BroadcastType.ALL
            },
            languageIndex = sortValues?.videoLanguageIndex ?: 0
        )
        _sortText.value = context.getString(R.string.sort_and_period,
            when (sortValues?.videoSort) {
                Sort.TIME.value -> context.getString(R.string.upload_date)
                else -> context.getString(R.string.view_count)
            },
            when (sortValues?.videoPeriod) {
                Period.DAY.value -> context.getString(R.string.today)
                Period.MONTH.value -> context.getString(R.string.this_month)
                Period.ALL.value -> context.getString(R.string.all_time)
                else -> context.getString(R.string.this_week)
            }
        )
    }

    fun filter(context: Context, sort: Sort, period: Period, type: BroadcastType, languageIndex: Int, text: CharSequence, saveSort: Boolean, saveDefault: Boolean) {
        filter.value = Filter(saveSort = saveSort, sort = sort, period = period, broadcastType = type, languageIndex = languageIndex)
        _sortText.value = text
        viewModelScope.launch {
            val sortValues = args.gameId?.let { sortGameRepository.getById(it) }
            if (saveSort) {
                (sortValues?.apply {
                    this.saveSort = saveSort
                    videoSort = sort.value
                    if (!User.get(context).helixToken.isNullOrBlank()) videoPeriod = period.value
                    videoType = type.value
                    videoLanguageIndex = languageIndex
                } ?: args.gameId?.let { SortGame(
                    id = it,
                    saveSort = saveSort,
                    videoSort = sort.value,
                    videoPeriod = if (User.get(context).helixToken.isNullOrBlank()) null else period.value,
                    videoType = type.value,
                    videoLanguageIndex = languageIndex)
                })?.let { sortGameRepository.save(it) }
            }
            if (saveDefault) {
                (sortValues?.apply {
                    this.saveSort = saveSort
                } ?: args.gameId?.let { SortGame(
                    id = it,
                    saveSort = saveSort)
                })?.let { sortGameRepository.save(it) }
                val sortDefaults = sortGameRepository.getById("default")
                (sortDefaults?.apply {
                    videoSort = sort.value
                    if (!User.get(context).helixToken.isNullOrBlank()) videoPeriod = period.value
                    videoType = type.value
                    videoLanguageIndex = languageIndex
                } ?: SortGame(
                    id = "default",
                    videoSort = sort.value,
                    videoPeriod = if (User.get(context).helixToken.isNullOrBlank()) null else period.value,
                    videoType = type.value,
                    videoLanguageIndex = languageIndex
                )).let { sortGameRepository.save(it) }
            }
        }
        if (saveDefault != context.prefs().getBoolean(C.SORT_DEFAULT_GAME_VIDEOS, false)) {
            context.prefs().edit { putBoolean(C.SORT_DEFAULT_GAME_VIDEOS, saveDefault) }
        }
    }

    data class Filter(
        val saveSort: Boolean,
        val sort: Sort,
        val period: Period,
        val broadcastType: BroadcastType,
        val languageIndex: Int
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
}
