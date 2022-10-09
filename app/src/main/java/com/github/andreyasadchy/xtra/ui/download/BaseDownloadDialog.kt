package com.github.andreyasadchy.xtra.ui.download

import android.content.Context
import android.content.SharedPreferences
import android.widget.Button
import android.widget.RadioButton
import androidx.core.content.edit
import androidx.fragment.app.DialogFragment
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.databinding.StorageSelectionBinding
import com.github.andreyasadchy.xtra.util.*
import kotlin.math.max

abstract class BaseDownloadDialog : DialogFragment() {

    abstract val baseBinding: StorageSelectionBinding
    private val binding get() = baseBinding
    protected lateinit var prefs: SharedPreferences
    private lateinit var storage: List<Storage>
    protected val downloadPath: String
        get() {
            val index = if (storage.size == 1) {
                0
            } else {
                val checked = max(binding.radioGroup.checkedRadioButtonId, 0)
                prefs.edit { putInt(C.DOWNLOAD_STORAGE, checked) }
                checked
            }
            return storage[index].path
        }

    protected fun init(context: Context) {
        with(binding) {
            prefs = context.prefs()
            storage = DownloadUtils.getAvailableStorage(context)
            if (DownloadUtils.isExternalStorageAvailable) {
                if (storage.size > 1) {
                    root.visible()
                    for (s in storage) {
                        radioGroup.addView(RadioButton(context).apply {
                            id = s.id
                            text = s.name
                        })
                    }
                    radioGroup.check(prefs.getInt(C.DOWNLOAD_STORAGE, 0))
                }
            } else {
                root.visible()
                noStorageDetected.visible()
                requireView().findViewById<Button>(R.id.download).gone()
            }
        }
    }

    data class Storage(
            val id: Int,
            val name: String,
            val path: String)
}