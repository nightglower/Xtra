package com.github.andreyasadchy.xtra.ui.clips.common

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.databinding.FragmentVideosListItemBinding
import com.github.andreyasadchy.xtra.model.helix.clip.Clip
import com.github.andreyasadchy.xtra.ui.clips.BaseClipsFragment
import com.github.andreyasadchy.xtra.ui.common.BasePagedListAdapter
import com.github.andreyasadchy.xtra.ui.games.GamesFragment
import com.github.andreyasadchy.xtra.util.TwitchApiHelper
import com.github.andreyasadchy.xtra.util.gone
import com.github.andreyasadchy.xtra.util.loadImage
import com.github.andreyasadchy.xtra.util.visible

class ChannelClipsAdapter(
    private val fragment: Fragment,
    private val clickListener: BaseClipsFragment.OnClipSelectedListener,
    private val gameClickListener: GamesFragment.OnGameSelectedListener,
    private val showDownloadDialog: (Clip) -> Unit) : BasePagedListAdapter<Clip, ChannelClipsAdapter.ViewHolder>(
    object : DiffUtil.ItemCallback<Clip>() {
        override fun areItemsTheSame(oldItem: Clip, newItem: Clip): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Clip, newItem: Clip): Boolean =
            oldItem.view_count == newItem.view_count &&
                    oldItem.title == newItem.title
    }) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(FragmentVideosListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: FragmentVideosListItemBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Clip?) {
            with(binding) {
                val context = fragment.requireContext()
                val gameListener: (View) -> Unit = { gameClickListener.openGame(item?.gameId, item?.gameName) }
                if (item != null) {
                    thumbnail.loadImage(fragment, item.thumbnail, diskCacheStrategy = DiskCacheStrategy.NONE)
                    root.setOnClickListener { clickListener.startClip(item) }
                    root.setOnLongClickListener { showDownloadDialog(item); true }
                    options.setOnClickListener { it ->
                        PopupMenu(context, it).apply {
                            inflate(R.menu.media_item)
                            setOnMenuItemClickListener {
                                when(it.itemId) {
                                    R.id.download -> showDownloadDialog(item)
                                    else -> menu.close()
                                }
                                true
                            }
                            show()
                        }
                    }
                }
                if (item?.uploadDate != null) {
                    val text = item.uploadDate?.let { TwitchApiHelper.formatTimeString(context, it) }
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
                if (item?.duration != null) {
                    duration.visible()
                    duration.text = DateUtils.formatElapsedTime(item.duration.toLong())
                } else {
                    duration.gone()
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