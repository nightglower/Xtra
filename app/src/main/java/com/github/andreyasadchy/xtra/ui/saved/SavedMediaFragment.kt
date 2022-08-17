package com.github.andreyasadchy.xtra.ui.saved

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.ui.common.MediaFragment
import com.github.andreyasadchy.xtra.ui.main.MainActivity
import com.github.andreyasadchy.xtra.ui.saved.bookmarks.BookmarksFragment
import com.github.andreyasadchy.xtra.ui.saved.downloads.DownloadsFragment
import com.github.andreyasadchy.xtra.util.C
import com.github.andreyasadchy.xtra.util.prefs
import com.github.andreyasadchy.xtra.util.visible
import kotlinx.android.synthetic.main.fragment_media.*

class SavedMediaFragment : MediaFragment() {

    private var pagerFragment = true
    private var defaultItem = 0
    private var firstLaunch = true

    override val spinnerItems: Array<String>
        get() = resources.getStringArray(R.array.spinnerSavedEntries)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity() as MainActivity
        pagerFragment = requireContext().prefs().getBoolean(C.UI_SAVEDPAGER, true)
        defaultItem = requireContext().prefs().getString(C.UI_SAVED_DEFAULT_PAGE, "0")?.toInt() ?: 0
        if (pagerFragment) {
            currentFragment = if (previousItem != -2) {
                val newFragment = SavedPagerFragment.newInstance(defaultItem)
                childFragmentManager.beginTransaction().replace(R.id.fragmentContainer, newFragment).commit()
                previousItem = -2
                newFragment
            } else {
                childFragmentManager.findFragmentById(R.id.fragmentContainer)
            }
        } else {
            spinner.visible()
            spinner.adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_dropdown_item, spinnerItems)
            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    currentFragment = if (position != previousItem) {
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