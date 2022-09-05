package com.github.andreyasadchy.xtra.ui.streams

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.github.andreyasadchy.xtra.databinding.FragmentStreamsListItemCompactBinding
import com.github.andreyasadchy.xtra.model.helix.stream.Stream
import com.github.andreyasadchy.xtra.ui.channel.ChannelPagerFragmentDirections
import com.github.andreyasadchy.xtra.ui.games.GameFragmentDirections
import com.github.andreyasadchy.xtra.ui.streams.common.StreamsFragment
import com.github.andreyasadchy.xtra.ui.streams.common.StreamsFragmentArgs
import com.github.andreyasadchy.xtra.util.*

class StreamsCompactAdapter(
    private val fragment: Fragment,
    private val clickListener: StreamsFragment.OnStreamSelectedListener,
    private val args: StreamsFragmentArgs? = null) : PagingDataAdapter<Stream, StreamsCompactAdapter.PagingViewHolder>(
    object : DiffUtil.ItemCallback<Stream>() {
        override fun areItemsTheSame(oldItem: Stream, newItem: Stream): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Stream, newItem: Stream): Boolean =
            oldItem.viewer_count == newItem.viewer_count &&
                    oldItem.game_name == newItem.game_name &&
                    oldItem.title == newItem.title
    }) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagingViewHolder {
        val binding = FragmentStreamsListItemCompactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PagingViewHolder(binding, fragment, clickListener, args)
    }

    override fun onBindViewHolder(holder: PagingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PagingViewHolder(
        private val binding: FragmentStreamsListItemCompactBinding,
        private val fragment: Fragment,
        private val clickListener: StreamsFragment.OnStreamSelectedListener,
        private val args: StreamsFragmentArgs?): RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Stream?) {
            with(binding) {
                val context = fragment.requireContext()
                val channelClickListener: (View) -> Unit = { fragment.findNavController().navigate(ChannelPagerFragmentDirections.actionGlobalChannelPagerFragment(
                    channelId = item?.user_id,
                    channelLogin = item?.user_login,
                    channelName = item?.user_name,
                    channelLogo = item?.channelLogo,
                    streamId = item?.id
                )) }
                val gameClickListener: (View) -> Unit = { fragment.findNavController().navigate(GameFragmentDirections.actionGlobalGameFragment(
                    gameId = item?.game_id,
                    gameName = item?.game_name
                )) }
                if (item != null) {
                    root.setOnClickListener { clickListener.startStream(item) }
                }
                if (item?.channelLogo != null) {
                    userImage.visible()
                    userImage.loadImage(fragment, item.channelLogo, circle = true)
                    userImage.setOnClickListener(channelClickListener)
                } else {
                    userImage.gone()
                }
                if (item?.user_name != null) {
                    username.visible()
                    username.text = item.user_name
                    username.setOnClickListener(channelClickListener)
                } else {
                    username.gone()
                }
                if (item?.title != null && item.title != "") {
                    title.visible()
                    title.text = item.title.trim()
                } else {
                    title.gone()
                }
                if (item?.game_name != null) {
                    gameName.visible()
                    gameName.text = item.game_name
                    gameName.setOnClickListener(gameClickListener)
                } else {
                    gameName.gone()
                }
                if (item?.viewer_count != null) {
                    viewers.visible()
                    viewers.text = TwitchApiHelper.formatCount(context, item.viewer_count ?: 0)
                } else {
                    viewers.gone()
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
                if (item?.started_at != null && context.prefs().getBoolean(C.UI_UPTIME, true)) {
                    val text = TwitchApiHelper.getUptime(context = context, input = item.started_at)
                    if (text != null) {
                        uptime.visible()
                        uptime.text = text
                    } else {
                        uptime.gone()
                    }
                } else {
                    uptime.gone()
                }
                if (!item?.tags.isNullOrEmpty() && context.prefs().getBoolean(C.UI_TAGS, true)) {
                    tagsLayout.removeAllViews()
                    tagsLayout.visible()
                    item?.tags?.forEach { tag ->
                        val text = TextView(context)
                        text.text = tag.name
                        if (tag.id != null) {
                            text.setOnClickListener { fragment.findNavController().navigate(GameFragmentDirections.actionGlobalGameFragment(gameId = args?.gameId, gameName = args?.gameName, tags = listOf(tag.id).toTypedArray())) }
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