package com.github.andreyasadchy.xtra.ui.saved

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.databinding.FragmentMediaBinding
import com.github.andreyasadchy.xtra.ui.common.MediaFragment
import com.github.andreyasadchy.xtra.ui.main.MainActivity
import com.github.andreyasadchy.xtra.ui.saved.bookmarks.BookmarksFragment
import com.github.andreyasadchy.xtra.ui.saved.downloads.DownloadsFragment
import com.github.andreyasadchy.xtra.util.visible

class SavedMediaFragment : MediaFragment() {

    companion object {
        private const val DEFAULT_ITEM = "default_item"

        fun newInstance(defaultItem: Int?) = SavedMediaFragment().apply {
            arguments = Bundle().apply {
                putInt(DEFAULT_ITEM, defaultItem ?: 0)
            }
        }
    }

    override val spinnerItems: Array<String>
        get() = resources.getStringArray(R.array.spinnerSavedEntries)

    override val mediaBinding get() = binding
    private var _binding: FragmentMediaBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMediaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            val activity = requireActivity() as MainActivity
            spinner.visible()
            spinner.adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_dropdown_item, spinnerItems)
            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    currentFragment = if (position != previousItem && isResumed) {
                        val newFragment = onSpinnerItemSelected(position)
                        childFragmentManager.beginTransaction().replace(R.id.fragmentContainer, newFragment).commit()
                        previousItem = position
                        newFragment
                    } else {
                        childFragmentManager.findFragmentById(R.id.fragmentContainer)
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    override fun onSpinnerItemSelected(position: Int): Fragment {
        with(binding) {
            val defaultItem = requireArguments().getInt(DEFAULT_ITEM)
            if (firstLaunch) {
                spinner.setSelection(defaultItem)
                firstLaunch = false
            }
            return when (position) {
                0 -> BookmarksFragment()
                else -> DownloadsFragment()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}