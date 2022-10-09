package com.github.andreyasadchy.xtra.ui.streams.common

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.databinding.DialogStreamsSortBinding
import com.github.andreyasadchy.xtra.model.helix.stream.Sort
import com.github.andreyasadchy.xtra.ui.common.ExpandingBottomSheetDialogFragment
import com.github.andreyasadchy.xtra.ui.main.MainActivity
import com.github.andreyasadchy.xtra.util.C

class StreamsSortDialog : ExpandingBottomSheetDialogFragment() {

    interface OnFilter {
        fun onChange(sort: Sort)
    }

    companion object {

        private const val SORT = "sort"

        fun newInstance(sort: Sort? = Sort.VIEWERS_HIGH): StreamsSortDialog {
            return StreamsSortDialog().apply {
                arguments = bundleOf(SORT to sort)
            }
        }
    }

    private var _binding: DialogStreamsSortBinding? = null
    private val binding get() = _binding!!
    private lateinit var listener: OnFilter

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = parentFragment as OnFilter
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogStreamsSortBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            val activity = requireActivity() as MainActivity
            val args = requireArguments()
            val originalSortId = if (args.getSerializable(SORT) as Sort == Sort.VIEWERS_HIGH) R.id.viewers_high else R.id.viewers_low
            sort.check(originalSortId)
            apply.setOnClickListener {
                val checkedSortId = sort.checkedRadioButtonId
                if (checkedSortId != originalSortId) {
                    listener.onChange(
                        if (checkedSortId == R.id.viewers_high) Sort.VIEWERS_HIGH else Sort.VIEWERS_LOW
                    )
                }
                dismiss()
            }
            selectTags.setOnClickListener {
                activity.openTagSearch(gameId = parentFragment?.arguments?.getString(C.GAME_ID), gameName = parentFragment?.arguments?.getString(C.GAME_NAME))
                dismiss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}