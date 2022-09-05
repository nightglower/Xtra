package com.github.andreyasadchy.xtra.repository

import android.util.Base64
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.github.andreyasadchy.xtra.*
import com.github.andreyasadchy.xtra.api.HelixApi
import com.github.andreyasadchy.xtra.api.MiscApi
import com.github.andreyasadchy.xtra.model.chat.CheerEmote
import com.github.andreyasadchy.xtra.model.chat.TwitchEmote
import com.github.andreyasadchy.xtra.model.chat.VideoMessagesResponse
import com.github.andreyasadchy.xtra.model.gql.points.ChannelPointsContextDataResponse
import com.github.andreyasadchy.xtra.model.helix.channel.ChannelViewerList
import com.github.andreyasadchy.xtra.model.helix.clip.Clip
import com.github.andreyasadchy.xtra.model.helix.game.Game
import com.github.andreyasadchy.xtra.model.helix.stream.Stream
import com.github.andreyasadchy.xtra.model.helix.user.User
import com.github.andreyasadchy.xtra.model.helix.video.Video
import com.github.andreyasadchy.xtra.ui.view.chat.animateGifs
import com.github.andreyasadchy.xtra.ui.view.chat.emoteQuality
import com.github.andreyasadchy.xtra.util.TwitchApiHelper
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiRepository @Inject constructor(
    private val apolloClient: ApolloClient,
    private val helix: HelixApi,
    private val gql: GraphQLRepository,
    private val misc: MiscApi) {

    private fun getApolloClient(clientId: String? = null, token: String? = null): ApolloClient {
        return apolloClient.newBuilder().apply {
            clientId?.let { addHttpHeader("Client-ID", it) }
            token?.let { addHttpHeader("Authorization", it) }
        }.build()
    }

    suspend fun loadGameBoxArt(gameId: String, helixClientId: String?, helixToken: String?, gqlClientId: String?): String? = withContext(Dispatchers.IO) {
        try {
            getApolloClient(gqlClientId).query(GameBoxArtQuery(Optional.Present(gameId))).execute().data?.game?.boxArtURL
        } catch (e: Exception) {
            helix.getGames(helixClientId, helixToken?.let { TwitchApiHelper.addTokenPrefixHelix(it) }, listOf(gameId)).data?.firstOrNull()?.boxArt
        }
    }

    suspend fun loadStream(channelId: String, channelLogin: String?, helixClientId: String?, helixToken: String?, gqlClientId: String?): Stream? = withContext(Dispatchers.IO) {
        try {
            val get = getApolloClient(gqlClientId).query(UsersStreamQuery(Optional.Present(listOf(channelId)))).execute().data?.users?.firstOrNull()
            if (get != null) {
                Stream(id = get.stream?.id, user_id = channelId, user_login = get.login, user_name = get.displayName, game_id = get.stream?.game?.id,
                    game_name = get.stream?.game?.displayName, type = get.stream?.type, title = get.stream?.broadcaster?.broadcastSettings?.title,
                    viewer_count = get.stream?.viewersCount, started_at = get.stream?.createdAt?.toString(), thumbnail_url = get.stream?.previewImageURL,
                    profileImageURL = get.profileImageURL)
            } else null
        } catch (e: Exception) {
            try {
                helix.getStreams(helixClientId, helixToken?.let { TwitchApiHelper.addTokenPrefixHelix(it) }, listOf(channelId)).data?.firstOrNull()
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun loadVideo(videoId: String, helixClientId: String?, helixToken: String?, gqlClientId: String?): Video? = withContext(Dispatchers.IO) {
        try {
            val get = getApolloClient(gqlClientId).query(VideoQuery(Optional.Present(videoId))).execute().data
            if (get != null) {
                Video(id = videoId, user_id = get.video?.owner?.id, user_login = get.video?.owner?.login, user_name = get.video?.owner?.displayName,
                    profileImageURL = get.video?.owner?.profileImageURL, title = get.video?.title, createdAt = get.video?.createdAt?.toString(), thumbnail_url = get.video?.previewThumbnailURL,
                    type = get.video?.broadcastType?.toString(), duration = get.video?.lengthSeconds?.toString())
            } else null
        } catch (e: Exception) {
            helix.getVideos(helixClientId, helixToken?.let { TwitchApiHelper.addTokenPrefixHelix(it) }, listOf(videoId)).data?.firstOrNull()
        }
    }

    suspend fun loadVideos(ids: List<String>, helixClientId: String?, helixToken: String?): List<Video>? = withContext(Dispatchers.IO) {
        helix.getVideos(helixClientId, helixToken?.let { TwitchApiHelper.addTokenPrefixHelix(it) }, ids).data
    }

    suspend fun loadClip(clipId: String, helixClientId: String?, helixToken: String?, gqlClientId: String?): Clip? = withContext(Dispatchers.IO) {
        try {
            var user: Clip? = null
            try {
                user = gql.loadClipData(gqlClientId, clipId).data
            } catch (e: Exception) {}
            val video = gql.loadClipVideo(gqlClientId, clipId).data
            Clip(id = clipId, broadcaster_id = user?.broadcaster_id, broadcaster_login = user?.broadcaster_login, broadcaster_name = user?.broadcaster_name,
                profileImageURL = user?.profileImageURL, video_id = video?.video_id, duration = video?.duration, videoOffsetSeconds = video?.videoOffsetSeconds ?: user?.videoOffsetSeconds)
        } catch (e: Exception) {
            helix.getClips(helixClientId, helixToken?.let { TwitchApiHelper.addTokenPrefixHelix(it) }, listOf(clipId)).data?.firstOrNull()
        }
    }

    suspend fun loadUserChannelPage(channelId: String?, channelLogin: String?, helixClientId: String?, helixToken: String?, gqlClientId: String?): Stream? = withContext(Dispatchers.IO) {
        try {
            getApolloClient(gqlClientId).query(UserChannelPageQuery(Optional.Present(channelId), Optional.Present(channelLogin))).execute().data?.user?.let { i ->
                Stream(
                    id = i.stream?.id,
                    user_id = i.id,
                    user_login = i.login,
                    user_name = i.displayName,
                    game_id = i.stream?.game?.id,
                    game_name = i.stream?.game?.displayName,
                    type = i.stream?.type,
                    title = i.stream?.title,
                    viewer_count = i.stream?.viewersCount,
                    started_at = i.stream?.createdAt?.toString(),
                    profileImageURL = i.profileImageURL,
                    channelUser = User(
                        bannerImageURL = i.bannerImageURL,
                        view_count = i.profileViewCount,
                        created_at = i.createdAt?.toString(),
                        followers_count = i.followers?.totalCount,
                        broadcaster_type = when {
                            i.roles?.isPartner == true -> "partner"
                            i.roles?.isAffiliate == true -> "affiliate"
                            else -> null
                        },
                        type = when {
                            i.roles?.isStaff == true -> "staff"
                            i.roles?.isSiteAdmin == true -> "admin"
                            i.roles?.isGlobalMod == true -> "global_mod"
                            else -> null
                        }
                    ),
                    lastBroadcast = i.lastBroadcast?.startedAt?.toString()
                )
            }
        } catch (e: Exception) {
            helix.getStreams(helixClientId, helixToken?.let { TwitchApiHelper.addTokenPrefixHelix(it) }, channelId?.let { listOf(channelId) }, channelLogin?.let { listOf(channelLogin) }).data?.firstOrNull()
        }
    }

    suspend fun loadUser(channelId: String?, channelLogin: String?, helixClientId: String?, helixToken: String?): User? = withContext(Dispatchers.IO) {
        helix.getUsers(helixClientId, helixToken?.let { TwitchApiHelper.addTokenPrefixHelix(it) }, channelId?.let { listOf(channelId) }, channelLogin?.let { listOf(channelLogin) }).data?.firstOrNull()
    }

    suspend fun loadCheckUser(channelId: String? = null, channelLogin: String? = null, helixClientId: String?, helixToken: String?, gqlClientId: String?): User? = withContext(Dispatchers.IO) {
        try {
            getApolloClient(gqlClientId).query(UserQuery(Optional.Present(channelId), Optional.Present(channelLogin))).execute().data?.user?.let { i ->
                User(
                    id = i.id,
                    login = i.login,
                    display_name = i.displayName,
                    profile_image_url = i.profileImageURL
                )
            }
        } catch (e: Exception) {
            helix.getUsers(helixClientId, helixToken?.let { TwitchApiHelper.addTokenPrefixHelix(it) }, channelId?.let { listOf(channelId) }, channelLogin?.let { listOf(channelLogin) }).data?.firstOrNull()
        }
    }

    suspend fun loadUserMessageClicked(channelId: String? = null, channelLogin: String? = null, targetId: String?, helixClientId: String?, helixToken: String?, gqlClientId: String?): User? = withContext(Dispatchers.IO) {
        try {
            getApolloClient(gqlClientId).query(UserMessageClickedQuery(Optional.Present(channelId), Optional.Present(channelLogin), Optional.Present(targetId))).execute().data?.user?.let { i ->
                User(
                    id = i.id,
                    login = i.login,
                    display_name = i.displayName,
                    profile_image_url = i.profileImageURL,
                    bannerImageURL = i.bannerImageURL,
                    created_at = i.createdAt?.toString(),
                    followedAt = i.follow?.followedAt?.toString()
                )
            }
        } catch (e: Exception) {
            helix.getUsers(helixClientId, helixToken?.let { TwitchApiHelper.addTokenPrefixHelix(it) }, channelId?.let { listOf(channelId) }, channelLogin?.let { listOf(channelLogin) }).data?.firstOrNull()
        }
    }

    suspend fun loadUserTypes(ids: List<String>, helixClientId: String?, helixToken: String?, gqlClientId: String?): List<User>? = withContext(Dispatchers.IO) {
        try {
            getApolloClient(gqlClientId).query(UsersTypeQuery(Optional.Present(ids))).execute().data?.users?.let { get ->
                val list = mutableListOf<User>()
                for (i in get) {
                    list.add(User(
                        id = i?.id,
                        broadcaster_type = when {
                            i?.roles?.isPartner == true -> "partner"
                            i?.roles?.isAffiliate == true -> "affiliate"
                            else -> null
                        },
                        type = when {
                            i?.roles?.isStaff == true -> "staff"
                            i?.roles?.isSiteAdmin == true -> "admin"
                            i?.roles?.isGlobalMod == true -> "global_mod"
                            else -> null
                        }
                    ))
                }
                list
            }
        } catch (e: Exception) {
            helix.getUsers(helixClientId, helixToken?.let { TwitchApiHelper.addTokenPrefixHelix(it) }, ids).data
        }
    }

    suspend fun loadCheerEmotes(userId: String, helixClientId: String?, helixToken: String?, gqlClientId: String?): List<CheerEmote>? = withContext(Dispatchers.IO) {
        try {
            val emotes = mutableListOf<CheerEmote>()
            val get = getApolloClient(gqlClientId).query(CheerEmotesQuery(Optional.Present(userId), Optional.Present(animateGifs), Optional.Present((when (emoteQuality) {"4" -> 4 "3" -> 3 "2" -> 2 else -> 1}).toDouble()))).execute().data
            if (get?.user?.cheer?.emotes != null) {
                for (i in get.user.cheer.emotes) {
                    if (i?.tiers != null) {
                        for (tier in i.tiers) {
                            i.prefix?.let { tier?.bits?.let { it1 -> tier.images?.first()?.url?.let { it2 -> emotes.add(CheerEmote(name = it, minBits = it1, color = tier.color, type = if (animateGifs) "image/gif" else "image/png", url = it2)) } } }
                        }
                    }
                }
            }
            emotes.ifEmpty { null }
        } catch (e: Exception) {
            helix.getCheerEmotes(helixClientId, helixToken?.let { TwitchApiHelper.addTokenPrefixHelix(it) }, userId).emotes
        }
    }

    suspend fun loadUserEmotes(gqlClientId: String?, gqlToken: String?, userId: String, channelId: String?): List<TwitchEmote>? = withContext(Dispatchers.IO) {
        try {
            val emotes = mutableListOf<TwitchEmote>()
            getApolloClient(gqlClientId, gqlToken?.let { TwitchApiHelper.addTokenPrefixGQL(it) }).query(UserEmotesQuery(Optional.Present(userId))).execute().data?.user?.emoteSets?.forEach { set ->
                set.emotes?.forEach { emote ->
                    if (emote?.id != null) {
                        emotes.add(TwitchEmote(
                            name = emote.id,
                            setId = emote.setID,
                            ownerId = emote.owner?.id
                        ))
                    }
                }
            }
            emotes
        } catch (e: Exception) {
            try {
                gql.loadUserEmotes(gqlClientId, gqlToken?.let { TwitchApiHelper.addTokenPrefixGQL(it) }, channelId).data
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun loadEmotesFromSet(helixClientId: String?, helixToken: String?, setIds: List<String>): List<TwitchEmote>? = withContext(Dispatchers.IO) {
        helix.getEmotesFromSet(helixClientId, helixToken?.let { TwitchApiHelper.addTokenPrefixHelix(it) }, setIds).data
    }

    suspend fun loadUserFollowing(helixClientId: String?, helixToken: String?, userId: String?, channelId: String?, gqlClientId: String?, gqlToken: String?, userLogin: String?): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!gqlToken.isNullOrBlank()) gql.loadFollowingUser(gqlClientId, gqlToken.let { TwitchApiHelper.addTokenPrefixGQL(it) }, userLogin).following else throw Exception()
        } catch (e: Exception) {
            helix.getUserFollows(helixClientId, helixToken?.let { TwitchApiHelper.addTokenPrefixHelix(it) }, userId, channelId).total == 1
        }
    }

    suspend fun loadGameFollowing(gqlClientId: String?, gqlToken: String?, gameName: String?): Boolean = withContext(Dispatchers.IO) {
        gql.loadFollowingGame(gqlClientId, gqlToken?.let { TwitchApiHelper.addTokenPrefixGQL(it) }, gameName).following
    }

    suspend fun loadVideoChatLog(gqlClientId: String?, videoId: String, offsetSeconds: Double): VideoMessagesResponse = withContext(Dispatchers.IO) {
        misc.getVideoChatLog(gqlClientId, videoId, offsetSeconds, 100)
    }

    suspend fun loadVideoChatAfter(gqlClientId: String?, videoId: String, cursor: String): VideoMessagesResponse = withContext(Dispatchers.IO) {
        misc.getVideoChatLogAfter(gqlClientId, videoId, cursor, 100)
    }

    suspend fun loadVodGamesGQL(clientId: String?, videoId: String?): List<Game> = withContext(Dispatchers.IO) {
        gql.loadVodGames(clientId, videoId).data
    }

    suspend fun loadChannelViewerListGQL(clientId: String?, channelLogin: String?): ChannelViewerList = withContext(Dispatchers.IO) {
        gql.loadChannelViewerList(clientId, channelLogin).data
    }

    suspend fun loadHosting(gqlClientId: String?, channelId: String?, channelLogin: String?): Stream? = withContext(Dispatchers.IO) {
        try {
            getApolloClient(gqlClientId).query(UserHostingQuery(Optional.Present(channelId), Optional.Present(channelLogin))).execute().data?.user?.hosting?.let { get ->
                Stream(
                    user_id = get.id,
                    user_login = get.login,
                    user_name = get.displayName,
                    profileImageURL = get.profileImageURL,
                )
            }
        } catch (e: Exception) {
            if (!channelLogin.isNullOrBlank()) {
                gql.loadChannelHosting(gqlClientId, channelLogin).data
            } else {
                null
            }
        }
    }

    suspend fun loadChannelPointsContext(gqlClientId: String?, gqlToken: String?, channelLogin: String?): ChannelPointsContextDataResponse = withContext(Dispatchers.IO){
        gql.loadChannelPointsContext(gqlClientId, gqlToken?.let { TwitchApiHelper.addTokenPrefixGQL(it) }, channelLogin)
    }

    suspend fun loadClaimPoints(gqlClientId: String?, gqlToken: String?, channelId: String?, claimID: String?) = withContext(Dispatchers.IO) {
        gql.loadClaimPoints(gqlClientId, gqlToken?.let { TwitchApiHelper.addTokenPrefixGQL(it) }, channelId, claimID)
    }

    suspend fun loadJoinRaid(gqlClientId: String?, gqlToken: String?, raidId: String?) = withContext(Dispatchers.IO) {
        gql.loadJoinRaid(gqlClientId, gqlToken?.let { TwitchApiHelper.addTokenPrefixGQL(it) }, raidId)
    }

    suspend fun loadMinuteWatched(userId: String?, streamId: String?, channelId: String?, channelLogin: String?) = withContext(Dispatchers.IO) {
        try {
            val pageResponse = channelLogin?.let { misc.getChannelPage(it).string() }
            if (!pageResponse.isNullOrBlank()) {
                val settingsRegex = Regex("(https://static.twitchcdn.net/config/settings.*?js)")
                val settingsUrl = settingsRegex.find(pageResponse)?.value
                val settingsResponse = settingsUrl?.let { misc.getUrl(it).string() }
                if (!settingsResponse.isNullOrBlank()) {
                    val spadeRegex = Regex("\"spade_url\":\"(.*?)\"")
                    val spadeUrl = spadeRegex.find(settingsResponse)?.groups?.get(1)?.value
                    if (!spadeUrl.isNullOrBlank()) {
                        val json = JsonObject().apply {
                            addProperty("event", "minute-watched")
                            add("properties", JsonObject().apply {
                                addProperty("channel_id", channelId)
                                addProperty("broadcast_id", streamId)
                                addProperty("player", "site")
                                addProperty("user_id", userId?.toInt())
                            })
                        }
                        val spadeRequest = Base64.encodeToString(json.toString().toByteArray(), Base64.NO_WRAP).toRequestBody()
                        misc.postUrl(spadeUrl, spadeRequest)
                    }
                }
            }
        } catch (e: Exception) {

        }
    }

    suspend fun followUser(gqlClientId: String?, gqlToken: String?, userId: String?): Boolean = withContext(Dispatchers.IO) {
        gql.loadFollowUser(gqlClientId, gqlToken?.let { TwitchApiHelper.addTokenPrefixGQL(it) }, userId).error.isNullOrBlank()
    }

    suspend fun unfollowUser(gqlClientId: String?, gqlToken: String?, userId: String?): Boolean = withContext(Dispatchers.IO) {
        !gql.loadUnfollowUser(gqlClientId, gqlToken?.let { TwitchApiHelper.addTokenPrefixGQL(it) }, userId).isJsonNull
    }

    suspend fun followGame(gqlClientId: String?, gqlToken: String?, gameId: String?): Boolean = withContext(Dispatchers.IO) {
        !gql.loadFollowGame(gqlClientId, gqlToken?.let { TwitchApiHelper.addTokenPrefixGQL(it) }, gameId).isJsonNull
    }

    suspend fun unfollowGame(gqlClientId: String?, gqlToken: String?, gameId: String?): Boolean = withContext(Dispatchers.IO) {
        !gql.loadUnfollowGame(gqlClientId, gqlToken?.let { TwitchApiHelper.addTokenPrefixGQL(it) }, gameId).isJsonNull
    }
}