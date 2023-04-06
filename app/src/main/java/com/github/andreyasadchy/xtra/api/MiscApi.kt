package com.github.andreyasadchy.xtra.api

import com.github.andreyasadchy.xtra.model.chat.*
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface MiscApi {

    @GET("https://www.twitch.tv/{channelLogin}")
    suspend fun getChannelPage(@Path("channelLogin") channelLogin: String): ResponseBody

    @GET
    suspend fun getUrl(@Url url: String): ResponseBody

    @POST
    suspend fun postUrl(@Url url: String, @Body body: RequestBody): Response<Unit>

    @GET("https://recent-messages.robotty.de/api/v2/recent-messages/{channelLogin}")
    suspend fun getRecentMessages(@Path("channelLogin") channelLogin: String, @Query("limit") limit: String): Response<RecentMessagesResponse>

    @GET("https://7tv.io/v3/emote-sets/global")
    suspend fun getGlobalStvEmotes(): Response<StvGlobalResponse>

    @GET("https://7tv.io/v3/users/twitch/{channelId}")
    suspend fun getStvEmotes(@Path("channelId") channelId: String): Response<StvChannelResponse>

    @GET("https://api.betterttv.net/3/cached/emotes/global")
    suspend fun getGlobalBttvEmotes(): Response<BttvGlobalResponse>

    @GET("https://api.betterttv.net/3/cached/users/twitch/{channelId}")
    suspend fun getBttvEmotes(@Path("channelId") channelId: String): Response<BttvChannelResponse>

    @GET("https://api.frankerfacez.com/v1/set/global")
    suspend fun getGlobalFfzEmotes(): Response<FfzGlobalResponse>

    @GET("https://api.frankerfacez.com/v1/room/id/{channelId}")
    suspend fun getFfzEmotes(@Path("channelId") channelId: String): Response<FfzChannelResponse>
}