package com.github.andreyasadchy.xtra.ui.search.tags

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.github.andreyasadchy.xtra.databinding.FragmentSearchChannelsListItemBinding
import com.github.andreyasadchy.xtra.model.helix.tag.Tag
import com.github.andreyasadchy.xtra.ui.games.GameFragmentDirections
import com.github.andreyasadchy.xtra.ui.games.GamesFragmentDirections
import com.github.andreyasadchy.xtra.util.gone
import com.github.andreyasadchy.xtra.util.visible

class TagSearchAdapter(
    private val fragment: Fragment,
    private val args: TagSearchFragmentArgs) : PagingDataAdapter<Tag, TagSearchAdapter.PagingViewHolder>(
    object : DiffUtil.ItemCallback<Tag>() {
        override fun areItemsTheSame(oldItem: Tag, newItem: Tag): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Tag, newItem: Tag): Boolean = true
    }) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagingViewHolder {
        val binding = FragmentSearchChannelsListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PagingViewHolder(binding, fragment, args)
    }

    override fun onBindViewHolder(holder: PagingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PagingViewHolder(
        private val binding: FragmentSearchChannelsListItemBinding,
        private val fragment: Fragment,
        private val args: TagSearchFragmentArgs): RecyclerView.ViewHolder(binding.root) {
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
                        root.setOnClickListener { fragment.findNavController().navigate(GamesFragmentDirections.actionGlobalGamesFragment(
                            tags = listOf(item.id).toTypedArray()
                        )) }
                    } else {
                        root.setOnClickListener { fragment.findNavController().navigate(GameFragmentDirections.actionGlobalGameFragment(
                            gameId = args.gameId,
                            gameName = args.gameName,
                            tags = listOf(item.id).toTypedArray()
                        )) }
                    }
                }
            }
        }
    }
}