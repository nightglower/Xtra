package com.github.andreyasadchy.xtra.ui.player.video

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.databinding.FragmentPlayerVideoBinding
import com.github.andreyasadchy.xtra.model.User
import com.github.andreyasadchy.xtra.model.helix.video.Video
import com.github.andreyasadchy.xtra.ui.chat.ChatReplayPlayerFragment
import com.github.andreyasadchy.xtra.ui.common.RadioButtonDialogFragment
import com.github.andreyasadchy.xtra.ui.common.RadioButtonDialogFragmentDirections
import com.github.andreyasadchy.xtra.ui.download.HasDownloadDialog
import com.github.andreyasadchy.xtra.ui.download.VideoDownloadDialogDirections
import com.github.andreyasadchy.xtra.ui.player.*
import com.github.andreyasadchy.xtra.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class VideoPlayerFragment : BasePlayerFragment(), HasDownloadDialog, ChatReplayPlayerFragment, RadioButtonDialogFragment.OnSortOptionChanged, PlayerSettingsDialog.PlayerSettingsListener, PlayerVolumeDialog.PlayerVolumeListener, PlayerGamesDialog.PlayerSeekListener {

    private var _binding: FragmentPlayerVideoBinding? = null
    private val binding get() = _binding!!
    private val args: VideoPlayerFragmentArgs by navArgs()
    override val viewModel: VideoPlayerViewModel by viewModels()

    private lateinit var video: Video
    override val channelId: String?
        get() = video.user_id
    override val channelLogin: String?
        get() = video.user_login
    override val channelName: String?
        get() = video.user_name
    override val channelImage: String?
        get() = video.channelLogo

    override val layoutId: Int
        get() = R.layout.fragment_player_video
    override val chatContainerId: Int
        get() = R.id.chatFragmentContainer

    override val shouldEnterPictureInPicture: Boolean
        get() = viewModel.playerMode.value == PlayerMode.NORMAL

    override val controllerShowTimeoutMs: Int = 5000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        video = args.video
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPlayerVideoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
/*        if (childFragmentManager.findFragmentById(R.id.chatFragmentContainer) == null) {
            childFragmentManager.beginTransaction().replace(R.id.chatFragmentContainer, ChatFragment.newInstance(channelId, video.id, 0.0)).commit()
        }*/
    }

    override fun initialize() {
        viewModel.setVideo(
            gqlClientId = prefs.getString(C.GQL_CLIENT_ID, ""),
            gqlToken = if (prefs.getBoolean(C.TOKEN_INCLUDE_TOKEN_VIDEO, true)) User.get(requireContext()).gqlToken else null,
            video = video,
            offset = args.offset.toDouble()
        )
        super.initialize()
        val settings = requireView().findViewById<ImageButton>(R.id.playerSettings)
        val playerMenu = requireView().findViewById<ImageButton>(R.id.playerMenu)
        val download = requireView().findViewById<ImageButton>(R.id.playerDownload)
        val gamesButton = requireView().findViewById<ImageButton>(R.id.playerGames)
        val mode = requireView().findViewById<ImageButton>(R.id.playerMode)
        viewModel.loaded.observe(viewLifecycleOwner) {
            if (it) {
                settings.enable()
                download.enable()
                //(childFragmentManager.findFragmentByTag("closeOnPip") as? PlayerSettingsDialog?)?.setQualities(viewModel.qualities, viewModel.qualityIndex)
            } else {
                download.disable()
                settings.disable()
            }
        }
        checkBookmark()
        viewModel.bookmarkItem.observe(viewLifecycleOwner) {
            //(childFragmentManager.findFragmentByTag("closeOnPip") as? PlayerSettingsDialog?)?.setBookmarkText(it != null)
        }
        if (prefs.getBoolean(C.PLAYER_SETTINGS, true)) {
            settings.visible()
            settings.setOnClickListener {
                findNavController().navigate(RadioButtonDialogFragmentDirections.actionGlobalRadioButtonDialogFragment(
                    requestCode = 0,
                    labels = viewModel.qualities.toTypedArray(),
                    checkedIndex = viewModel.qualityIndex
                ))
            }
        }
        if (prefs.getBoolean(C.PLAYER_MENU, true)) {
            playerMenu.visible()
            playerMenu.setOnClickListener {
                findNavController().navigate(PlayerSettingsDialogDirections.actionGlobalPlayerSettingsDialog(
                    qualities = viewModel.qualities.toTypedArray(),
                    qualityIndex = viewModel.qualityIndex,
                    speed = viewModel.currentPlayer.value!!.playbackParameters.speed,
                    vodGames = !viewModel.gamesList.value.isNullOrEmpty()
                ))
            }
        }
        if (prefs.getBoolean(C.PLAYER_DOWNLOAD, false)) {
            download.visible()
            download.setOnClickListener {
                showDownloadDialog()
            }
        }
        if (prefs.getBoolean(C.PLAYER_GAMESBUTTON, true) || prefs.getBoolean(C.PLAYER_MENU_GAMES, false)) {
            viewModel.loadGamesList(prefs.getString(C.GQL_CLIENT_ID, ""), video.id)
            viewModel.gamesList.observe(viewLifecycleOwner) { list ->
                if (list.isNotEmpty()) {
                    if (prefs.getBoolean(C.PLAYER_GAMESBUTTON, true)) {
                        gamesButton.visible()
                        gamesButton.setOnClickListener { showVodGames() }
                    }
                    //(childFragmentManager.findFragmentByTag("closeOnPip") as? PlayerSettingsDialog?)?.setVodGames()
                }
            }
        }
        if (prefs.getBoolean(C.PLAYER_MODE, false)) {
            mode.visible()
            mode.setOnClickListener {
                if (viewModel.playerMode.value != PlayerMode.AUDIO_ONLY) {
                    startAudioOnly()
                } else {
                    viewModel.onResume()
                }
            }
        }
    }

    fun showVodGames() {
        viewModel.gamesList.value?.let { findNavController().navigate(PlayerGamesDialogDirections.actionGlobalPlayerGamesDialog(it.toTypedArray())) }
    }

    fun checkBookmark() {
        viewModel.checkBookmark()
    }

    fun isBookmarked() {
        //(childFragmentManager.findFragmentByTag("closeOnPip") as? PlayerSettingsDialog?)?.setBookmarkText(viewModel.bookmarkItem.value != null)
    }

    fun saveBookmark() {
        viewModel.saveBookmark(requireContext(), prefs.getString(C.HELIX_CLIENT_ID, ""), User.get(requireContext()).helixToken, prefs.getString(C.GQL_CLIENT_ID, ""))
    }

    override fun onChange(requestCode: Int, index: Int, text: CharSequence, tag: Int?) {
        viewModel.changeQuality(index)
    }

    override fun onChangeQuality(index: Int) {
        viewModel.changeQuality(index)
    }

    override fun onChangeSpeed(speed: Float) {
        viewModel.setSpeed(speed)
    }

    override fun changeVolume(volume: Float) {
        viewModel.setVolume(volume)
    }

    override fun seek(position: Long) {
        viewModel.seek(position)
    }

    override fun showDownloadDialog() {
        if (DownloadUtils.hasStoragePermission(requireActivity())) {
            viewModel.videoInfo?.let { findNavController().navigate(VideoDownloadDialogDirections.actionGlobalVideoDownloadDialog(videoInfo = it)) }
        }
    }

    override fun onMovedToForeground() {
        viewModel.onResume()
    }

    override fun onMovedToBackground() {
        viewModel.onPause()
    }

    override fun onNetworkRestored() {
        if (isResumed) {
            viewModel.onResume()
        }
    }

    override fun onNetworkLost() {
        if (isResumed) {
            viewModel.onPause()
        }
    }

    override fun getCurrentPosition(): Double {
        return runBlocking(Dispatchers.Main) { viewModel.currentPlayer.value!!.currentPosition / 1000.0 }
    }

    fun startAudioOnly() {
        viewModel.startAudioOnly()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
