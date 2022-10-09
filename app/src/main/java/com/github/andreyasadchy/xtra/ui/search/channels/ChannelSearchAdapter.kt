package com.github.andreyasadchy.xtra.ui.search.channels

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.databinding.FragmentSearchChannelsListItemBinding
import com.github.andreyasadchy.xtra.model.helix.channel.ChannelSearch
import com.github.andreyasadchy.xtra.ui.common.BasePagedListAdapter
import com.github.andreyasadchy.xtra.ui.common.OnChannelSelectedListener
import com.github.andreyasadchy.xtra.util.TwitchApiHelper
import com.github.andreyasadchy.xtra.util.gone
import com.github.andreyasadchy.xtra.util.loadImage
import com.github.andreyasadchy.xtra.util.visible

class ChannelSearchAdapter(
    private val fragment: Fragment,
    private val listener: OnChannelSelectedListener) : BasePagedListAdapter<ChannelSearch, ChannelSearchAdapter.ViewHolder>(
    object : DiffUtil.ItemCallback<ChannelSearch>() {
        override fun areItemsTheSame(oldItem: ChannelSearch, newItem: ChannelSearch): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ChannelSearch, newItem: ChannelSearch): Boolean = true
    }) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(FragmentSearchChannelsListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: FragmentSearchChannelsListItemBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ChannelSearch?) {
            with(binding) {
                val context = fragment.requireContext()
                if (item != null) {
                    root.setOnClickListener { listener.viewChannel(item.id, item.broadcaster_login, item.display_name, item.channelLogo) }
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