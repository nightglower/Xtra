package com.github.andreyasadchy.xtra.ui.main

import android.app.ActivityManager
import android.content.*
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentTransaction
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.databinding.ActivityMainBinding
import com.github.andreyasadchy.xtra.model.User
import com.github.andreyasadchy.xtra.model.helix.clip.Clip
import com.github.andreyasadchy.xtra.model.helix.stream.Stream
import com.github.andreyasadchy.xtra.model.helix.video.Video
import com.github.andreyasadchy.xtra.model.offline.OfflineVideo
import com.github.andreyasadchy.xtra.ui.channel.ChannelPagerFragmentDirections
import com.github.andreyasadchy.xtra.ui.clips.common.ClipsFragment
import com.github.andreyasadchy.xtra.ui.games.GameFragmentDirections
import com.github.andreyasadchy.xtra.ui.player.BasePlayerFragment
import com.github.andreyasadchy.xtra.ui.saved.downloads.DownloadsFragment
import com.github.andreyasadchy.xtra.ui.streams.common.StreamsFragment
import com.github.andreyasadchy.xtra.ui.videos.BaseVideosFragment
import com.github.andreyasadchy.xtra.ui.view.SlidingLayout
import com.github.andreyasadchy.xtra.util.*
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : AppCompatActivity(), StreamsFragment.OnStreamSelectedListener, ClipsFragment.OnClipSelectedListener, BaseVideosFragment.OnVideoSelectedListener, DownloadsFragment.OnVideoSelectedListener, SlidingLayout.Listener {

    companion object {
        const val KEY_CODE = "code"
        const val KEY_VIDEO = "video"

        const val INTENT_OPEN_DOWNLOADS_TAB = 0
        const val INTENT_OPEN_DOWNLOADED_VIDEO = 1
        const val INTENT_OPEN_PLAYER = 2
    }

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var navController: NavController
    var playerFragment: BasePlayerFragment? = null
        private set
    private val networkReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            viewModel.setNetworkAvailable(isNetworkAvailable)
        }
    }
    private lateinit var prefs: SharedPreferences

    //Lifecycle methods

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = prefs()
        if (prefs.getBoolean(C.FIRST_LAUNCH2, true)) {
            PreferenceManager.setDefaultValues(this@MainActivity, R.xml.root_preferences, false)
            PreferenceManager.setDefaultValues(this@MainActivity, R.xml.player_button_preferences, true)
            PreferenceManager.setDefaultValues(this@MainActivity, R.xml.player_menu_preferences, true)
            PreferenceManager.setDefaultValues(this@MainActivity, R.xml.buffer_preferences, true)
            PreferenceManager.setDefaultValues(this@MainActivity, R.xml.token_preferences, true)
            PreferenceManager.setDefaultValues(this@MainActivity, R.xml.api_token_preferences, true)
            prefs.edit {
                putBoolean(C.FIRST_LAUNCH2, false)
                putInt(C.LANDSCAPE_CHAT_WIDTH, DisplayUtils.calculateLandscapeWidthByPercent(this@MainActivity, 30))
                if (resources.getBoolean(R.bool.isTablet)) {
                    putString(C.PORTRAIT_COLUMN_COUNT, "2")
                    putString(C.LANDSCAPE_COLUMN_COUNT, "3")
                }
            }
        }
        if (prefs.getBoolean(C.FIRST_LAUNCH, true)) {
            prefs.edit {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                    putString(C.CHAT_IMAGE_LIBRARY, "2")
                }
                putBoolean(C.FIRST_LAUNCH, false)
            }
        }
        if (prefs.getBoolean(C.FIRST_LAUNCH1, true)) {
            prefs.edit {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || !packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
                    putString(C.PLAYER_BACKGROUND_PLAYBACK, "1")
                } else {
                    putString(C.PLAYER_BACKGROUND_PLAYBACK, "0")
                }
                putBoolean(C.FIRST_LAUNCH1, false)
            }
        }
        applyTheme()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val notInitialized = savedInstanceState == null
        initNavigation()
        var flag = notInitialized && !isNetworkAvailable
        viewModel.isNetworkAvailable.observe(this) {
            it.getContentIfNotHandled()?.let { online ->
                if (online) {
                    if (prefs.getBoolean(C.VALIDATE_TOKENS, true)) {
                        viewModel.validate(prefs.getString(C.HELIX_CLIENT_ID, ""), prefs.getString(C.GQL_CLIENT_ID, ""), this)
                    }
                }
                if (flag) {
                    shortToast(if (online) R.string.connection_restored else R.string.no_connection)
                } else {
                    flag = true
                }
            }
        }
        registerReceiver(networkReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        restorePlayerFragment()
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        restorePlayerFragment()
    }

    override fun onDestroy() {
        unregisterReceiver(networkReceiver)
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    /**
     * Result of LoginActivity
     */
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        fun restartActivity() {
            finish()
            overridePendingTransition(0, 0)
            startActivity(Intent(this, MainActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION) })
            overridePendingTransition(0, 0)
        }
        when (requestCode) {
            1 -> { //Was not logged in
                when (resultCode) {//Logged in
                    RESULT_OK -> restartActivity()
                }
            }
            2 -> restartActivity() //Was logged in
        }
    }

    override fun onBackPressed() {
        if (!viewModel.isPlayerMaximized) {
            super.onBackPressed()
/*            val currentFragment = supportFragmentManager.findFragmentById(R.id.navHostFragment)?.childFragmentManager?.fragments?.getOrNull(0)
            if (currentFragment !is ChannelPagerFragment || (currentFragment.currentFragment.let { it !is ChatFragment || !it.hideEmotesMenu() })) {
                if (navController.previousBackStackEntry?.destination?.id == navController.graph.startDestinationId) {
                    binding.navBar.selectedItemId = navController.graph.startDestinationId
                } else {
                    super.onBackPressed()
                }
            }*/
        } else {
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            0 -> {
            }
        }
    }

    private fun isBackgroundRunning(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return false
        } else {
            val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
            val runningProcesses = am.runningAppProcesses
            for (processInfo in runningProcesses) {
                if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    for (activeProcess in processInfo.pkgList) {
                        if (activeProcess == packageName) {
                            //If your app is the process in foreground, then it's not in running in background
                            return false
                        }
                    }
                }
            }
            return true
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val url = intent.data.toString()
            when {
                url.contains("twitch.tv/videos/") -> {
                    val id = url.substringAfter("twitch.tv/videos/").takeIf { it.isNotBlank() }?.let { it.substringBefore("?", it.substringBefore("/")) }
                    val offset = url.substringAfter("?t=").takeIf { it.isNotBlank() }?.let { (TwitchApiHelper.getDuration(it)?.toDouble() ?: 0.0) * 1000.0 }
                    if (!id.isNullOrBlank()) {
                        viewModel.loadVideo(id, prefs.getString(C.HELIX_CLIENT_ID, ""), User.get(this).helixToken, prefs.getString(C.GQL_CLIENT_ID, ""))
                        viewModel.video.observe(this) { video ->
                            if (video != null && video.id.isNotBlank()) {
                                startVideo(video, offset)
                            }
                        }
                    }
                }
                url.contains("/clip/") -> {
                    val id = url.substringAfter("/clip/").takeIf { it.isNotBlank() }?.let { it.substringBefore("?", it.substringBefore("/")) }
                    if (!id.isNullOrBlank()) {
                        viewModel.loadClip(id, prefs.getString(C.HELIX_CLIENT_ID, ""), User.get(this).helixToken, prefs.getString(C.GQL_CLIENT_ID, ""))
                        viewModel.clip.observe(this) { clip ->
                            if (clip != null && clip.id.isNotBlank()) {
                                startClip(clip)
                            }
                        }
                    }
                }
                url.contains("clips.twitch.tv/") -> {
                    val id = url.substringAfter("clips.twitch.tv/").takeIf { it.isNotBlank() }?.let { it.substringBefore("?", it.substringBefore("/")) }
                    if (!id.isNullOrBlank()) {
                        viewModel.loadClip(id, prefs.getString(C.HELIX_CLIENT_ID, ""), User.get(this).helixToken, prefs.getString(C.GQL_CLIENT_ID, ""))
                        viewModel.clip.observe(this) { clip ->
                            if (clip != null && clip.id.isNotBlank()) {
                                startClip(clip)
                            }
                        }
                    }
                }
                url.contains("twitch.tv/directory/game/") -> {
                    val name = url.substringAfter("twitch.tv/directory/game/").takeIf { it.isNotBlank() }?.let { it.substringBefore("?", it.substringBefore("/")) }
                    if (!name.isNullOrBlank()) {
                        playerFragment?.minimize()
                        navController.navigate(GameFragmentDirections.actionGlobalGameFragment(
                            gameName = Uri.decode(name),
                        ))
                    }
                }
                else -> {
                    val login = url.substringAfter("twitch.tv/").takeIf { it.isNotBlank() }?.let { it.substringBefore("?", it.substringBefore("/")) }
                    if (!login.isNullOrBlank()) {
                        viewModel.loadUser(login, prefs.getString(C.HELIX_CLIENT_ID, ""), User.get(this).helixToken, prefs.getString(C.GQL_CLIENT_ID, ""))
                        viewModel.user.observe(this) { user ->
                            if (user != null && (!user.id.isNullOrBlank() || !user.login.isNullOrBlank())) {
                                playerFragment?.minimize()
                                navController.navigate(ChannelPagerFragmentDirections.actionGlobalChannelPagerFragment(
                                    channelId = user.id,
                                    channelLogin = user.login,
                                    channelName = user.display_name,
                                    channelLogo = user.channelLogo,
                                ))
                            }
                        }
                    }
                }
            }
        } else {
            when (intent?.getIntExtra(KEY_CODE, -1)) {
                INTENT_OPEN_DOWNLOADS_TAB -> binding.navBar.selectedItemId = R.id.savedFragment
                INTENT_OPEN_DOWNLOADED_VIDEO -> startOfflineVideo(intent.getParcelableExtra(KEY_VIDEO)!!)
                INTENT_OPEN_PLAYER -> playerFragment?.maximize() //TODO if was closed need to reopen
            }
        }
    }

