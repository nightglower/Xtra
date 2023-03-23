package com.github.andreyasadchy.xtra.ui.games

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.databinding.FragmentGamesListItemBinding
import com.github.andreyasadchy.xtra.model.ui.Game
import com.github.andreyasadchy.xtra.util.*

class GamesAdapter(
    private val fragment: Fragment) : PagingDataAdapter<Game, GamesAdapter.PagingViewHolder>(
    object : DiffUtil.ItemCallback<Game>() {
        override fun areItemsTheSame(oldItem: Game, newItem: Game): Boolean =
            oldItem.gameId == newItem.gameId

        override fun areContentsTheSame(oldItem: Game, newItem: Game): Boolean =
            oldItem.viewersCount == newItem.viewersCount
    }) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagingViewHolder {
        val binding = FragmentGamesListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PagingViewHolder(binding, fragment)
    }

    override fun onBindViewHolder(holder: PagingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PagingViewHolder(
        private val binding: FragmentGamesListItemBinding,
        private val fragment: Fragment): RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Game?) {
            with(binding) {
                if (item != null) {
                    val context = fragment.requireContext()
                    root.setOnClickListener {
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
                    if (item.boxArt != null)  {
                        gameImage.visible()
                        gameImage.loadImage(fragment, item.boxArt)
                    } else {
                        gameImage.gone()
                    }
                    if (item.gameName != null)  {
                        gameName.visible()
                        gameName.text = item.gameName
                    } else {
                        gameName.gone()
                    }
                    if (item.viewersCount != null) {
                        viewers.visible()
                        viewers.text = TwitchApiHelper.formatViewersCount(context, item.viewersCount!!)
                    } else {
                        viewers.gone()
                    }
                    if (item.broadcastersCount != null && context.prefs().getBoolean(C.UI_BROADCASTERSCOUNT, true)) {
                        broadcastersCount.visible()
                        broadcastersCount.text = context.resources.getQuantityString(R.plurals.broadcasters, item.broadcastersCount!!, item.broadcastersCount)
                    } else {
                        broadcastersCount.gone()
                    }
                    if (!item.tags.isNullOrEmpty() && context.prefs().getBoolean(C.UI_TAGS, true)) {
                        tagsLayout.removeAllViews()
                        tagsLayout.visible()
                        for (tag in item.tags!!) {
                            val text = TextView(context)
                            text.text = tag.name
                            if (tag.id != null) {
                                text.setOnClickListener {
                                    fragment.findNavController().navigate(GamesFragmentDirections.actionGlobalGamesFragment(
                                        tags = arrayOf(tag.id)
                                    ))
                                }
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
