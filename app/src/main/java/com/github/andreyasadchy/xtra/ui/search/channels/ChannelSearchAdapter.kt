package com.github.andreyasadchy.xtra.ui.search.channels

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.databinding.FragmentSearchChannelsListItemBinding
import com.github.andreyasadchy.xtra.model.ui.User
import com.github.andreyasadchy.xtra.ui.channel.ChannelPagerFragmentDirections
import com.github.andreyasadchy.xtra.util.TwitchApiHelper
import com.github.andreyasadchy.xtra.util.gone
import com.github.andreyasadchy.xtra.util.loadImage
import com.github.andreyasadchy.xtra.util.visible

class ChannelSearchAdapter(
    private val fragment: Fragment) : PagingDataAdapter<User, ChannelSearchAdapter.PagingViewHolder>(
    object : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean =
            oldItem.channelId == newItem.channelId

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean = true
    }) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagingViewHolder {
        val binding = FragmentSearchChannelsListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PagingViewHolder(binding, fragment)
    }

    override fun onBindViewHolder(holder: PagingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PagingViewHolder(
        private val binding: FragmentSearchChannelsListItemBinding,
        private val fragment: Fragment): RecyclerView.ViewHolder(binding.root) {
        fun bind(item: User?) {
            with(binding) {
                if (item != null) {
                    val context = fragment.requireContext()
                    root.setOnClickListener {
                        fragment.findNavController().navigate(ChannelPagerFragmentDirections.actionGlobalChannelPagerFragment(
                            channelId = item.channelId,
                            channelLogin = item.channelLogin,
                            channelName = item.channelName,
                            channelLogo = item.channelLogo,
                        ))
                    }
                    if (item.channelLogo != null) {
                        userImage.visible()
                        userImage.loadImage(fragment, item.channelLogo, circle = true)
                    } else {
                        userImage.gone()
                    }
                    if (item.channelName != null) {
                        userName.visible()
                        userName.text = item.channelName
                    } else {
                        userName.gone()
                    }
                    if (item.followersCount != null) {
                        userFollowers.visible()
                        userFollowers.text = context.getString(R.string.followers, TwitchApiHelper.formatCount(context, item.followersCount))
                    } else {
                        userFollowers.gone()
                    }
                    if (!item.type.isNullOrBlank() || item.isLive == true) {
                        typeText.visible()
                        if (item.type == "rerun") {
                            typeText.text = context.getString(R.string.video_type_rerun)
                        } else {
                            typeText.text = context.getString(R.string.live)
                        }
                    } else {
                        typeText.gone()
                    }
                }
            }
        }
    }
}