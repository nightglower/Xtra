package com.github.andreyasadchy.xtra.ui.download

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.RadioButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.edit
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.databinding.DialogClipDownloadBinding
import com.github.andreyasadchy.xtra.util.*
import kotlin.math.max

class ClipDownloadDialog : DialogFragment() {

    private var _binding: DialogClipDownloadBinding? = null
    private val binding get() = _binding!!
    private val args: ClipDownloadDialogArgs by navArgs()
    private val viewModel: ClipDownloadViewModel by viewModels()

    private lateinit var storage: List<DownloadUtils.Storage>
    private val downloadPath: String
        get() {
            val index = if (storage.size == 1) {
                0
            } else {
                val checked = max(binding.storageSelectionContainer.radioGroup.checkedRadioButtonId, 0)
                requireContext().prefs().edit { putInt(C.DOWNLOAD_STORAGE, checked) }
                checked
            }
            return storage[index].path
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogClipDownloadBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val map = mutableMapOf<String, String>()
        args.qualityValues?.let { values ->
            args.qualityKeys?.forEachIndexed { index, key ->
                map[key] = values[index]
            }
        }
        viewModel.init(requireContext().prefs().getString(C.GQL_CLIENT_ID, ""), args.clip, map)
        viewModel.qualities.observe(viewLifecycleOwner) {
            ((requireView() as NestedScrollView).children.first() as ConstraintLayout).children.forEach { v -> v.isVisible = v.id != R.id.progressBar && v.id != R.id.storageSelectionContainer }
            init(it)
        }
    }

    private fun init(qualities: Map<String, String>) {
        with(binding) {
            val context = requireContext()
            storage = DownloadUtils.getAvailableStorage(context)
            if (DownloadUtils.isExternalStorageAvailable) {
                if (storage.size > 1) {
                    storageSelectionContainer.root.visible()
                    for (s in storage) {
                        storageSelectionContainer.radioGroup.addView(RadioButton(context).apply {
                            id = s.id
                            text = s.name
                        })
                    }
                    storageSelectionContainer.radioGroup.check(context.prefs().getInt(C.DOWNLOAD_STORAGE, 0))
                }
            } else {
                storageSelectionContainer.root.visible()
                storageSelectionContainer.noStorageDetected.visible()
                download.gone()
            }
            spinner.adapter = ArrayAdapter(context, R.layout.spinner_quality_item, qualities.keys.toTypedArray())
            cancel.setOnClickListener { dismiss() }
            download.setOnClickListener {
                val quality = spinner.selectedItem.toString()
                viewModel.download(qualities.getValue(quality), downloadPath, quality)
                dismiss()
            }
        }
    }
}
