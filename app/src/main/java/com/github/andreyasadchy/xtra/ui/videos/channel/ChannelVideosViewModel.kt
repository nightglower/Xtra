package com.github.andreyasadchy.xtra.ui.videos.channel

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
import com.github.andreyasadchy.xtra.model.offline.SortChannel
import com.github.andreyasadchy.xtra.repository.*
import com.github.andreyasadchy.xtra.repository.datasource.ChannelVideosDataSource
import com.github.andreyasadchy.xtra.type.VideoSort
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
class ChannelVideosViewModel @Inject constructor(
    @ApplicationContext context: Context,
    repository: ApiRepository,
    playerRepository: PlayerRepository,
    bookmarksRepository: BookmarksRepository,
    private val sortChannelRepository: SortChannelRepository,
    private val graphQLRepository: GraphQLRepository,
    private val helix: HelixApi,
    private val apolloClient: ApolloClient,
    savedStateHandle: SavedStateHandle) : BaseVideosViewModel(playerRepository, bookmarksRepository, repository) {

    private val _sortText = MutableLiveData<CharSequence>()
    val sortText: LiveData<CharSequence>
        get() = _sortText
    private val args = ChannelVideosFragmentArgs.fromSavedStateHandle(savedStateHandle)
    val filter = MutableStateFlow(Filter(
        saveSort = true,
        sort = Sort.TIME,
        period = Period.ALL,
        broadcastType = BroadcastType.ALL
    ))

    @OptIn(ExperimentalCoroutinesApi::class)
    val flow = filter.flatMapLatest { filter ->
        Pager(
            PagingConfig(pageSize = 10, prefetchDistance = 3, initialLoadSize = 15)
        ) {
            with(filter) {
                ChannelVideosDataSource(
                    channelId = args.channelId,
                    channelLogin = args.channelLogin,
                    helixClientId = context.prefs().getString(C.HELIX_CLIENT_ID, ""),
                    helixToken = User.get(context).helixToken,
                    helixPeriod = period,
                    helixBroadcastTypes = broadcastType,
                    helixSort = sort,
                    helixApi = helix,
                    gqlClientId = context.prefs().getString(C.GQL_CLIENT_ID, ""),
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
                    apiPref = TwitchApiHelper.listFromPrefs(context.prefs().getString(C.API_PREF_CHANNEL_VIDEOS, ""), TwitchApiHelper.channelVideosApiDefaults))
            }
        }.flow.cachedIn(viewModelScope)
    }

    fun setChannelId(context: Context) {
        var sortValues = args.channelId?.let { runBlocking { sortChannelRepository.getById(it) } }
        if (sortValues?.saveSort != true) {
            sortValues = runBlocking { sortChannelRepository.getById("default") }
        }
        filter.value = Filter(
            saveSort = sortValues?.saveSort ?: true,
            sort = when (sortValues?.videoSort) {
                Sort.VIEWS.value -> Sort.VIEWS
                else -> Sort.TIME
            },
            period = Period.ALL,
            broadcastType = when (sortValues?.videoType) {
                BroadcastType.ARCHIVE.value -> BroadcastType.ARCHIVE
                BroadcastType.HIGHLIGHT.value -> BroadcastType.HIGHLIGHT
                BroadcastType.UPLOAD.value -> BroadcastType.UPLOAD
                else -> BroadcastType.ALL
            }
        )
        _sortText.value = context.getString(R.string.sort_and_period,
            when (sortValues?.videoSort) {
                Sort.VIEWS.value -> context.getString(R.string.view_count)
                else -> context.getString(R.string.upload_date)
            }, context.getString(R.string.all_time)
        )
    }

    fun filter(context: Context, sort: Sort, type: BroadcastType, text: CharSequence, saveSort: Boolean, saveDefault: Boolean) {
        filter.value = Filter(saveSort = saveSort, sort = sort, period = Period.ALL, broadcastType = type)
        _sortText.value = text
        viewModelScope.launch {
            val sortValues = args.channelId?.let { sortChannelRepository.getById(it) }
            if (saveSort) {
                (sortValues?.apply {
                    this.saveSort = saveSort
                    videoSort = sort.value
                    videoType = type.value
                } ?: args.channelId?.let { SortChannel(
                    id = it,
                    saveSort = saveSort,
                    videoSort = sort.value,
                    videoType = type.value)
                })?.let { sortChannelRepository.save(it) }
            }
            if (saveDefault) {
                (sortValues?.apply {
                    this.saveSort = saveSort
                } ?: args.channelId?.let { SortChannel(
                    id = it,
                    saveSort = saveSort)
                })?.let { sortChannelRepository.save(it) }
                val sortDefaults = sortChannelRepository.getById("default")
                (sortDefaults?.apply {
                    videoSort = sort.value
                    videoType = type.value
                } ?: SortChannel(
                    id = "default",
                    videoSort = sort.value,
                    videoType = type.value
                )).let { sortChannelRepository.save(it) }
            }
        }
        if (saveDefault != context.prefs().getBoolean(C.SORT_DEFAULT_CHANNEL_VIDEOS, false)) {
            context.prefs().edit { putBoolean(C.SORT_DEFAULT_CHANNEL_VIDEOS, saveDefault) }
        }
    }

    data class Filter(
        val saveSort: Boolean,
        val sort: Sort,
        val period: Period,
        val broadcastType: BroadcastType
    )
}
