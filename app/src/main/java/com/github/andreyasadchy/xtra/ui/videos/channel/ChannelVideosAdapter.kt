package com.github.andreyasadchy.xtra.ui.videos.channel

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.databinding.FragmentVideosListItemBinding
import com.github.andreyasadchy.xtra.model.helix.video.Video
import com.github.andreyasadchy.xtra.ui.games.GamesFragment
import com.github.andreyasadchy.xtra.ui.videos.BaseVideosAdapter
import com.github.andreyasadchy.xtra.ui.videos.BaseVideosFragment
import com.github.andreyasadchy.xtra.util.*

class ChannelVideosAdapter(
    private val fragment: Fragment,
    private val clickListener: BaseVideosFragment.OnVideoSelectedListener,
    private val gameClickListener: GamesFragment.OnGameSelectedListener,
    private val showDownloadDialog: (Video) -> Unit,
    private val saveBookmark: (Video) -> Unit) : BaseVideosAdapter<Video, ChannelVideosAdapter.PagingViewHolder>(
    object : DiffUtil.ItemCallback<Video>() {
        override fun areItemsTheSame(oldItem: Video, newItem: Video): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Video, newItem: Video): Boolean =
            oldItem.view_count == newItem.view_count &&
                    oldItem.thumbnail_url == newItem.thumbnail_url &&
                    oldItem.title == newItem.title &&
                    oldItem.duration == newItem.duration
    }) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagingViewHolder {
        return PagingViewHolder(FragmentVideosListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: PagingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PagingViewHolder(private val binding: FragmentVideosListItemBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Video?) {
            with(binding) {
                val context = fragment.requireContext()
                val gameListener: (View) -> Unit = { gameClickListener.openGame(item?.gameId, item?.gameName) }
                val getDuration = item?.duration?.let { TwitchApiHelper.getDuration(it) }
                val position = item?.id?.let { positions?.get(it.toLong()) }
                if (item != null) {
                    thumbnail.loadImage(fragment, item.thumbnail, diskCacheStrategy = DiskCacheStrategy.NONE)
                    root.setOnClickListener { clickListener.startVideo(item, position?.toDouble()) }
                    root.setOnLongClickListener { showDownloadDialog(item); true }
                    options.setOnClickListener { it ->
                        PopupMenu(context, it).apply {
                            inflate(R.menu.media_item)
                            if (item.id.isNotBlank()) {
                                menu.findItem(R.id.bookmark).isVisible = true
                                if (bookmarks?.find { it.id == item.id } != null) {
                                    menu.findItem(R.id.bookmark).title = context.getString(R.string.remove_bookmark)
                                } else {
                                    menu.findItem(R.id.bookmark).title = context.getString(R.string.add_bookmark)
                                }
                            }
                            setOnMenuItemClickListener {
                                when(it.itemId) {
                                    R.id.download -> showDownloadDialog(item)
                                    R.id.bookmark -> saveBookmark(item)
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
                if (item?.view_count != null) {
                    views.visible()
                    views.text = TwitchApiHelper.formatViewsCount(context, item.view_count)
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
                if (!item?.tags.isNullOrEmpty() && context.prefs().getBoolean(C.UI_TAGS, true)) {
                    tagsLayout.removeAllViews()
                    tagsLayout.visible()
                    item?.tags?.forEach { tag ->
                        val text = TextView(context)
                        text.text = tag.name
                        if (tag.id != null) {
                            text.setOnClickListener { gameClickListener.openGame(tags = listOf(tag.id)) }
                        }
                        tagsLayout.addView(text)
                    }
                } else {
                    tagsLayout.gone()
                }
            }
        }
    }
}