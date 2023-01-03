package com.github.andreyasadchy.xtra.ui.player

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.XtraApp
import com.github.andreyasadchy.xtra.ui.common.BaseAndroidViewModel
import com.github.andreyasadchy.xtra.ui.common.OnQualityChangeListener
import com.github.andreyasadchy.xtra.ui.player.stream.StreamPlayerViewModel
import com.github.andreyasadchy.xtra.util.*
import com.github.andreyasadchy.xtra.util.C
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.Player.EVENT_PLAYBACK_STATE_CHANGED
import com.google.android.exoplayer2.Player.EVENT_PLAY_WHEN_READY_CHANGED
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.trackselection.TrackSelectionOverride
import com.google.android.exoplayer2.upstream.HttpDataSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import kotlin.concurrent.schedule


abstract class PlayerViewModel(context: Application) : BaseAndroidViewModel(context), Player.Listener, OnQualityChangeListener {

    protected val tag: String = javaClass.simpleName

    var player: ExoPlayer? = null
    protected var mediaSourceFactory: MediaSource.Factory? = null
    protected lateinit var mediaItem: MediaItem //TODO maybe redo these viewmodels to custom players

    protected val _playerUpdated = MutableLiveData<Boolean>()
    val playerUpdated: LiveData<Boolean>
        get() = _playerUpdated
    protected val _playerMode = MutableLiveData<PlayerMode>().apply { value = PlayerMode.NORMAL }
    val playerMode: LiveData<PlayerMode>
        get() = _playerMode
    var qualityIndex = 0
        protected set
    protected var previousQuality = 0
    protected var playbackPosition: Long = 0

    protected var binder: AudioPlayerService.AudioBinder? = null

    protected var isResumed = true
    var pauseHandled = false

    lateinit var mediaSession: MediaSessionCompat
    lateinit var mediaSessionConnector: MediaSessionConnector

    private val _showPauseButton = MutableLiveData<Boolean>()
    val showPauseButton: LiveData<Boolean>
        get() = _showPauseButton
    protected val _isPlaying = MutableLiveData<Boolean>()
    val isPlaying: LiveData<Boolean>
        get() = _isPlaying
    private val _subtitlesAvailable = MutableLiveData<Boolean>()
    val subtitlesAvailable: LiveData<Boolean>
        get() = _subtitlesAvailable

    private var timer: Timer? = null
    private val _sleepTimer = MutableLiveData<Boolean>()
    val sleepTimer: LiveData<Boolean>
        get() = _sleepTimer
    private var timerEndTime = 0L
    val timerTimeLeft
        get() = timerEndTime - System.currentTimeMillis()

    init {
        val volume = context.prefs().getInt(C.PLAYER_VOLUME, 100) / 100f
        setVolume(volume)
    }

    fun setTimer(duration: Long) {
        timer?.let {
            it.cancel()
            timerEndTime = 0L
            timer = null
        }
        if (duration > 0L) {
            timer = Timer().apply {
                timerEndTime = System.currentTimeMillis() + duration
                schedule(duration) {
                    stopBackgroundAudio()
                    _sleepTimer.postValue(true)
                }
            }
        }
    }

    open fun onResume() {
        initializePlayer()
    }

    open fun onPause() {
        releasePlayer()
    }

    open fun restartPlayer() {
        playbackPosition = player?.currentPosition ?: 0
        player?.stop()
        initializePlayer()
        player?.seekTo(playbackPosition)
    }