//Navigation listeners

    override fun startStream(stream: Stream) {
        //startPlayer(StreamPlayerFragment.newInstance(stream))
    }

    override fun startVideo(video: Video, offset: Double?) {
        //startPlayer(VideoPlayerFragment.newInstance(video, offset))
    }

    override fun startClip(clip: Clip) {
        //startPlayer(ClipPlayerFragment.newInstance(clip))
    }

    override fun startOfflineVideo(video: OfflineVideo) {
        //startPlayer(OfflinePlayerFragment.newInstance(video))
    }

//SlidingLayout.Listener

    override fun onMaximize() {
        viewModel.onMaximize()
    }

    override fun onMinimize() {
        viewModel.onMinimize()
    }

    override fun onClose() {
        closePlayer()
    }

//Player methods

    private fun startPlayer(fragment: BasePlayerFragment) {
//        if (playerFragment == null) {
        playerFragment = fragment
        supportFragmentManager.beginTransaction()
                .replace(R.id.playerContainer, fragment).commit()
        viewModel.onPlayerStarted()
    }

    fun closePlayer() {
        supportFragmentManager.beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .remove(supportFragmentManager.findFragmentById(R.id.playerContainer)!!)
                .commit()
        playerFragment = null
        viewModel.onPlayerClosed()
    }

    private fun restorePlayerFragment() {
        if (viewModel.isPlayerOpened) {
            if (playerFragment == null) {
                playerFragment = supportFragmentManager.findFragmentById(R.id.playerContainer) as BasePlayerFragment?
            } else {
                if (playerFragment?.slidingLayout?.secondView?.isVisible == false && prefs.getString(C.PLAYER_BACKGROUND_PLAYBACK, "0") == "0") {
                    playerFragment?.maximize()
                }
            }
        }
    }

    fun popFragment() {
        onBackPressed()
    }

    private fun initNavigation() {
        navController = (supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment).navController
        binding.navBar.setupWithNavController(navController)
/*        binding.navBar.apply {
            setOnItemSelectedListener {
                NavigationUI.onNavDestinationSelected(it, navController)
                return@setOnItemSelectedListener true
            }
            setOnItemReselectedListener {
                val currentFragment = supportFragmentManager.findFragmentById(R.id.navHostFragment)?.childFragmentManager?.fragments?.getOrNull(0)
                when {
                    it.itemId == R.id.gamesFragment && currentFragment is GamesFragment && currentFragment.arguments?.getStringArray(C.TAGS).isNullOrEmpty() -> currentFragment.scrollToTop()
                    it.itemId == R.id.topFragment && currentFragment is TopFragment -> currentFragment.scrollToTop()
                    it.itemId == R.id.followMediaFragment && currentFragment is FollowMediaFragment -> currentFragment.scrollToTop()
                    it.itemId == R.id.savedMediaFragment && currentFragment is SavedMediaFragment -> currentFragment.scrollToTop()
                    else -> navController.navigate(it.itemId, null, NavOptions.Builder().setPopUpTo(it.itemId, true).build())
                }
            }
        }*/
    }
}
