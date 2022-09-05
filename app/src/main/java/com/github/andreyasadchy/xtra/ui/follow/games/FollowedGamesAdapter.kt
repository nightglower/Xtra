package com.github.andreyasadchy.xtra.ui.follow.games

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.databinding.FragmentFollowedGamesListItemBinding
import com.github.andreyasadchy.xtra.model.helix.game.Game
import com.github.andreyasadchy.xtra.ui.games.GameFragmentDirections
import com.github.andreyasadchy.xtra.ui.games.GamesFragmentDirections
import com.github.andreyasadchy.xtra.util.*

class FollowedGamesAdapter(
    private val fragment: Fragment) : PagingDataAdapter<Game, FollowedGamesAdapter.PagingViewHolder>(
    object : DiffUtil.ItemCallback<Game>() {
        override fun areItemsTheSame(oldItem: Game, newItem: Game): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Game, newItem: Game): Boolean =
            oldItem.viewersCount == newItem.viewersCount
    }) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagingViewHolder {
        val binding = FragmentFollowedGamesListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PagingViewHolder(binding, fragment)
    }

    override fun onBindViewHolder(holder: PagingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PagingViewHolder(
        private val binding: FragmentFollowedGamesListItemBinding,
        private val fragment: Fragment): RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Game?) {
            val context = fragment.requireContext()
            with(binding) {
                if (item != null) {
                    root.setOnClickListener { fragment.findNavController().navigate(GameFragmentDirections.actionGlobalGameFragment(
                        gameId = item.id,
                        gameName = item.name,
                        updateLocal = true
                    )) }
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
                            text.setOnClickListener { fragment.findNavController().navigate(GamesFragmentDirections.actionGlobalGamesFragment(tags = listOf(tag.id).toTypedArray())) }
                        }
                        tagsLayout.addView(text)
                    }
                } else {
                    tagsLayout.gone()
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
