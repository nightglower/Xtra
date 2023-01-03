package com.github.andreyasadchy.xtra.ui.player.offline

import android.app.Application
import androidx.core.net.toUri
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.model.offline.OfflineVideo
import com.github.andreyasadchy.xtra.repository.OfflineRepository
import com.github.andreyasadchy.xtra.ui.player.AudioPlayerService
import com.github.andreyasadchy.xtra.ui.player.PlayerMode
import com.github.andreyasadchy.xtra.ui.player.PlayerViewModel
import com.github.andreyasadchy.xtra.util.C
import com.github.andreyasadchy.xtra.util.prefs
import com.google.android.exoplayer2.MediaItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OfflinePlayerViewModel @Inject constructor(
        context: Application,
        private val repository: OfflineRepository) : PlayerViewModel(context) {

    private lateinit var video: OfflineVideo
    val qualities = mutableListOf(context.getString(R.string.source), context.getString(R.string.audio_only))

    init {
        val speed = context.prefs().getFloat(C.PLAYER_SPEED, 1f)
        setSpeed(speed)
    }

    fun setVideo(video: OfflineVideo) {
        val context = getApplication<Application>()
        if (!this::video.isInitialized) {
            this.video = video
            mediaItem = MediaItem.fromUri(video.url.toUri())
            initializePlayer()
            player?.seekTo(if (context.prefs().getBoolean(C.PLAYER_USE_VIDEOPOSITIONS, true)) video.lastWatchPosition ?: 0 else 0)
        }
    }

    override fun onResume() {
        isResumed = true
        pauseHandled = false
        if (playerMode.value == PlayerMode.NORMAL) {
            initializePlayer()
            player?.seekTo(playbackPosition)
        } else if (playerMode.value == PlayerMode.AUDIO_ONLY) {
            hideAudioNotification()
            if (qualityIndex == 0) {
                changeQuality(qualityIndex)
            }
        }
    }

    override fun onPause() {
        isResumed = false
        if (playerMode.value == PlayerMode.NORMAL) {
            playbackPosition = player?.currentPosition ?: 0
            val context = getApplication<Application>()
            if (!pauseHandled && _isPlaying.value == true && context.prefs().getBoolean(C.PLAYER_LOCK_SCREEN_AUDIO, true)) {
                startAudioOnly(true)
            } else {
                releasePlayer()
            }
        } else if (playerMode.value == PlayerMode.AUDIO_ONLY) {
            showAudioNotification()
        }
    }

    override fun changeQuality(index: Int) {
        qualityIndex = index
        if (qualityIndex == 0) {
            playbackPosition = player?.currentPosition ?: 0
            stopBackgroundAudio()
            releasePlayer()
            initializePlayer()
            player?.seekTo(playbackPosition)
            _playerMode.value = PlayerMode.NORMAL
        } else {
            startAudioOnly()
        }
    }

    fun startAudioOnly(showNotification: Boolean = false) {
        startBackgroundAudio(video.url, video.channelName, video.name, video.channelLogo, true, AudioPlayerService.TYPE_OFFLINE, video.id, showNotification)
        _playerMode.value = PlayerMode.AUDIO_ONLY
    }

    override fun onCleared() {
        if (playerMode.value == PlayerMode.NORMAL) {
            player?.currentPosition?.let { position ->
                repository.updateVideoPosition(video.id, position)
            }
        }
        super.onCleared()
        if (playerMode.value == PlayerMode.AUDIO_ONLY && isResumed) {
            stopBackgroundAudio()
        }
    }
}
