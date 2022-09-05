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
import com.github.andreyasadchy.xtra.model.helix.channel.ChannelSearch
import com.github.andreyasadchy.xtra.ui.channel.ChannelPagerFragmentDirections
import com.github.andreyasadchy.xtra.util.TwitchApiHelper
import com.github.andreyasadchy.xtra.util.gone
import com.github.andreyasadchy.xtra.util.loadImage
import com.github.andreyasadchy.xtra.util.visible

class ChannelSearchAdapter(
    private val fragment: Fragment) : PagingDataAdapter<ChannelSearch, ChannelSearchAdapter.PagingViewHolder>(
    object : DiffUtil.ItemCallback<ChannelSearch>() {
        override fun areItemsTheSame(oldItem: ChannelSearch, newItem: ChannelSearch): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ChannelSearch, newItem: ChannelSearch): Boolean = true
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
        fun bind(item: ChannelSearch?) {
            with(binding) {
                val context = fragment.requireContext()
                if (item != null) {
                    root.setOnClickListener { fragment.findNavController().navigate(ChannelPagerFragmentDirections.actionGlobalChannelPagerFragment(
                        channelId = item.id,
                        channelLogin = item.broadcaster_login,
                        channelName = item.display_name,
                        channelLogo = item.channelLogo
                    )) }
                }
                if (item?.channelLogo != null) {
                    userImage.visible()
                    userImage.loadImage(fragment, item.channelLogo, circle = true)
                } else {
                    userImage.gone()
                }
                if (item?.display_name != null) {
                    userName.visible()
                    userName.text = item.display_name
                } else {
                    userName.gone()
                }
                if (item?.followers_count != null) {
                    userFollowers.visible()
                    userFollowers.text = context.getString(R.string.followers, TwitchApiHelper.formatCount(context, item.followers_count))
                } else {
                    userFollowers.gone()
                }
                if (!item?.type.isNullOrBlank() || item?.is_live == true) {
                    typeText.visible()
                    if (item?.type == "rerun") {
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