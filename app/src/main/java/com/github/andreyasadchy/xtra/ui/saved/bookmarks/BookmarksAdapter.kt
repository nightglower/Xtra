package com.github.andreyasadchy.xtra.ui.saved.bookmarks

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.databinding.FragmentVideosListItemBinding
import com.github.andreyasadchy.xtra.model.helix.video.Video
import com.github.andreyasadchy.xtra.model.offline.Bookmark
import com.github.andreyasadchy.xtra.model.offline.VodBookmarkIgnoredUser
import com.github.andreyasadchy.xtra.ui.common.OnChannelSelectedListener
import com.github.andreyasadchy.xtra.ui.games.GamesFragment
import com.github.andreyasadchy.xtra.ui.videos.BaseVideosFragment
import com.github.andreyasadchy.xtra.util.*

class BookmarksAdapter(
    private val fragment: Fragment,
    private val clickListener: BaseVideosFragment.OnVideoSelectedListener,
    private val channelClickListener: OnChannelSelectedListener,
    private val gameClickListener: GamesFragment.OnGameSelectedListener,
    private val refreshVideo: (String) -> Unit,
    private val showDownloadDialog: (Video) -> Unit,
    private val vodIgnoreUser: (String) -> Unit,
    private val deleteVideo: (Bookmark) -> Unit) : ListAdapter<Bookmark, BookmarksAdapter.ViewHolder>(
    object : DiffUtil.ItemCallback<Bookmark>() {
        override fun areItemsTheSame(oldItem: Bookmark, newItem: Bookmark): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Bookmark, newItem: Bookmark): Boolean =
            oldItem.title == newItem.title &&
                    oldItem.duration == newItem.duration
    }) {

    private var positions: Map<Long, Long>? = null

    fun setVideoPositions(positions: Map<Long, Long>) {
        this.positions = positions
        if (itemCount != 0) {
            notifyDataSetChanged()
        }
    }

    private var ignored: List<VodBookmarkIgnoredUser>? = null

    fun setIgnoredUsers(list: List<VodBookmarkIgnoredUser>) {
        this.ignored = list
        if (itemCount != 0) {
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(FragmentVideosListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: FragmentVideosListItemBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Bookmark?) {
            with(binding) {
                val context = fragment.requireContext()
                val channelListener: (View) -> Unit = { channelClickListener.viewChannel(item?.userId, item?.userLogin, item?.userName, item?.userLogo, updateLocal = true) }
                val gameListener: (View) -> Unit = { gameClickListener.openGame(item?.gameId, item?.gameName) }
                val getDuration = item?.duration?.let { TwitchApiHelper.getDuration(it) }
                val position = item?.id?.let { positions?.get(it.toLong()) }
                val ignore = ignored?.find { it.user_id == item?.userId } != null
                val userType = item?.userType ?: item?.userBroadcasterType
                if (item != null) {
                    thumbnail.loadImage(fragment, item.thumbnail, diskCacheStrategy = DiskCacheStrategy.NONE)
                    root.setOnClickListener { clickListener.startVideo(Video(
                        id = item.id,
                        user_id = item.userId,
                        user_login = item.userLogin,
                        user_name = item.userName,
                        profileImageURL = item.userLogo,
                        gameId = item.gameId,
                        gameName = item.gameName,
                        title = item.title,
                        createdAt = item.createdAt,
                        thumbnail_url = item.thumbnail,
                        type = item.type,
                        duration = item.duration,
                    ), position?.toDouble()) }
                    root.setOnLongClickListener { deleteVideo(item); true }
                    options.setOnClickListener { it ->
                        PopupMenu(context, it).apply {
                            inflate(R.menu.offline_item)
                            if (item.id.isNotBlank()) {
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
                                        id = item.id,
                                        user_id = item.userId,
                                        user_login = item.userLogin,
                                        user_name = item.userName,
                                        profileImageURL = item.userLogo,
                                        gameId = item.gameId,
                                        gameName = item.gameName,
                                        title = item.title,
                                        createdAt = item.createdAt,
                                        thumbnail_url = item.thumbnail,
                                        type = item.type,
                                        duration = item.duration,
                                    ))
                                    R.id.vodIgnore -> item.userId?.let { id -> vodIgnoreUser(id) }
                                    R.id.refresh -> refreshVideo(item.id)
                                    else -> menu.close()
                                }
                                true
                            }
                            show()
                        }
                    }
                }
                if (item?.createdAt != null) {
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
                if (item?.type?.lowercase() == "archive" && userType != null && item.createdAt != null && context.prefs().getBoolean(C.UI_BOOKMARK_TIME_LEFT, true) && !ignore) {
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
                if (item?.type != null) {
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
                if (position != null && getDuration != null && getDuration > 0L) {
                    progressBar.progress = (position / (getDuration * 10)).toInt()
                    progressBar.visible()
                } else {
                    progressBar.gone()
                }
                if (item?.userLogo != null)  {
                    userImage.visible()
                    userImage.loadImage(fragment, item.userLogo, circle = true, diskCacheStrategy = DiskCacheStrategy.NONE)
                    userImage.setOnClickListener(channelListener)
                } else {
                    userImage.gone()
                }
                if (item?.userName != null)  {
                    username.visible()
                    username.text = item.userName
                    username.setOnClickListener(channelListener)
                } else {
                    username.gone()
                }
                if (item?.title != null && item.title != "")  {
                    title.visible()
                    title.text = item.title.trim()
                } else {
                    title.gone()
                }
                if (item?.gameName != null)  {
                    gameName.visible()
                    gameName.text = item.gameName
                    gameName.setOnClickListener(gameListener)
                } else {
                    gameName.gone()
                }
            }
        }
    }
}