package com.github.andreyasadchy.xtra.ui.videos.followed

import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.apollographql.apollo3.ApolloClient
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.model.User
import com.github.andreyasadchy.xtra.model.helix.video.BroadcastType
import com.github.andreyasadchy.xtra.model.helix.video.Period
import com.github.andreyasadchy.xtra.model.helix.video.Sort
import com.github.andreyasadchy.xtra.model.offline.SortChannel
import com.github.andreyasadchy.xtra.repository.*
import com.github.andreyasadchy.xtra.repository.datasource.FollowedVideosDataSource
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
class FollowedVideosViewModel @Inject constructor(
    @ApplicationContext context: Context,
    repository: ApiRepository,
    playerRepository: PlayerRepository,
    bookmarksRepository: BookmarksRepository,
    private val sortChannelRepository: SortChannelRepository,
    private val graphQLRepository: GraphQLRepository,
    private val apolloClient: ApolloClient) : BaseVideosViewModel(playerRepository, bookmarksRepository, repository) {

    private val _sortText = MutableLiveData<CharSequence>()
    val sortText: LiveData<CharSequence>
        get() = _sortText
    val filter = MutableStateFlow(Filter(
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
                FollowedVideosDataSource(
                    userId = User.get(context).id,
                    gqlClientId = context.prefs().getString(C.GQL_CLIENT_ID, ""),
                    gqlToken = User.get(context).gqlToken,
                    gqlQueryType = when (broadcastType) {
                        BroadcastType.ARCHIVE -> com.github.andreyasadchy.xtra.type.BroadcastType.ARCHIVE
                        BroadcastType.HIGHLIGHT -> com.github.andreyasadchy.xtra.type.BroadcastType.HIGHLIGHT
                        BroadcastType.UPLOAD -> com.github.andreyasadchy.xtra.type.BroadcastType.UPLOAD
                        else -> null },
                    gqlQuerySort = when (sort) { Sort.TIME -> VideoSort.TIME else -> VideoSort.VIEWS },
                    gqlApi = graphQLRepository,
                    apolloClient = apolloClient,
                    apiPref = TwitchApiHelper.listFromPrefs(context.prefs().getString(C.API_PREF_FOLLOWED_VIDEOS, ""), TwitchApiHelper.followedVideosApiDefaults))
            }
        }.flow.cachedIn(viewModelScope)
    }

    fun setUser(context: Context) {
        val sortValues = runBlocking { sortChannelRepository.getById("followed_videos") }
        filter.value = Filter(
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

    fun filter(context: Context, sort: Sort, period: Period, type: BroadcastType, text: CharSequence, saveDefault: Boolean) {
        filter.value = Filter(sort = sort, period = period, broadcastType = type)
        _sortText.value = text
        if (saveDefault) {
            viewModelScope.launch {
                val sortDefaults = sortChannelRepository.getById("followed_videos")
                (sortDefaults?.apply {
                    videoSort = sort.value
                    videoType = type.value
                } ?: SortChannel(
                    id = "followed_videos",
                    videoSort = sort.value,
                    videoType = type.value
                )).let { sortChannelRepository.save(it) }
            }
        }
        if (saveDefault != context.prefs().getBoolean(C.SORT_DEFAULT_FOLLOWED_VIDEOS, false)) {
            context.prefs().edit { putBoolean(C.SORT_DEFAULT_FOLLOWED_VIDEOS, saveDefault) }
        }
    }

    data class Filter(
        val sort: Sort,
        val period: Period,
        val broadcastType: BroadcastType
    )
}
