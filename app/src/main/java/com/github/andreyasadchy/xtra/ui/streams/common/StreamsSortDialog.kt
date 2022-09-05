package com.github.andreyasadchy.xtra.ui.streams.common

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.databinding.DialogStreamsSortBinding
import com.github.andreyasadchy.xtra.model.helix.stream.Sort
import com.github.andreyasadchy.xtra.ui.common.ExpandingBottomSheetDialogFragment

class StreamsSortDialog : ExpandingBottomSheetDialogFragment() {

    interface OnFilter {
        fun onChange(sort: Sort)
    }

    private var _binding: DialogStreamsSortBinding? = null
    private val binding get() = _binding!!
    private val args: StreamsSortDialogArgs by navArgs()
    private lateinit var listener: OnFilter

    override fun onAttach(context: Context) {
        super.onAttach(context)
        //listener = requireParentFragment().childFragmentManager.fragments[0] as OnFilter
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogStreamsSortBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            //val activity = requireActivity() as MainActivity
            val originalSortId = if (args.sort == Sort.VIEWERS_HIGH) R.id.viewers_high else R.id.viewers_low
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
                //activity.openTagSearch(gameId = parentFragment?.arguments?.getString(C.GAME_ID), gameName = parentFragment?.arguments?.getString(C.GAME_NAME))
                dismiss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}