package com.github.andreyasadchy.xtra.repository.datasource

import androidx.core.util.Pair
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.github.andreyasadchy.xtra.SearchStreamsQuery
import com.github.andreyasadchy.xtra.api.HelixApi
import com.github.andreyasadchy.xtra.model.helix.stream.Stream
import com.github.andreyasadchy.xtra.model.helix.tag.Tag
import com.github.andreyasadchy.xtra.util.C

class SearchStreamsDataSource(
    private val query: String,
    private val helixClientId: String?,
    private val helixToken: String?,
    private val helixApi: HelixApi,
    private val gqlClientId: String?,
    private val apolloClient: ApolloClient,
    private val apiPref: ArrayList<Pair<Long?, String?>?>?) : PagingSource<Int, Stream>() {
    private var api: String? = null
    private var offset: String? = null
    private var nextPage: Boolean = true

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Stream> {
        return try {
            val response = try {
                when (apiPref?.elementAt(0)?.second) {
                    C.HELIX -> if (!helixToken.isNullOrBlank()) { api = C.HELIX; helixLoad(params) } else throw Exception()
                    C.GQL_QUERY -> { api = C.GQL_QUERY; gqlQueryLoad(params) }
                    else -> throw Exception()
                }
            } catch (e: Exception) {
                try {
                    when (apiPref?.elementAt(1)?.second) {
                        C.HELIX -> if (!helixToken.isNullOrBlank()) { api = C.HELIX; helixLoad(params) } else throw Exception()
                        C.GQL_QUERY -> { api = C.GQL_QUERY; gqlQueryLoad(params) }
                        else -> throw Exception()
                    }
                } catch (e: Exception) {
                    listOf()
                }
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

    private suspend fun helixLoad(params: LoadParams<Int>): List<Stream> {
        val get = helixApi.getChannels(helixClientId, helixToken, query, params.loadSize, offset, true)
        val list = mutableListOf<Stream>()
        get.data?.forEach {
            list.add(Stream(
                user_id = it.id,
                user_login = it.broadcaster_login,
                user_name = it.display_name,
                game_id = it.game_id,
                game_name = it.game_name,
                title = it.title,
                started_at = it.started_at,
                profileImageURL = it.thumbnail_url,
            ))
        }
        offset = get.pagination?.cursor
        return list
    }

    private suspend fun gqlQueryLoad(params: LoadParams<Int>): List<Stream> {
        val get1 = apolloClient.newBuilder().apply { gqlClientId?.let { addHttpHeader("Client-ID", it) } }.build().query(SearchStreamsQuery(
            query = query,
            first = Optional.Present(params.loadSize),
            after = Optional.Present(offset)
        )).execute().data?.searchStreams
        val get = get1?.edges
        val list = mutableListOf<Stream>()
        if (get != null) {
            for (edge in get) {
                edge.node?.let { i ->
                    val tags = mutableListOf<Tag>()
                    i.tags?.forEach { tag ->
                        tags.add(Tag(
                            id = tag.id,
                            name = tag.localizedName
                        ))
                    }
                    list.add(Stream(
                        id = i.id,
                        user_id = i.broadcaster?.id,
                        user_login = i.broadcaster?.login,
                        user_name = i.broadcaster?.displayName,
                        game_id = i.game?.id,
                        game_name = i.game?.displayName,
                        type = i.type,
                        title = i.broadcaster?.broadcastSettings?.title,
                        viewer_count = i.viewersCount,
                        started_at = i.createdAt?.toString(),
                        thumbnail_url = i.previewImageURL,
                        profileImageURL = i.broadcaster?.profileImageURL,
                        tags = tags
                    ))
                }
            }
            offset = get1.edges.lastOrNull()?.cursor?.toString()
            nextPage = get1.pageInfo?.hasNextPage ?: true
        }
        return list
    }

    override fun getRefreshKey(state: PagingState<Int, Stream>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }
}
