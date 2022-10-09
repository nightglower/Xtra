package com.github.andreyasadchy.xtra.ui.streams

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.github.andreyasadchy.xtra.databinding.FragmentStreamsListItemCompactBinding
import com.github.andreyasadchy.xtra.model.helix.stream.Stream
import com.github.andreyasadchy.xtra.ui.common.BasePagedListAdapter
import com.github.andreyasadchy.xtra.ui.common.OnChannelSelectedListener
import com.github.andreyasadchy.xtra.ui.games.GamesFragment
import com.github.andreyasadchy.xtra.util.*

class StreamsCompactAdapter(
    private val fragment: Fragment,
    private val clickListener: BaseStreamsFragment.OnStreamSelectedListener,
    private val channelClickListener: OnChannelSelectedListener,
    private val gameClickListener: GamesFragment.OnGameSelectedListener) : BasePagedListAdapter<Stream, StreamsCompactAdapter.ViewHolder>(
    object : DiffUtil.ItemCallback<Stream>() {
        override fun areItemsTheSame(oldItem: Stream, newItem: Stream): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Stream, newItem: Stream): Boolean =
            oldItem.viewer_count == newItem.viewer_count &&
                    oldItem.game_name == newItem.game_name &&
                    oldItem.title == newItem.title
    }) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(FragmentStreamsListItemCompactBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: FragmentStreamsListItemCompactBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Stream?) {
            with(binding) {
                val context = fragment.requireContext()
                val channelListener: (View) -> Unit = { channelClickListener.viewChannel(item?.user_id, item?.user_login, item?.user_name, item?.channelLogo, streamId = item?.id) }
                val gameListener: (View) -> Unit = { gameClickListener.openGame(item?.game_id, item?.game_name) }
                if (item != null) {
                    root.setOnClickListener { clickListener.startStream(item) }
                }
                if (item?.channelLogo != null) {
                    userImage.visible()
                    userImage.loadImage(fragment, item.channelLogo, circle = true)
                    userImage.setOnClickListener(channelListener)
                } else {
                    userImage.gone()
                }
                if (item?.user_name != null) {
                    username.visible()
                    username.text = item.user_name
                    username.setOnClickListener(channelListener)
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
                    gameName.setOnClickListener(gameListener)
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
                            text.setOnClickListener { gameClickListener.openGame(tags = listOf(tag.id), id = fragment.parentFragment?.arguments?.getString(
                                C.GAME_ID), name = fragment.parentFragment?.arguments?.getString(C.GAME_NAME)) }
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