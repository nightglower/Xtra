package com.github.andreyasadchy.xtra.util.chat

import com.github.andreyasadchy.xtra.model.chat.ChatMessage
import com.github.andreyasadchy.xtra.util.TwitchApiHelper

interface PubSubListener {
    fun onPlaybackMessage(live: Boolean?, viewers: Int?)
    fun onRewardMessage(message: ChatMessage)
    fun onPointsEarned(message: PointsEarned)
    fun onClaimAvailable()
    fun onMinuteWatched()
    fun onRaidUpdate(message: Raid)
}

data class PointsEarned(
    val pointsGained: Int? = null,
    val timestamp: Long? = null,
    val fullMsg: String? = null)

data class Raid(
    val raidId: String? = null,
    val targetId: String? = null,
    val targetLogin: String? = null,
    val targetName: String? = null,
    val targetProfileImage: String? = null,
    val viewerCount: Int? = null,
    val openStream: Boolean) {

    val targetLogo: String?
        get() = TwitchApiHelper.getTemplateUrl(targetProfileImage, "profileimage")
}
