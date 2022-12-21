package com.github.andreyasadchy.xtra.repository.datasource

import androidx.core.util.Pair
import androidx.paging.DataSource
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.github.andreyasadchy.xtra.UserFollowedStreamsQuery
import com.github.andreyasadchy.xtra.UsersStreamQuery
import com.github.andreyasadchy.xtra.api.HelixApi
import com.github.andreyasadchy.xtra.model.ui.Stream
import com.github.andreyasadchy.xtra.model.ui.Tag
import com.github.andreyasadchy.xtra.repository.GraphQLRepository
import com.github.andreyasadchy.xtra.repository.LocalFollowChannelRepository
import com.github.andreyasadchy.xtra.util.C
import kotlinx.coroutines.CoroutineScope

class FollowedStreamsDataSource(
    private val localFollowsChannel: LocalFollowChannelRepository,
    private val userId: String?,
    private val helixClientId: String?,
    private val helixToken: String?,
    private val helixApi: HelixApi,
    private val gqlClientId: String?,
    private val gqlToken: String?,
    private val gqlApi: GraphQLRepository,
    private val apolloClient: ApolloClient,
    private val apiPref: ArrayList<Pair<Long?, String?>?>,
    coroutineScope: CoroutineScope) : BasePositionalDataSource<Stream>(coroutineScope) {
    private var api: String? = null
    private var offset: String? = null
    private var nextPage: Boolean = true

    override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<Stream>) {
        loadInitial(params, callback) {
            val list = mutableListOf<Stream>()
            val localIds = localFollowsChannel.loadFollows().mapNotNull { it.userId }
            val local = if (localIds.isNotEmpty()) {
                try {
                    gqlQueryLocal(localIds)
                } catch (e: Exception) {
                    try {
                        if (!helixToken.isNullOrBlank()) helixLocal(localIds) else throw Exception()
                    } catch (e: Exception) {
                        listOf()
                    }
                }
            } else listOf()
            if (local.isNotEmpty()) {
                list.addAll(local)
            }
            val remote = try {
                when (apiPref.elementAt(0)?.second) {
                    C.HELIX -> if (!helixToken.isNullOrBlank()) { api = C.HELIX; helixLoad() } else throw Exception()
                    C.GQL_QUERY -> if (!gqlToken.isNullOrBlank()) { api = C.GQL_QUERY; gqlQueryLoad() } else throw Exception()
                    C.GQL -> if (!gqlToken.isNullOrBlank()) { api = C.GQL; gqlLoad() } else throw Exception()
                    else -> throw Exception()
                }
            } catch (e: Exception) {
                try {
                    when (apiPref.elementAt(1)?.second) {
                        C.HELIX -> if (!helixToken.isNullOrBlank()) { api = C.HELIX; helixLoad() } else throw Exception()
                        C.GQL_QUERY -> if (!gqlToken.isNullOrBlank()) { api = C.GQL_QUERY; gqlQueryLoad() } else throw Exception()
                        C.GQL -> if (!gqlToken.isNullOrBlank()) { api = C.GQL; gqlLoad() } else throw Exception()
                        else -> throw Exception()
                    }
                } catch (e: Exception) {
                    try {
                        when (apiPref.elementAt(2)?.second) {
                            C.HELIX -> if (!helixToken.isNullOrBlank()) { api = C.HELIX; helixLoad() } else throw Exception()
                            C.GQL_QUERY -> if (!gqlToken.isNullOrBlank()) { api = C.GQL_QUERY; gqlQueryLoad() } else throw Exception()
                            C.GQL -> if (!gqlToken.isNullOrBlank()) { api = C.GQL; gqlLoad() } else throw Exception()
                            else -> throw Exception()
                        }
                    } catch (e: Exception) {
                        listOf()
                    }
                }
            }
            if (remote.isNotEmpty()) {
                for (i in remote) {
                    val item = list.find { it.channelId == i.channelId }
                    if (item == null) {
                        list.add(i)
                    }
                }
            }
            list.sortByDescending { it.viewerCount }
            list
        }
    }

    private suspend fun helixLoad(): List<Stream> {
        val get = helixApi.getFollowedStreams(
            clientId = helixClientId,
            token = helixToken,
            userId = userId,
            limit = 100,
            offset = offset
        )
        val list = mutableListOf<Stream>()
        get.data.let { list.addAll(it) }
        val ids = list.mapNotNull { it.channelId }
        if (ids.isNotEmpty()) {
            val users = helixApi.getUsers(clientId = helixClientId, token = helixToken, ids = ids).data
            for (i in users) {
                val item = list.find { it.channelId == i.channelId }
                if (item != null) {
                    item.profileImageUrl = i.profileImageUrl
                }
            }
        }
        offset = get.cursor
        return list
    }

    private suspend fun gqlQueryLoad(): List<Stream> {
        val get1 = apolloClient.newBuilder().apply {
            gqlClientId?.let { addHttpHeader("Client-ID", it) }
            gqlToken?.let { addHttpHeader("Authorization", it) }
        }.build().query(UserFollowedStreamsQuery(
            first = Optional.Present(100),
            after = Optional.Present(offset)
        )).execute().data?.user?.followedLiveUsers
        val get = get1?.edges
        val list = mutableListOf<Stream>()
        if (get != null) {
            for (i in get) {
                val tags = mutableListOf<Tag>()
                i?.node?.stream?.tags?.forEach { tag ->
                    tags.add(Tag(
                        id = tag.id,
                        name = tag.localizedName
                    ))
                }
                list.add(Stream(
                    id = i?.node?.stream?.id,
                    channelId = i?.node?.id,
                    channelLogin = i?.node?.login,
                    channelName = i?.node?.displayName,
                    gameId = i?.node?.stream?.game?.id,
                    gameName = i?.node?.stream?.game?.displayName,
                    type = i?.node?.stream?.type,
                    title = i?.node?.stream?.broadcaster?.broadcastSettings?.title,
                    viewerCount = i?.node?.stream?.viewersCount,
                    startedAt = i?.node?.stream?.createdAt?.toString(),
                    thumbnailUrl = i?.node?.stream?.previewImageURL,
                    profileImageUrl = i?.node?.profileImageURL,
                    tags = tags
                ))
            }
            offset = get.lastOrNull()?.cursor?.toString()
            nextPage = get1.pageInfo?.hasNextPage ?: true
        }
        return list
    }

    private suspend fun gqlLoad(): List<Stream> {
        val get = gqlApi.loadFollowedStreams(gqlClientId, gqlToken, 100, offset)
        offset = get.cursor
        nextPage = get.hasNextPage ?: true
        return get.data
    }

    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<Stream>) {
        loadRange(params, callback) {
            if (!offset.isNullOrBlank()) {
                when (api) {
                    C.HELIX -> helixLoad()
                    C.GQL_QUERY -> if (nextPage) gqlQueryLoad() else listOf()
                    C.GQL -> if (nextPage) gqlLoad() else listOf()
                    else -> listOf()
                }
            } else listOf()
        }
    }

    private suspend fun gqlQueryLocal(ids: List<String>): List<Stream> {
        val streams = mutableListOf<Stream>()
        for (localIds in ids.chunked(100)) {
            val get = apolloClient.newBuilder().apply { gqlClientId?.let { addHttpHeader("Client-ID", it) } }.build().query(UsersStreamQuery(Optional.Present(localIds))).execute().data?.users
            if (get != null) {
                for (i in get) {
                    if (i?.stream?.viewersCount != null) {
                        val tags = mutableListOf<Tag>()
                        i.stream.tags?.forEach { tag ->
                            tags.add(Tag(
                                id = tag.id,
                                name = tag.localizedName
                            ))
                        }
                        streams.add(Stream(id = i.stream.id, channelId = i.id, channelLogin = i.login, channelName = i.displayName, gameId = i.stream.game?.id,
                            gameName = i.stream.game?.displayName, type = i.stream.type, title = i.stream.broadcaster?.broadcastSettings?.title,
                            viewerCount = i.stream.viewersCount, startedAt = i.stream.createdAt?.toString(), thumbnailUrl = i.stream.previewImageURL,
                            profileImageUrl = i.profileImageURL, tags = tags)
                        )
                    }
                }
            }
        }
        return streams
    }

    private suspend fun helixLocal(ids: List<String>): List<Stream> {
        val streams = mutableListOf<Stream>()
        for (localIds in ids.chunked(100)) {
            val get = helixApi.getStreams(
                clientId = helixClientId,
                token = helixToken,
                ids = localIds
            ).data
            for (i in get) {
                if (i.viewerCount != null) {
                    streams.add(i)
                }
            }
        }
        if (streams.isNotEmpty()) {
            val userIds = streams.mapNotNull { it.channelId }
            for (streamIds in userIds.chunked(100)) {
                val users = helixApi.getUsers(clientId = helixClientId, token = helixToken, ids = streamIds).data
                for (i in users) {
                    val item = streams.find { it.channelId == i.channelId }
                    if (item != null) {
                        item.profileImageUrl = i.profileImageUrl
                    }
                }
            }
        }
        return streams
    }

    class Factory(
        private val localFollowsChannel: LocalFollowChannelRepository,
        private val userId: String?,
        private val helixClientId: String?,
        private val helixToken: String?,
        private val helixApi: HelixApi,
        private val gqlClientId: String?,
        private val gqlToken: String?,
        private val gqlApi: GraphQLRepository,
        private val apolloClient: ApolloClient,
        private val apiPref: ArrayList<Pair<Long?, String?>?>,
        private val coroutineScope: CoroutineScope) : BaseDataSourceFactory<Int, Stream, FollowedStreamsDataSource>() {

        override fun create(): DataSource<Int, Stream> =
                FollowedStreamsDataSource(localFollowsChannel, userId, helixClientId, helixToken, helixApi, gqlClientId, gqlToken, gqlApi, apolloClient, apiPref, coroutineScope).also(sourceLiveData::postValue)
    }
}