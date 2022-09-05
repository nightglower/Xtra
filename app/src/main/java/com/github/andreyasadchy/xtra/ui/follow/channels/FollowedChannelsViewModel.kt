package com.github.andreyasadchy.xtra.ui.follow.channels

import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.apollographql.apollo3.ApolloClient
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.api.HelixApi
import com.github.andreyasadchy.xtra.model.User
import com.github.andreyasadchy.xtra.model.helix.follows.Order
import com.github.andreyasadchy.xtra.model.helix.follows.Sort
import com.github.andreyasadchy.xtra.model.offline.SortChannel
import com.github.andreyasadchy.xtra.repository.*
import com.github.andreyasadchy.xtra.repository.datasource.FollowedChannelsDataSource
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
class FollowedChannelsViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val localFollowsChannel: LocalFollowChannelRepository,
    private val offlineRepository: OfflineRepository,
    private val bookmarksRepository: BookmarksRepository,
    private val sortChannelRepository: SortChannelRepository,
    private val graphQLRepository: GraphQLRepository,
    private val helix: HelixApi,
    private val apolloClient: ApolloClient) : ViewModel() {

    private val _sortText = MutableLiveData<CharSequence>()
    val sortText: LiveData<CharSequence>
        get() = _sortText
    val filter = MutableStateFlow(Filter(sort = Sort.LAST_BROADCAST, order = Order.DESC))

    @OptIn(ExperimentalCoroutinesApi::class)
    val flow = filter.flatMapLatest { filter ->
        Pager(
            PagingConfig(pageSize = 40, prefetchDistance = 10, initialLoadSize = 40)
        ) {
            FollowedChannelsDataSource(
                localFollowsChannel = localFollowsChannel,
                offlineRepository = offlineRepository,
                bookmarksRepository = bookmarksRepository,
                userId = User.get(context).id,
                helixClientId = context.prefs().getString(C.HELIX_CLIENT_ID, ""),
                helixToken = User.get(context).helixToken,
                helixApi = helix,
                gqlClientId = context.prefs().getString(C.GQL_CLIENT_ID, ""),
                gqlToken = User.get(context).gqlToken,
                gqlApi = graphQLRepository,
                apolloClient = apolloClient,
                apiPref = TwitchApiHelper.listFromPrefs(context.prefs().getString(C.API_PREF_FOLLOWED_CHANNELS, ""), TwitchApiHelper.followedChannelsApiDefaults),
                sort = filter.sort,
                order = filter.order
            )
        }.flow.cachedIn(viewModelScope)
    }

    fun setUser(context: Context) {
        val sortValues = runBlocking { sortChannelRepository.getById("followed_channels") }
        filter.value = Filter(
            sort = when (sortValues?.videoSort) {
                Sort.FOLLOWED_AT.value -> Sort.FOLLOWED_AT
                Sort.ALPHABETICALLY.value -> Sort.ALPHABETICALLY
                else -> Sort.LAST_BROADCAST
            },
            order = when (sortValues?.videoType) {
                Order.ASC.value -> Order.ASC
                else -> Order.DESC
            }
        )
        _sortText.value = context.getString(R.string.sort_and_order,
            when (sortValues?.videoSort) {
                Sort.FOLLOWED_AT.value -> context.getString(R.string.time_followed)
                Sort.ALPHABETICALLY.value -> context.getString(R.string.alphabetically)
                else -> context.getString(R.string.last_broadcast)
            },
            when (sortValues?.videoType) {
                Order.ASC.value -> context.getString(R.string.ascending)
                else -> context.getString(R.string.descending)
            }
        )
    }

    fun filter(context: Context, sort: Sort, order: Order, text: CharSequence, saveDefault: Boolean) {
        filter.value = Filter(sort = sort, order = order)
        _sortText.value = text
        if (saveDefault) {
            viewModelScope.launch {
                val sortDefaults = sortChannelRepository.getById("followed_channels")
                (sortDefaults?.apply {
                    videoSort = sort.value
                    videoType = order.value
                } ?: SortChannel(
                    id = "followed_channels",
                    videoSort = sort.value,
                    videoType = order.value
                )).let { sortChannelRepository.save(it) }
            }
        }
        if (saveDefault != context.prefs().getBoolean(C.SORT_DEFAULT_FOLLOWED_CHANNELS, false)) {
            context.prefs().edit { putBoolean(C.SORT_DEFAULT_FOLLOWED_CHANNELS, saveDefault) }
        }
    }

    data class Filter(
        val sort: Sort,
        val order: Order
    )
}
