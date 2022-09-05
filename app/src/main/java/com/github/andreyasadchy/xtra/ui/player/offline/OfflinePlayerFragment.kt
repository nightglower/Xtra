package com.github.andreyasadchy.xtra.ui.player.offline

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.databinding.FragmentPlayerOfflineBinding
import com.github.andreyasadchy.xtra.model.offline.OfflineVideo
import com.github.andreyasadchy.xtra.ui.common.RadioButtonDialogFragment
import com.github.andreyasadchy.xtra.ui.common.RadioButtonDialogFragmentDirections
import com.github.andreyasadchy.xtra.ui.player.*
import com.github.andreyasadchy.xtra.util.C
import com.github.andreyasadchy.xtra.util.visible

class OfflinePlayerFragment : BasePlayerFragment(), RadioButtonDialogFragment.OnSortOptionChanged, PlayerSettingsDialog.PlayerSettingsListener, PlayerVolumeDialog.PlayerVolumeListener {

    private var _binding: FragmentPlayerOfflineBinding? = null
    private val binding get() = _binding!!
    private val args: OfflinePlayerFragmentArgs by navArgs()
    override val viewModel: OfflinePlayerViewModel by viewModels()

    private lateinit var video: OfflineVideo
    override val channelId: String?
        get() = video.channelId
    override val channelLogin: String?
        get() = video.channelLogin
    override val channelName: String?
        get() = video.channelName
    override val channelImage: String?
        get() = video.channelLogo

    override val layoutId: Int
        get() = R.layout.fragment_player_offline
    override val chatContainerId: Int
        get() = R.id.dummyView

    override val shouldEnterPictureInPicture: Boolean
        get() = viewModel.playerMode.value == PlayerMode.NORMAL

    override val controllerShowTimeoutMs: Int = 5000

    override fun onCreate(savedInstanceState: Bundle?) {
        enableNetworkCheck = false
        super.onCreate(savedInstanceState)
        video = args.offlineVideo
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPlayerOfflineBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun initialize() {
        viewModel.setVideo(video)
        super.initialize()
        val settings = requireView().findViewById<ImageButton>(R.id.playerSettings)
        val playerMenu = requireView().findViewById<ImageButton>(R.id.playerMenu)
        val mode = requireView().findViewById<ImageButton>(R.id.playerMode)
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
                    vodGames = false
                ))
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

    override fun onNetworkRestored() {
        //do nothing
    }

    override fun onMovedToForeground() {
        viewModel.onResume()
    }

    override fun onMovedToBackground() {
        viewModel.onPause()
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

    fun startAudioOnly() {
        viewModel.startAudioOnly()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}