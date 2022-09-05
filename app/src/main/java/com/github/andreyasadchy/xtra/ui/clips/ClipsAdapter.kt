package com.github.andreyasadchy.xtra.ui.clips

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.databinding.FragmentVideosListItemBinding
import com.github.andreyasadchy.xtra.model.helix.clip.Clip
import com.github.andreyasadchy.xtra.ui.channel.ChannelPagerFragmentDirections
import com.github.andreyasadchy.xtra.ui.clips.common.ClipsFragment
import com.github.andreyasadchy.xtra.ui.games.GameFragmentDirections
import com.github.andreyasadchy.xtra.util.TwitchApiHelper
import com.github.andreyasadchy.xtra.util.gone
import com.github.andreyasadchy.xtra.util.loadImage
import com.github.andreyasadchy.xtra.util.visible

class ClipsAdapter(
    private val fragment: Fragment,
    private val clickListener: ClipsFragment.OnClipSelectedListener,
    private val showDownloadDialog: (Clip) -> Unit) : PagingDataAdapter<Clip, ClipsAdapter.PagingViewHolder>(
    object : DiffUtil.ItemCallback<Clip>() {
        override fun areItemsTheSame(oldItem: Clip, newItem: Clip): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Clip, newItem: Clip): Boolean =
            oldItem.view_count == newItem.view_count &&
                    oldItem.title == newItem.title
    }) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagingViewHolder {
        val binding = FragmentVideosListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PagingViewHolder(binding, fragment, clickListener, showDownloadDialog)
    }

    override fun onBindViewHolder(holder: PagingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PagingViewHolder(
        private val binding: FragmentVideosListItemBinding,
        private val fragment: Fragment,
        private val clickListener: ClipsFragment.OnClipSelectedListener,
        private val showDownloadDialog: (Clip) -> Unit): RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Clip?) {
            with(binding) {
                val context = fragment.requireContext()
                val channelClickListener: (View) -> Unit = { fragment.findNavController().navigate(ChannelPagerFragmentDirections.actionGlobalChannelPagerFragment(
                    channelId = item?.broadcaster_id,
                    channelLogin = item?.broadcaster_login,
                    channelName = item?.broadcaster_name,
                    channelLogo = item?.channelLogo
                )) }
                val gameClickListener: (View) -> Unit = { fragment.findNavController().navigate(GameFragmentDirections.actionGlobalGameFragment(
                    gameId = item?.gameId,
                    gameName = item?.gameName
                )) }
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
                if (item?.channelLogo != null)  {
                    userImage.visible()
                    userImage.loadImage(fragment, item.channelLogo, circle = true)
                    userImage.setOnClickListener(channelClickListener)
                } else {
                    userImage.gone()
                }
                if (item?.broadcaster_name != null)  {
                    username.visible()
                    username.text = item.broadcaster_name
                    username.setOnClickListener(channelClickListener)
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
                    gameName.setOnClickListener(gameClickListener)
                } else {
                    gameName.gone()
                }
            }
        }
    }
}