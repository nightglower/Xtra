package com.github.andreyasadchy.xtra.ui.streams.followed

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.apollographql.apollo3.ApolloClient
import com.github.andreyasadchy.xtra.api.HelixApi
import com.github.andreyasadchy.xtra.model.User
import com.github.andreyasadchy.xtra.repository.GraphQLRepository
import com.github.andreyasadchy.xtra.repository.LocalFollowChannelRepository
import com.github.andreyasadchy.xtra.repository.datasource.FollowedStreamsDataSource
import com.github.andreyasadchy.xtra.util.C
import com.github.andreyasadchy.xtra.util.TwitchApiHelper
import com.github.andreyasadchy.xtra.util.prefs
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class FollowedStreamsViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val localFollowsChannel: LocalFollowChannelRepository,
    private val graphQLRepository: GraphQLRepository,
    private val helix: HelixApi,
    private val apolloClient: ApolloClient) : ViewModel() {

    val compactAdapter = context.prefs().getBoolean(C.COMPACT_STREAMS, false)

    val flow = Pager(
        if (compactAdapter) {
            PagingConfig(pageSize = 30, prefetchDistance = 10, initialLoadSize = 30)
        } else {
            PagingConfig(pageSize = 10, prefetchDistance = 3, initialLoadSize = 15)
        }
    ) {
        FollowedStreamsDataSource(
            localFollowsChannel = localFollowsChannel,
            userId = User.get(context).id,
            helixClientId = context.prefs().getString(C.HELIX_CLIENT_ID, ""),
            helixToken = User.get(context).helixToken,
            helixApi = helix,
            gqlClientId = context.prefs().getString(C.GQL_CLIENT_ID, ""),
            gqlToken = User.get(context).gqlToken,
            gqlApi = graphQLRepository,
            apolloClient = apolloClient,
            apiPref = TwitchApiHelper.listFromPrefs(context.prefs().getString(C.API_PREF_FOLLOWED_STREAMS, ""), TwitchApiHelper.followedStreamsApiDefaults))
    }.flow.cachedIn(viewModelScope)
}