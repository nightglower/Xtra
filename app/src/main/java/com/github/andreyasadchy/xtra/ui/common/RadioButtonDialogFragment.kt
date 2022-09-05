package com.github.andreyasadchy.xtra.ui.common

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.core.widget.NestedScrollView
import androidx.navigation.fragment.navArgs


class RadioButtonDialogFragment : ExpandingBottomSheetDialogFragment() {

    interface OnSortOptionChanged {
        fun onChange(requestCode: Int, index: Int, text: CharSequence, tag: Int?)
    }

    private val args: RadioButtonDialogFragmentArgs by navArgs()

    private lateinit var listenerSort: OnSortOptionChanged

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listenerSort = parentFragment as OnSortOptionChanged
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val context = requireContext()
        val layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        val radioGroup = RadioGroup(context).also { it.layoutParams = layoutParams }
        val checkedId = args.checkedIndex
        val clickListener = View.OnClickListener { v ->
            val clickedId = v.id
            if (clickedId != checkedId) {
                listenerSort.onChange(args.requestCode, clickedId, (v as RadioButton).text, v.tag as Int?)
            }
            dismiss()
        }
        val tags = args.tags
        args.labels.forEachIndexed { index, label ->
            val button = AppCompatRadioButton(context).apply {
                id = index
                text = label
                tag = tags?.getOrNull(index)
                setOnClickListener(clickListener)
            }
            radioGroup.addView(button, layoutParams)
        }
        radioGroup.check(checkedId)
        return NestedScrollView(context).apply { addView(radioGroup) }
    }
}