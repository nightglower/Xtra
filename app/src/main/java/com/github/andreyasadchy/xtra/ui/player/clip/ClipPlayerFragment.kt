package com.github.andreyasadchy.xtra.ui.player.clip

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.databinding.FragmentPlayerClipBinding
import com.github.andreyasadchy.xtra.model.helix.clip.Clip
import com.github.andreyasadchy.xtra.model.helix.video.Video
import com.github.andreyasadchy.xtra.ui.chat.ChatReplayPlayerFragment
import com.github.andreyasadchy.xtra.ui.common.RadioButtonDialogFragment
import com.github.andreyasadchy.xtra.ui.common.RadioButtonDialogFragmentDirections
import com.github.andreyasadchy.xtra.ui.download.ClipDownloadDialogDirections
import com.github.andreyasadchy.xtra.ui.download.HasDownloadDialog
import com.github.andreyasadchy.xtra.ui.main.MainActivity
import com.github.andreyasadchy.xtra.ui.player.BasePlayerFragment
import com.github.andreyasadchy.xtra.ui.player.PlayerSettingsDialog
import com.github.andreyasadchy.xtra.ui.player.PlayerSettingsDialogDirections
import com.github.andreyasadchy.xtra.ui.player.PlayerVolumeDialog
import com.github.andreyasadchy.xtra.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class ClipPlayerFragment : BasePlayerFragment(), HasDownloadDialog, ChatReplayPlayerFragment, RadioButtonDialogFragment.OnSortOptionChanged, PlayerSettingsDialog.PlayerSettingsListener, PlayerVolumeDialog.PlayerVolumeListener {

    private var _binding: FragmentPlayerClipBinding? = null
    private val binding get() = _binding!!
    private val args: ClipPlayerFragmentArgs by navArgs()
    override val viewModel: ClipPlayerViewModel by viewModels()

    private lateinit var clip: Clip
    override val channelId: String?
        get() = clip.broadcaster_id
    override val channelLogin: String?
        get() = clip.broadcaster_login
    override val channelName: String?
        get() = clip.broadcaster_name
    override val channelImage: String?
        get() = clip.channelLogo

    override val layoutId: Int
        get() = R.layout.fragment_player_clip
    override val chatContainerId: Int
        get() = R.id.clipChatContainer

    override val shouldEnterPictureInPicture: Boolean
        get() = true

    override val controllerShowTimeoutMs: Int = 2500

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        clip = args.clip
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPlayerClipBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
/*        if (childFragmentManager.findFragmentById(R.id.chatFragmentContainer) == null) {
            childFragmentManager.beginTransaction().replace(R.id.chatFragmentContainer, ChatFragment.newInstance(channelId, clip.video_id, clip.videoOffsetSeconds?.toDouble())).commit()
        }*/
        if (clip.video_id.isNullOrBlank()) {
            binding.watchVideo.gone()
        }
    }

    override fun initialize() {
        viewModel.setClip(clip)
        super.initialize()
        val settings = requireView().findViewById<ImageButton>(R.id.playerSettings)
        val playerMenu = requireView().findViewById<ImageButton>(R.id.playerMenu)
        val download = requireView().findViewById<ImageButton>(R.id.playerDownload)
        viewModel.loaded.observe(this) {
            settings.enable()
            download.enable()
            //(childFragmentManager.findFragmentByTag("closeOnPip") as? PlayerSettingsDialog?)?.setQualities(viewModel.qualities.keys.toList(), viewModel.qualityIndex)
        }
        if (prefs.getBoolean(C.PLAYER_SETTINGS, true)) {
            settings.visible()
            settings.setOnClickListener {
                findNavController().navigate(RadioButtonDialogFragmentDirections.actionGlobalRadioButtonDialogFragment(
                    requestCode = 0,
                    labels = viewModel.qualities.keys.toTypedArray(),
                    checkedIndex = viewModel.qualityIndex
                ))
            }
        }
        if (prefs.getBoolean(C.PLAYER_MENU, true)) {
            playerMenu.visible()
            playerMenu.setOnClickListener {
                findNavController().navigate(PlayerSettingsDialogDirections.actionGlobalPlayerSettingsDialog(
                    qualities = viewModel.qualities.keys.toTypedArray(),
                    qualityIndex = viewModel.qualityIndex,
                    speed = viewModel.currentPlayer.value!!.playbackParameters.speed,
                    vodGames = false
                ))
            }
        }
        if (prefs.getBoolean(C.PLAYER_DOWNLOAD, false)) {
            download.visible()
            download.setOnClickListener {
                showDownloadDialog()
            }
        }
        if (!clip.video_id.isNullOrBlank()) {
            binding.watchVideo.setOnClickListener {
                (requireActivity() as MainActivity).startVideo(Video(
                    id = clip.video_id!!,
                    user_id = clip.broadcaster_id,
                    user_login = clip.broadcaster_login,
                    user_name = clip.broadcaster_name,
                    profileImageURL = clip.profileImageURL
                ), (if (clip.videoOffsetSeconds != null) {
                    (clip.videoOffsetSeconds?.toDouble() ?: 0.0) * 1000.0 + viewModel.player.currentPosition
                } else {
                    0.0
                }))
            }
        }
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

    override fun showDownloadDialog() {
        if (DownloadUtils.hasStoragePermission(requireActivity())) {
            findNavController().navigate(ClipDownloadDialogDirections.actionGlobalClipDownloadDialog(
                clip = clip,
                qualityKeys = viewModel.qualities.keys.toTypedArray(),
                qualityValues = viewModel.qualities.values.toTypedArray()
            ))
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
