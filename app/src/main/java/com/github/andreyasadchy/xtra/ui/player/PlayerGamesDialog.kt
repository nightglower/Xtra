package com.github.andreyasadchy.xtra.ui.player

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.widget.NestedScrollView
import androidx.navigation.fragment.navArgs
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.ui.common.ExpandingBottomSheetDialogFragment
import com.github.andreyasadchy.xtra.ui.view.GridRecyclerView


class PlayerGamesDialog : ExpandingBottomSheetDialogFragment() {

    private val args: PlayerGamesDialogArgs by navArgs()

    interface PlayerSeekListener {
        fun seek(position: Long)
    }

    lateinit var listener: PlayerSeekListener

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = parentFragment as PlayerSeekListener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = requireContext()
        val layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val recycleView = GridRecyclerView(context).apply {
            id = R.id.recyclerView
            setLayoutParams(layoutParams)
            adapter = PlayerGamesDialogAdapter(this@PlayerGamesDialog, args.games)
        }
        return NestedScrollView(context).apply { addView(recycleView) }
    }
}
