package com.github.andreyasadchy.xtra.repository.datasource

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.github.andreyasadchy.xtra.model.helix.tag.Tag
import com.github.andreyasadchy.xtra.repository.GraphQLRepository

class TagsDataSourceGQL(
    private val clientId: String?,
    private val getGameTags: Boolean,
    private val gameId: String?,
    private val gameName: String?,
    private val query: String?,
    private val api: GraphQLRepository) : PagingSource<Int, Tag>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Tag> {
        return try {
            val response = try {
                if (gameId != null && gameName != null) {
                    if (query.isNullOrBlank()) {
                        val get = api.loadGameStreamTags(clientId, gameName)
                        get.data.ifEmpty { listOf() }
                    } else {
                        val search = api.loadSearchGameTags(clientId, gameId, query)
                        search.data.ifEmpty { listOf() }
                    }
                } else {
                    if (query.isNullOrBlank()) {
                        if (getGameTags) {
                            if (savedGameTags == null) {
                                val get = api.loadGameTags(clientId)
                                if (get.data.isNotEmpty()) {
                                    savedGameTags = get.data
                                    get.data
                                } else listOf()
                            } else savedGameTags ?: listOf()
                        } else {
                            if (savedAllTags == null) {
                                val get = api.loadStreamTags(clientId)
                                if (get.data.isNotEmpty()) {
                                    savedAllTags = get.data
                                    get.data
                                } else listOf()
                            } else savedAllTags ?: listOf()
                        }
                    } else {
                        val search = api.loadSearchAllTags(clientId, query)
                        search.data.ifEmpty { listOf() }
                    }
                }
            } catch (e: Exception) {
                listOf()
            }
            LoadResult.Page(
                data = response,
                prevKey = null,
                nextKey = null
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Tag>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    companion object {
        private var savedAllTags: List<Tag>? = null
        private var savedGameTags: List<Tag>? = null
    }
}
