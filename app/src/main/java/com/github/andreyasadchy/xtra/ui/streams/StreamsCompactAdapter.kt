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
import com.github.andreyasadchy.xtra.databinding.FragmentStreamsListItemBinding
import com.github.andreyasadchy.xtra.model.ui.Stream
import com.github.andreyasadchy.xtra.ui.channel.ChannelPagerFragmentDirections
import com.github.andreyasadchy.xtra.ui.games.GameMediaFragmentDirections
import com.github.andreyasadchy.xtra.ui.games.GamePagerFragmentArgs
import com.github.andreyasadchy.xtra.ui.games.GamePagerFragmentDirections
import com.github.andreyasadchy.xtra.ui.main.MainActivity
import com.github.andreyasadchy.xtra.util.*

class StreamsCompactAdapter(
    private val fragment: Fragment,
    private val args: GamePagerFragmentArgs? = null) : PagingDataAdapter<Stream, StreamsCompactAdapter.PagingViewHolder>(
    object : DiffUtil.ItemCallback<Stream>() {
        override fun areItemsTheSame(oldItem: Stream, newItem: Stream): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Stream, newItem: Stream): Boolean =
            oldItem.viewerCount == newItem.viewerCount &&
                    oldItem.gameName == newItem.gameName &&
                    oldItem.title == newItem.title
    }) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagingViewHolder {
        val binding = FragmentStreamsListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PagingViewHolder(binding, fragment, args)
    }

    override fun onBindViewHolder(holder: PagingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PagingViewHolder(
        private val binding: FragmentStreamsListItemBinding,
        private val fragment: Fragment,
        private val args: GamePagerFragmentArgs?): RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Stream?) {
            with(binding) {
                if (item != null) {
                    val context = fragment.requireContext()
                    val channelListener: (View) -> Unit = { fragment.findNavController().navigate(ChannelPagerFragmentDirections.actionGlobalChannelPagerFragment(
                        channelId = item.channelId,
                        channelLogin = item.channelLogin,
                        channelName = item.channelName,
                        channelLogo = item.channelLogo,
                        streamId = item.id
                    )) }
                    val gameListener: (View) -> Unit = {
                        fragment.findNavController().navigate(
                            if (context.prefs().getBoolean(C.UI_GAMEPAGER, true)) {
                                GamePagerFragmentDirections.actionGlobalGamePagerFragment(
                                    gameId = item.gameId,
                                    gameName = item.gameName
                                )
                            } else {
                                GameMediaFragmentDirections.actionGlobalGameMediaFragment(
                                    gameId = item.gameId,
                                    gameName = item.gameName
                                )
                            }
                        )
                    }
                    root.setOnClickListener {
                        (fragment.activity as MainActivity).startStream(item)
                    }
                    if (item.channelLogo != null)  {
                        userImage.visible()
                        userImage.loadImage(fragment, item.channelLogo, circle = true)
                        userImage.setOnClickListener(channelListener)
                    } else {
                        userImage.gone()
                    }
                    if (item.channelName != null)  {
                        username.visible()
                        username.text = item.channelName
                        username.setOnClickListener(channelListener)
                    } else {
                        username.gone()
                    }
                    if (item.title != null && item.title != "")  {
                        title.visible()
                        title.text = item.title.trim()
                    } else {
                        title.gone()
                    }
                    if (item.gameName != null)  {
                        gameName.visible()
                        gameName.text = item.gameName
                        gameName.setOnClickListener(gameListener)
                    } else {
                        gameName.gone()
                    }
                    if (item.viewerCount != null) {
                        viewers.visible()
                        viewers.text = TwitchApiHelper.formatCount(context, item.viewerCount ?: 0)
                    } else {
                        viewers.gone()
                    }
                    if (item.type != null) {
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
                    if (context.prefs().getBoolean(C.UI_UPTIME, true) && item.startedAt != null) {
                        val text = TwitchApiHelper.getUptime(context = context, input = item.startedAt)
                        if (text != null) {
                            uptime.visible()
                            uptime.text = text
                        } else {
                            uptime.gone()
                        }
                    } else {
                        uptime.gone()
                    }
                    if (!item.tags.isNullOrEmpty() && context.prefs().getBoolean(C.UI_TAGS, true)) {
                        tagsLayout.removeAllViews()
                        tagsLayout.visible()
                        for (tag in item.tags) {
                            val text = TextView(context)
                            text.text = tag
                            text.setOnClickListener {
                                fragment.findNavController().navigate(GamePagerFragmentDirections.actionGlobalGamePagerFragment(
                                    gameId = args?.gameId,
                                    gameName = args?.gameName,
                                    tags = arrayOf(tag),
                                ))
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
}