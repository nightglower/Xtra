package com.github.andreyasadchy.xtra.ui.saved.downloads

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.databinding.FragmentDownloadsListItemBinding
import com.github.andreyasadchy.xtra.model.offline.OfflineVideo
import com.github.andreyasadchy.xtra.ui.channel.ChannelPagerFragmentDirections
import com.github.andreyasadchy.xtra.ui.games.GameFragmentDirections
import com.github.andreyasadchy.xtra.util.*

class DownloadsAdapter(
    private val fragment: Fragment,
    private val clickListener: DownloadsFragment.OnVideoSelectedListener,
    private val deleteVideo: (OfflineVideo) -> Unit) : ListAdapter<OfflineVideo, DownloadsAdapter.ViewHolder>(
    object : DiffUtil.ItemCallback<OfflineVideo>() {
        override fun areItemsTheSame(oldItem: OfflineVideo, newItem: OfflineVideo): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: OfflineVideo, newItem: OfflineVideo): Boolean {
            return false //bug, oldItem and newItem are sometimes the same
        }
    }) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = FragmentDownloadsListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, fragment, clickListener, deleteVideo)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: FragmentDownloadsListItemBinding,
        private val fragment: Fragment,
        private val clickListener: DownloadsFragment.OnVideoSelectedListener,
        private val deleteVideo: (OfflineVideo) -> Unit): RecyclerView.ViewHolder(binding.root) {
        fun bind(item: OfflineVideo?) {
            with(binding) {
                val context = fragment.requireContext()
                val channelClickListener: (View) -> Unit = { fragment.findNavController().navigate(ChannelPagerFragmentDirections.actionGlobalChannelPagerFragment(
                    channelId = item?.channelId,
                    channelLogin = item?.channelLogin,
                    channelName = item?.channelName,
                    channelLogo = item?.channelLogo,
                    updateLocal = true
                )) }
                val gameClickListener: (View) -> Unit = { fragment.findNavController().navigate(GameFragmentDirections.actionGlobalGameFragment(
                    gameId = item?.gameId,
                    gameName = item?.gameName
                )) }
                if (item != null) {
                    thumbnail.loadImage(fragment, item.thumbnail, diskCacheStrategy = DiskCacheStrategy.NONE)
                    root.setOnClickListener { clickListener.startOfflineVideo(item) }
                    root.setOnLongClickListener { deleteVideo(item); true }
                    options.setOnClickListener { it ->
                        PopupMenu(context, it).apply {
                            inflate(R.menu.offline_item)
                            setOnMenuItemClickListener {
                                when(it.itemId) {
                                    R.id.delete -> deleteVideo(item)
                                    else -> menu.close()
                                }
                                true
                            }
                            show()
                        }
                    }
                    status.apply {
                        if (item.status == OfflineVideo.STATUS_DOWNLOADED) {
                            gone()
                        } else {
                            text = if (item.status == OfflineVideo.STATUS_DOWNLOADING) {
                                context.getString(R.string.downloading_progress, ((item.progress.toFloat() / item.maxProgress) * 100f).toInt())
                            } else {
                                context.getString(R.string.download_pending)
                            }
                            visible()
                            setOnClickListener { deleteVideo(item) }
                            setOnLongClickListener { deleteVideo(item); true }
                        }
                    }
                }
                if (item?.uploadDate != null) {
                    date.visible()
                    date.text = context.getString(R.string.uploaded_date, TwitchApiHelper.formatTime(context, item.uploadDate))
                } else {
                    date.gone()
                }
                if (item?.downloadDate != null) {
                    downloadDate.visible()
                    downloadDate.text = context.getString(R.string.downloaded_date, TwitchApiHelper.formatTime(context, item.downloadDate))
                } else {
                    downloadDate.gone()
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
                if (item?.channelLogo != null)  {
                    userImage.visible()
                    userImage.loadImage(fragment, item.channelLogo, circle = true, diskCacheStrategy = DiskCacheStrategy.NONE)
                    userImage.setOnClickListener(channelClickListener)
                } else {
                    userImage.gone()
                }
                if (item?.channelName != null)  {
                    username.visible()
                    username.text = item.channelName
                    username.setOnClickListener(channelClickListener)
                } else {
                    username.gone()
                }
                if (item?.name != null && item.name != "")  {
                    title.visible()
                    title.text = item.name.trim()
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
                if (item?.duration != null) {
                    duration.visible()
                    duration.text = DateUtils.formatElapsedTime(item.duration / 1000L)
                    if (item.sourceStartPosition != null)  {
                        sourceStart.visible()
                        sourceStart.text = context.getString(R.string.source_vod_start, DateUtils.formatElapsedTime(item.sourceStartPosition / 1000L))
                        sourceEnd.visible()
                        sourceEnd.text = context.getString(R.string.source_vod_end, DateUtils.formatElapsedTime((item.sourceStartPosition + item.duration) / 1000L))
                    } else {
                        sourceStart.gone()
                        sourceEnd.gone()
                    }
                    if (context.prefs().getBoolean(C.PLAYER_USE_VIDEOPOSITIONS, true) && item.lastWatchPosition != null && item.duration > 0L) {
                        progressBar.progress = (item.lastWatchPosition!!.toFloat() / item.duration * 100).toInt()
                        progressBar.visible()
                    } else {
                        progressBar.gone()
                    }
                } else {
                    duration.gone()
                    sourceStart.gone()
                    sourceEnd.gone()
                    progressBar.gone()
                }
                if (sourceEnd.isVisible && sourceStart.isVisible) {
                    val params = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    params.setMargins(0, context.convertDpToPixels(5F), 0, 0)
                    sourceEnd.layoutParams = params
                }
                if (type.isVisible && (sourceStart.isVisible || sourceEnd.isVisible)) {
                    val params = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    params.setMargins(0, context.convertDpToPixels(5F), 0, 0)
                    type.layoutParams = params
                }
            }
        }
    }
}