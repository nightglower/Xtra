package com.github.andreyasadchy.xtra.ui.follow.channels

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.navigation.fragment.navArgs
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.databinding.DialogFollowedChannelsSortBinding
import com.github.andreyasadchy.xtra.model.helix.follows.Order
import com.github.andreyasadchy.xtra.model.helix.follows.Order.ASC
import com.github.andreyasadchy.xtra.model.helix.follows.Order.DESC
import com.github.andreyasadchy.xtra.model.helix.follows.Sort
import com.github.andreyasadchy.xtra.model.helix.follows.Sort.*
import com.github.andreyasadchy.xtra.ui.common.ExpandingBottomSheetDialogFragment

class FollowedChannelsSortDialog : ExpandingBottomSheetDialogFragment() {

    interface OnFilter {
        fun onChange(sort: Sort, sortText: CharSequence, order: Order, orderText: CharSequence, saveDefault: Boolean)
    }

    private var _binding: DialogFollowedChannelsSortBinding? = null
    private val binding get() = _binding!!
    private val args: FollowedChannelsSortDialogArgs by navArgs()
    private lateinit var listener: OnFilter

    override fun onAttach(context: Context) {
        super.onAttach(context)
        //listener = requireParentFragment().childFragmentManager.fragments[0] as OnFilter
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogFollowedChannelsSortBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            val originalSortId = when (args.sort) {
                FOLLOWED_AT -> R.id.time_followed
                ALPHABETICALLY -> R.id.alphabetically
                LAST_BROADCAST -> R.id.last_broadcast
            }
            val originalOrderId = if (args.order == DESC) R.id.newest_first else R.id.oldest_first
            val originalSaveDefault = args.saveDefault
            sort.check(originalSortId)
            order.check(originalOrderId)
            saveDefault.isChecked = originalSaveDefault
            apply.setOnClickListener {
                val checkedSortId = sort.checkedRadioButtonId
                val checkedOrderId = order.checkedRadioButtonId
                val checkedSaveDefault = saveDefault.isChecked
                if (checkedSortId != originalSortId || checkedOrderId != originalOrderId || checkedSaveDefault != originalSaveDefault) {
                    val sortBtn = view.findViewById<RadioButton>(checkedSortId)
                    val orderBtn = view.findViewById<RadioButton>(checkedOrderId)
                    listener.onChange(
                        when (checkedSortId) {
                            R.id.time_followed -> FOLLOWED_AT
                            R.id.alphabetically -> ALPHABETICALLY
                            else -> LAST_BROADCAST
                        },
                        sortBtn.text,
                        if (checkedOrderId == R.id.newest_first) DESC else ASC,
                        orderBtn.text,
                        checkedSaveDefault
                    )
                }
                dismiss()
            }
        }
    }
}