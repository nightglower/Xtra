package com.github.andreyasadchy.xtra.ui.follow.channels

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.databinding.FragmentFollowedChannelsListItemBinding
import com.github.andreyasadchy.xtra.model.helix.follows.Follow
import com.github.andreyasadchy.xtra.ui.common.BasePagedListAdapter
import com.github.andreyasadchy.xtra.ui.common.OnChannelSelectedListener
import com.github.andreyasadchy.xtra.util.TwitchApiHelper
import com.github.andreyasadchy.xtra.util.gone
import com.github.andreyasadchy.xtra.util.loadImage
import com.github.andreyasadchy.xtra.util.visible

class FollowedChannelsAdapter(
    private val fragment: Fragment,
    private val listener: OnChannelSelectedListener) : BasePagedListAdapter<Follow, FollowedChannelsAdapter.ViewHolder>(
    object : DiffUtil.ItemCallback<Follow>() {
        override fun areItemsTheSame(oldItem: Follow, newItem: Follow): Boolean =
            oldItem.to_id == newItem.to_id

        override fun areContentsTheSame(oldItem: Follow, newItem: Follow): Boolean = true
    }) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(FragmentFollowedChannelsListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: FragmentFollowedChannelsListItemBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Follow?) {
            with(binding) {
                val context = fragment.requireContext()
                if (item != null) {
                    root.setOnClickListener { listener.viewChannel(item.to_id, item.to_login, item.to_name, item.channelLogo, item.followLocal) }
                }
                if (item?.channelLogo != null) {
                    userImage.visible()
                    userImage.loadImage(fragment, item.channelLogo, circle = true, diskCacheStrategy = DiskCacheStrategy.NONE)
                } else {
                    userImage.gone()
                }
                if (item?.to_name != null) {
                    username.visible()
                    username.text = item.to_name
                } else {
                    username.gone()
                }
                if (item?.lastBroadcast != null) {
                    val text = item.lastBroadcast?.let { TwitchApiHelper.formatTimeString(context, it) }
                    if (text != null) {
                        userStream.visible()
                        userStream.text = context.getString(R.string.last_broadcast_date, text)
                    } else {
                        userStream.gone()
                    }
                } else {
                    userStream.gone()
                }
                if (item?.followed_at != null) {
                    val text = TwitchApiHelper.formatTimeString(context, item.followed_at!!)
                    if (text != null) {
                        userFollowed.visible()
                        userFollowed.text = context.getString(R.string.followed_at, text)
                    } else {
                        userFollowed.gone()
                    }
                } else {
                    userFollowed.gone()
                }
                if (item?.followTwitch == true) {
                    twitchText.visible()
                } else {
                    twitchText.gone()
                }
                if (item?.followLocal == true) {
                    localText.visible()
                } else {
                    localText.gone()
                }
            }
        }
    }
}