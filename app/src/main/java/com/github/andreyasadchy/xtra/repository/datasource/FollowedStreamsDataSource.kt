package com.github.andreyasadchy.xtra.repository.datasource

import androidx.core.util.Pair
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.github.andreyasadchy.xtra.FollowedStreamsQuery
import com.github.andreyasadchy.xtra.UsersStreamQuery
import com.github.andreyasadchy.xtra.api.HelixApi
import com.github.andreyasadchy.xtra.model.helix.stream.Stream
import com.github.andreyasadchy.xtra.model.helix.tag.Tag
import com.github.andreyasadchy.xtra.repository.GraphQLRepository
import com.github.andreyasadchy.xtra.repository.LocalFollowChannelRepository
import com.github.andreyasadchy.xtra.util.C

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
    private val apiPref: ArrayList<Pair<Long?, String?>?>) : PagingSource<Int, Stream>() {
    private var api: String? = null
    private var offset: String? = null
    private var nextPage: Boolean = true

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Stream> {
        return try {
            val response = try {
                if (!offset.isNullOrBlank()) {
                    when (api) {
                        C.HELIX -> helixLoad()
                        C.GQL_QUERY -> if (nextPage) gqlQueryLoad() else listOf()
                        C.GQL -> gqlLoad()
                        else -> listOf()
                    }
                } else {
                    val list = mutableListOf<Stream>()
                    val localIds = localFollowsChannel.loadFollows().map { it.user_id }
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
                            val item = list.find { it.user_id == i.user_id }
                            if (item == null) {
                                list.add(i)
                            }
                        }
                    }
                    list.sortByDescending { it.viewer_count }
                    list
                }
            } catch (e: Exception) {
                listOf()
            }
            LoadResult.Page(
                data = response,
                prevKey = null,
                nextKey = if (!offset.isNullOrBlank() && (api != C.GQL_QUERY || nextPage)) (params.key ?: 1) + 1 else null
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    private suspend fun helixLoad(): List<Stream> {
        val get = helixApi.getFollowedStreams(helixClientId, helixToken, userId, 100, offset)
        val list = mutableListOf<Stream>()
        get.data?.let { list.addAll(it) }
        val ids = list.mapNotNull { it.user_id }
        if (ids.isNotEmpty()) {
            val users = helixApi.getUsers(helixClientId, helixToken, ids).data
            if (users != null) {
                for (i in users) {
                    val item = list.find { it.user_id == i.id }
                    if (item != null) {
                        item.profileImageURL = i.profile_image_url
                    }
                }
            }
        }
        offset = get.pagination?.cursor
        return list
    }

    private suspend fun gqlQueryLoad(): List<Stream> {
        val get1 = apolloClient.newBuilder().apply {
            gqlClientId?.let { addHttpHeader("Client-ID", it) }
            gqlToken?.let { addHttpHeader("Authorization", it) }
        }.build().query(FollowedStreamsQuery(
            id = Optional.Present(userId),
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
                    user_id = i?.node?.id,
                    user_login = i?.node?.login,
                    user_name = i?.node?.displayName,
                    game_id = i?.node?.stream?.game?.id,
                    game_name = i?.node?.stream?.game?.displayName,
                    type = i?.node?.stream?.type,
                    title = i?.node?.stream?.broadcaster?.broadcastSettings?.title,
                    viewer_count = i?.node?.stream?.viewersCount,
                    started_at = i?.node?.stream?.createdAt?.toString(),
                    thumbnail_url = i?.node?.stream?.previewImageURL,
                    profileImageURL = i?.node?.profileImageURL,
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
        return get.data
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
                        streams.add(Stream(id = i.stream.id, user_id = i.id, user_login = i.login, user_name = i.displayName, game_id = i.stream.game?.id,
                            game_name = i.stream.game?.displayName, type = i.stream.type, title = i.stream.broadcaster?.broadcastSettings?.title,
                            viewer_count = i.stream.viewersCount, started_at = i.stream.createdAt?.toString(), thumbnail_url = i.stream.previewImageURL,
                            profileImageURL = i.profileImageURL, tags = tags)
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
            val get = helixApi.getStreams(helixClientId, helixToken, localIds).data
            if (get != null) {
                for (i in get) {
                    if (i.viewer_count != null) {
                        streams.add(i)
                    }
                }
            }
        }
        if (streams.isNotEmpty()) {
            val userIds = streams.mapNotNull { it.user_id }
            for (streamIds in userIds.chunked(100)) {
                val users = helixApi.getUsers(helixClientId, helixToken, streamIds).data
                if (users != null) {
                    for (i in users) {
                        val item = streams.find { it.user_id == i.id }
                        if (item != null) {
                            item.profileImageURL = i.profile_image_url
                        }
                    }
                }
            }
        }
        return streams
    }

    override fun getRefreshKey(state: PagingState<Int, Stream>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }
}