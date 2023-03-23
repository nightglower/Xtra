package com.github.andreyasadchy.xtra.ui.saved.bookmarks

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.databinding.FragmentVideosListItemBinding
import com.github.andreyasadchy.xtra.model.offline.Bookmark
import com.github.andreyasadchy.xtra.model.offline.VodBookmarkIgnoredUser
import com.github.andreyasadchy.xtra.model.ui.Video
import com.github.andreyasadchy.xtra.ui.channel.ChannelPagerFragmentDirections
import com.github.andreyasadchy.xtra.ui.games.GameMediaFragmentDirections
import com.github.andreyasadchy.xtra.ui.games.GamePagerFragmentDirections
import com.github.andreyasadchy.xtra.ui.main.MainActivity
import com.github.andreyasadchy.xtra.util.*

class BookmarksAdapter(
    private val fragment: Fragment,
    private val refreshVideo: (String?) -> Unit,
    private val showDownloadDialog: (Video) -> Unit,
    private val vodIgnoreUser: (String) -> Unit,
    private val deleteVideo: (Bookmark) -> Unit) : ListAdapter<Bookmark, BookmarksAdapter.PagingViewHolder>(
    object : DiffUtil.ItemCallback<Bookmark>() {
        override fun areItemsTheSame(oldItem: Bookmark, newItem: Bookmark): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Bookmark, newItem: Bookmark): Boolean =
            oldItem.title == newItem.title &&
                    oldItem.duration == newItem.duration
    }) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagingViewHolder {
        val binding = FragmentVideosListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PagingViewHolder(binding, fragment)
    }

    override fun onBindViewHolder(holder: PagingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private var positions: Map<Long, Long>? = null

    fun setVideoPositions(positions: Map<Long, Long>) {
        this.positions = positions
        if (!currentList.isNullOrEmpty()) {
            notifyDataSetChanged()
        }
    }

    private var ignored: List<VodBookmarkIgnoredUser>? = null

    fun setIgnoredUsers(list: List<VodBookmarkIgnoredUser>) {
        this.ignored = list
        if (!currentList.isNullOrEmpty()) {
            notifyDataSetChanged()
        }
    }

    inner class PagingViewHolder(
        private val binding: FragmentVideosListItemBinding,
        private val fragment: Fragment): RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Bookmark?) {
            with(binding) {
                if (item != null) {
                    val context = fragment.requireContext()
                    val channelListener: (View) -> Unit = { fragment.findNavController().navigate(ChannelPagerFragmentDirections.actionGlobalChannelPagerFragment(
                        channelId = item.userId,
                        channelLogin = item.userLogin,
                        channelName = item.userName,
                        channelLogo = item.userLogo,
                        updateLocal = true
                    )) }
                    val gameListener: (View) -> Unit = {
                        fragment.findNavController().navigate(
                            if (context.prefs().getBoolean(C.UI_GAMEPAGER, true)) {
                                GamePagerFragmentDirections.actionGlobalGamePagerFragment(
                                    gameId = item.gameId,
                                    gameName = item.gameName
                                )
                            } else {
                                GameMediaFragmentDirections.actionGlobalGameMediaFragment(
                                    gameId = item.gameId,
                                    gameName = item.gameName
                                )
                            }
                        )
                    }
                    val getDuration = item.duration?.let { TwitchApiHelper.getDuration(it) }
                    val position = item.videoId?.toLongOrNull()?.let { positions?.get(it) }
                    val ignore = ignored?.find { it.user_id == item.userId } != null
                    val userType = item.userType ?: item.userBroadcasterType
                    root.setOnClickListener {
                        (fragment.activity as MainActivity).startVideo(Video(
                            id = item.videoId,
                            channelId = item.userId,
                            channelLogin = item.userLogin,
                            channelName = item.userName,
                            profileImageUrl = item.userLogo,
                            gameId = item.gameId,
                            gameName = item.gameName,
                            title = item.title,
                            uploadDate = item.createdAt,
                            thumbnailUrl = item.thumbnail,
                            type = item.type,
                            duration = item.duration,
                            animatedPreviewURL = item.animatedPreviewURL,
                        ), position?.toDouble())
                    }
                    root.setOnLongClickListener { deleteVideo(item); true }
                    thumbnail.loadImage(fragment, item.thumbnail, diskCacheStrategy = DiskCacheStrategy.NONE)
                    if (item.createdAt != null) {
                        val text = TwitchApiHelper.formatTimeString(context, item.createdAt)
                        if (text != null) {
                            date.visible()
                            date.text = text
                        } else {
                            date.gone()
                        }
                    } else {
                        date.gone()
                    }
                    if (item.type?.lowercase() == "archive" && userType != null && item.createdAt != null && context.prefs().getBoolean(C.UI_BOOKMARK_TIME_LEFT, true) && !ignore) {
                        val time = TwitchApiHelper.getVodTimeLeft(context, item.createdAt,
                            when (userType.lowercase()) {
                                "" -> 14
                                "affiliate" -> 14
                                else -> 60
                            })
                        if (!time.isNullOrBlank()) {
                            views.visible()
                            views.text = context.getString(R.string.vod_time_left, time)
                        } else {
                            views.gone()
                        }
                    } else {
                        views.gone()
                    }
                    if (getDuration != null) {
                        duration.visible()
                        duration.text = DateUtils.formatElapsedTime(getDuration)
                    } else {
                        duration.gone()
                    }
                    if (item.type != null) {
                        val text = TwitchApiHelper.getType(context, item.type)
                        if (text != null) {
                            type.visible()
                            type.text = text
                        } else {
                            type.gone()
                        }
                    } else {
                        type.gone()
                    }
                    if (item.userLogo != null)  {
                        userImage.visible()
                        userImage.loadImage(fragment, item.userLogo, circle = true, diskCacheStrategy = DiskCacheStrategy.NONE)
                        userImage.setOnClickListener(channelListener)
                    } else {
                        userImage.gone()
                    }
                    if (item.userName != null)  {
                        username.visible()
                        username.text = item.userName
                        username.setOnClickListener(channelListener)
                    } else {
                        username.gone()
                    }
                    if (position != null && getDuration != null && getDuration > 0L) {
                        progressBar.progress = (position / (getDuration * 10)).toInt()
                        progressBar.visible()
                    } else {
                        progressBar.gone()
                    }
                    if (item.title != null)  {
                        title.visible()
                        title.text = item.title.trim()
                    } else {
                        title.gone()
                    }
                    if (item.gameName != null)  {
                        gameName.visible()
                        gameName.text = item.gameName
                        gameName.setOnClickListener(gameListener)
                    } else {
                        gameName.gone()
                    }
                    options.setOnClickListener { it ->
                        PopupMenu(context, it).apply {
                            inflate(R.menu.offline_item)
                            if (!item.videoId.isNullOrBlank()) {
                                menu.findItem(R.id.refresh).isVisible = true
                                menu.findItem(R.id.download).isVisible = true
                            }
                            if (item.type?.lowercase() == "archive" && item.userId != null && context.prefs().getBoolean(C.UI_BOOKMARK_TIME_LEFT, true)) {
                                menu.findItem(R.id.vodIgnore).isVisible = true
                                if (ignore) {
                                    menu.findItem(R.id.vodIgnore).title = context.getString(R.string.vod_remove_ignore)
                                } else {
                                    menu.findItem(R.id.vodIgnore).title = context.getString(R.string.vod_ignore_user)
                                }
                            }
                            setOnMenuItemClickListener {
                                when(it.itemId) {
                                    R.id.delete -> deleteVideo(item)
                                    R.id.download -> showDownloadDialog(Video(
                                        id = item.videoId,
                                        channelId = item.userId,
                                        channelLogin = item.userLogin,
                                        channelName = item.userName,
                                        profileImageUrl = item.userLogo,
                                        gameId = item.gameId,
                                        gameName = item.gameName,
                                        title = item.title,
                                        uploadDate = item.createdAt,
                                        thumbnailUrl = item.thumbnail,
                                        type = item.type,
                                        duration = item.duration,
                                        animatedPreviewURL = item.animatedPreviewURL,
                                    ))
                                    R.id.vodIgnore -> item.userId?.let { id -> vodIgnoreUser(id) }
                                    R.id.refresh -> refreshVideo(item.videoId)
                                    else -> menu.close()
                                }
                                true
                            }
                            show()
                        }
                    }
                }
            }
        }
    }
}