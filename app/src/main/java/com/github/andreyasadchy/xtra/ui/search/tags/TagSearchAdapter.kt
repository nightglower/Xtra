package com.github.andreyasadchy.xtra.ui.search.tags

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.github.andreyasadchy.xtra.databinding.FragmentSearchChannelsListItemBinding
import com.github.andreyasadchy.xtra.model.helix.tag.Tag
import com.github.andreyasadchy.xtra.ui.common.BasePagedListAdapter
import com.github.andreyasadchy.xtra.ui.games.GamesFragment
import com.github.andreyasadchy.xtra.util.C
import com.github.andreyasadchy.xtra.util.gone
import com.github.andreyasadchy.xtra.util.visible

class TagSearchAdapter(
    private val fragment: Fragment,
    private val gamesListener: GamesFragment.OnTagGames,
    private val streamsListener: GamesFragment.OnGameSelectedListener) : BasePagedListAdapter<Tag, TagSearchAdapter.ViewHolder>(
    object : DiffUtil.ItemCallback<Tag>() {
        override fun areItemsTheSame(oldItem: Tag, newItem: Tag): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Tag, newItem: Tag): Boolean = true
    }) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(FragmentSearchChannelsListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: FragmentSearchChannelsListItemBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Tag?) {
            with(binding) {
                if (item?.name != null) {
                    userName.visible()
                    userName.text = item.name
                } else {
                    userName.gone()
                }
                if (item?.id != null) {
                    if (item.scope == "CATEGORY") {
                        root.setOnClickListener { gamesListener.openTagGames(listOf(item.id)) }
                    } else {
                        root.setOnClickListener { streamsListener.openGame(tags = listOf(item.id), id = fragment.parentFragment?.arguments?.getString(C.GAME_ID), name = fragment.parentFragment?.arguments?.getString(C.GAME_NAME)) }
                    }
                }
            }
        }
    }
}