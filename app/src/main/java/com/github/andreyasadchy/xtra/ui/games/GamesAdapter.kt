package com.github.andreyasadchy.xtra.ui.games

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.databinding.FragmentGamesListItemBinding
import com.github.andreyasadchy.xtra.model.helix.game.Game
import com.github.andreyasadchy.xtra.ui.common.BasePagedListAdapter
import com.github.andreyasadchy.xtra.util.*

class GamesAdapter(
    private val fragment: Fragment,
    private val listener: GamesFragment.OnGameSelectedListener,
    private val gamesListener: GamesFragment.OnTagGames) : BasePagedListAdapter<Game, GamesAdapter.ViewHolder>(
    object : DiffUtil.ItemCallback<Game>() {
        override fun areItemsTheSame(oldItem: Game, newItem: Game): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Game, newItem: Game): Boolean =
            oldItem.viewersCount == newItem.viewersCount
    }) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(FragmentGamesListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: FragmentGamesListItemBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Game?) {
            with(binding) {
                val context = fragment.requireContext()
                if (item != null) {
                    root.setOnClickListener { listener.openGame(item.id, item.name) }
                }
                if (item?.boxArt != null)  {
                    gameImage.visible()
                    gameImage.loadImage(fragment, item.boxArt)
                } else {
                    gameImage.gone()
                }
                if (item?.name != null)  {
                    gameName.visible()
                    gameName.text = item.name
                } else {
                    gameName.gone()
                }
                if (item?.viewersCount != null) {
                    viewers.visible()
                    viewers.text = TwitchApiHelper.formatViewersCount(context, item.viewersCount!!)
                } else {
                    viewers.gone()
                }
                if (item?.broadcastersCount != null && context.prefs().getBoolean(C.UI_BROADCASTERSCOUNT, true)) {
                    broadcastersCount.visible()
                    broadcastersCount.text = context.resources.getQuantityString(R.plurals.broadcasters, item.broadcastersCount!!, item.broadcastersCount)
                } else {
                    broadcastersCount.gone()
                }
                if (!item?.tags.isNullOrEmpty() && context.prefs().getBoolean(C.UI_TAGS, true)) {
                    tagsLayout.removeAllViews()
                    tagsLayout.visible()
                    item?.tags?.forEach { tag ->
                        val text = TextView(context)
                        text.text = tag.name
                        if (tag.id != null) {
                            text.setOnClickListener { gamesListener.openTagGames(listOf(tag.id)) }
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
