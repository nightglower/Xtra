package com.github.andreyasadchy.xtra.model.gql.search

import com.github.andreyasadchy.xtra.model.ui.Video
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type

class SearchVideosDataDeserializer : JsonDeserializer<SearchVideosDataResponse> {

    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): SearchVideosDataResponse {
        val data = mutableListOf<Video>()
        val dataJson = json.asJsonObject?.getAsJsonObject("data")?.getAsJsonObject("searchFor")?.getAsJsonObject("videos")
        val cursor = dataJson?.get("cursor")?.takeIf { !it.isJsonNull }?.asString
        dataJson?.getAsJsonArray("edges")?.forEach { item ->
            item?.asJsonObject?.getAsJsonObject("item")?.let { obj ->
                data.add(Video(
                    id = obj.get("id")?.takeIf { !it.isJsonNull }?.asString,
                    channelId = obj.get("owner")?.takeIf { it.isJsonObject }?.asJsonObject?.get("id")?.takeIf { !it.isJsonNull }?.asString,
                    channelLogin = obj.get("owner")?.takeIf { it.isJsonObject }?.asJsonObject?.get("login")?.takeIf { !it.isJsonNull }?.asString,
                    channelName = obj.get("owner")?.takeIf { it.isJsonObject }?.asJsonObject?.get("displayName")?.takeIf { !it.isJsonNull }?.asString,
                    title = obj.get("title")?.takeIf { !it.isJsonNull }?.asString,
                    uploadDate = obj.get("createdAt")?.takeIf { !it.isJsonNull }?.asString,
                    thumbnailUrl = obj.get("previewThumbnailURL")?.takeIf { !it.isJsonNull }?.asString,
                    viewCount = obj.get("viewCount")?.takeIf { !it.isJsonNull }?.asInt,
                    duration = obj.get("lengthSeconds")?.takeIf { !it.isJsonNull }?.asString,
                    gameId = obj.get("game")?.takeIf { it.isJsonObject }?.asJsonObject?.get("id")?.takeIf { !it.isJsonNull }?.asString,
                    gameName = obj.get("game")?.takeIf { it.isJsonObject }?.asJsonObject?.get("displayName")?.takeIf { !it.isJsonNull }?.asString,
                ))
            }
        }
        return SearchVideosDataResponse(data, cursor)
    }
}