    protected fun initializePlayer() {
        if (player == null) {
            val context = getApplication<Application>()
            player = ExoPlayer.Builder(context).apply {
                if (context.prefs().getBoolean(C.PLAYER_FORCE_FIRST_DECODER, false)) {
                    setRenderersFactory(DefaultRenderersFactory(context).setMediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
                        MediaCodecSelector.DEFAULT.getDecoderInfos(mimeType, requiresSecureDecoder, requiresTunnelingDecoder).firstOrNull()?.let {
                            listOf(it)
                        } ?: emptyList()
                    })
                }
                mediaSourceFactory?.let { setMediaSourceFactory(it) }
                setLoadControl(DefaultLoadControl.Builder().setBufferDurationsMs(
                    context.prefs().getString(C.PLAYER_BUFFER_MIN, "15000")?.toIntOrNull() ?: 15000,
                    context.prefs().getString(C.PLAYER_BUFFER_MAX, "50000")?.toIntOrNull() ?: 50000,
                    context.prefs().getString(C.PLAYER_BUFFER_PLAYBACK, "2000")?.toIntOrNull() ?: 2000,
                    context.prefs().getString(C.PLAYER_BUFFER_REBUFFER, "5000")?.toIntOrNull() ?: 5000
                ).build())
                setSeekBackIncrementMs(context.prefs().getString(C.PLAYER_REWIND, "10000")?.toLongOrNull() ?: 10000)
                setSeekForwardIncrementMs(context.prefs().getString(C.PLAYER_FORWARD, "10000")?.toLongOrNull() ?: 10000)
            }.build().apply {
                addListener(this@PlayerViewModel)
                playWhenReady = true
            }
            _playerUpdated.postValue(true)
        }
        if (this::mediaItem.isInitialized) {
            player?.setMediaItem(mediaItem)
            player?.prepare()
            mediaSessionConnector.setPlayer(player)
            mediaSession.isActive = true
        }
    }

    protected fun releasePlayer() {
        player?.release()
        player = null
        _playerUpdated.postValue(true)
        mediaSessionConnector.setPlayer(null)
        mediaSession.isActive = false
    }

    protected fun startBackgroundAudio(playlistUrl: String, channelName: String?, title: String?, imageUrl: String?, usePlayPause: Boolean, type: Int, videoId: Number?, showNotification: Boolean) {
        val context = XtraApp.INSTANCE //TODO
        val intent = Intent(context, AudioPlayerService::class.java).apply {
            putExtra(AudioPlayerService.KEY_PLAYLIST_URL, playlistUrl)
            putExtra(AudioPlayerService.KEY_CHANNEL_NAME, channelName)
            putExtra(AudioPlayerService.KEY_TITLE, title)
            putExtra(AudioPlayerService.KEY_IMAGE_URL, imageUrl)
            putExtra(AudioPlayerService.KEY_USE_PLAY_PAUSE, usePlayPause)
            putExtra(AudioPlayerService.KEY_CURRENT_POSITION, player?.currentPosition)
            putExtra(AudioPlayerService.KEY_TYPE, type)
            putExtra(AudioPlayerService.KEY_VIDEO_ID, videoId)
        }
        releasePlayer()
        val connection = object : ServiceConnection {

            override fun onServiceDisconnected(name: ComponentName) {
            }

            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                binder = service as AudioPlayerService.AudioBinder
                player = service.player
                _playerUpdated.postValue(true)
                if (showNotification) {
                    showAudioNotification()
                }
            }
        }
        AudioPlayerService.connection = connection
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    protected fun stopBackgroundAudio() {
        AudioPlayerService.connection?.let {
//            val context = getApplication<Application>()
            XtraApp.INSTANCE.unbindService(it) //TODO
        }
    }

    protected fun showAudioNotification() {
        binder?.showNotification()
    }

    protected fun hideAudioNotification() {
        if (AudioPlayerService.connection != null) {
            binder?.hideNotification()
        } else {
            qualityIndex = previousQuality
            releasePlayer()
            initializePlayer()
            player?.seekTo(AudioPlayerService.position)
        }
    }

    //Player.Listener

    override fun onEvents(player: Player, events: Player.Events) {
        if (events.containsAny(EVENT_PLAYBACK_STATE_CHANGED, EVENT_PLAY_WHEN_READY_CHANGED)) {
            _showPauseButton.postValue(player.playbackState != Player.STATE_ENDED && player.playbackState != Player.STATE_IDLE && player.playWhenReady)
        }
        super.onEvents(player, events)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        _isPlaying.postValue(isPlaying)
    }

    override fun onTracksChanged(tracks: Tracks) {
        _subtitlesAvailable.postValue(tracks.groups.find { it.type == com.google.android.exoplayer2.C.TRACK_TYPE_TEXT } != null)
    }

    override fun onPlayerError(error: PlaybackException) {
        val playerError = player?.playerError
        Log.e(tag, "Player error", playerError)
        playbackPosition = player?.currentPosition ?: 0
        val context = getApplication<Application>()
        if (context.isNetworkAvailable) {
            try {
                val isStreamEnded = try {
                    playerError?.type == ExoPlaybackException.TYPE_SOURCE &&
                            this@PlayerViewModel is StreamPlayerViewModel &&
                            playerError.sourceException.let { it is HttpDataSource.InvalidResponseCodeException && it.responseCode == 404 }
                } catch (e: IllegalStateException) {
//                    Crashlytics.log(Log.ERROR, tag, "onPlayerError: Stream end check error. Type: ${error.type}")
//                    Crashlytics.logException(e)
                    return
                }
                if (isStreamEnded) {
                    context.toast(R.string.stream_ended)
                } else {
                    context.shortToast(R.string.player_error)
                    viewModelScope.launch {
                        delay(1500L)
                        try {
                            restartPlayer()
                        } catch (e: Exception) {
//                            Crashlytics.log(Log.ERROR, tag, "onPlayerError: Retry error. ${e.message}")
//                            Crashlytics.logException(e)
                        }
                    }
                }
            } catch (e: Exception) {
//                Crashlytics.log(Log.ERROR, tag, "onPlayerError ${e.message}")
//                Crashlytics.logException(e)
            }
        }
    }

    override fun onCleared() {
        releasePlayer()
        timer?.cancel()
    }

    fun setSpeed(speed: Float) {
        player?.playbackParameters = PlaybackParameters(speed)
    }

    fun setVolume(volume: Float) {
        player?.volume = volume
    }

    fun subtitlesEnabled(): Boolean {
        return player?.currentTracks?.groups?.find { it.type == com.google.android.exoplayer2.C.TRACK_TYPE_TEXT }?.isSelected == true
    }

    fun toggleSubtitles(enabled: Boolean) {
        player?.let { player ->
            if (enabled) {
                player.currentTracks.groups.find { it.type == com.google.android.exoplayer2.C.TRACK_TYPE_TEXT }?.let {
                    player.trackSelectionParameters = player.trackSelectionParameters
                        .buildUpon()
                        .setOverrideForType(TrackSelectionOverride(it.mediaTrackGroup, 0))
                        .build()
                }
            } else {
                player.trackSelectionParameters = player.trackSelectionParameters
                    .buildUpon()
                    .clearOverridesOfType(com.google.android.exoplayer2.C.TRACK_TYPE_TEXT)
                    .build()
            }
        }
    }
}