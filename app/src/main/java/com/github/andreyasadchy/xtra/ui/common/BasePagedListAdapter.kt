package com.github.andreyasadchy.xtra.ui.common

import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.github.andreyasadchy.xtra.repository.LoadingState

abstract class BasePagedListAdapter<T, VH : RecyclerView.ViewHolder?>(diffCallback: DiffUtil.ItemCallback<T>) : PagedListAdapter<T, VH>(diffCallback) {

    private var pagingState: LoadingState? = null

    override fun getItemCount(): Int {
        return super.getItemCount() + if (hasExtraRow()) 1 else 0
    }

    fun setPagingState(pagingState: LoadingState) {
        val previousState = this.pagingState
        val hadExtraRow = hasExtraRow()
        this.pagingState = pagingState
        val hasExtraRow = hasExtraRow()
        if (hadExtraRow != hasExtraRow) {
            if (hadExtraRow) {
                notifyItemRemoved(super.getItemCount())
            } else {
                notifyItemInserted(super.getItemCount())
            }
        } else if (hasExtraRow && previousState != pagingState) {
            notifyItemChanged(itemCount - 1)
        }
    }

    private fun hasExtraRow() = pagingState != null && pagingState != LoadingState.LOADED
}