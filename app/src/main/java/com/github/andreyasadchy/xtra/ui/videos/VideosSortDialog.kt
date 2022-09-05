package com.github.andreyasadchy.xtra.ui.videos

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.navigation.fragment.navArgs
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.databinding.DialogStreamsSortBinding
import com.github.andreyasadchy.xtra.databinding.DialogVideosSortBinding
import com.github.andreyasadchy.xtra.model.User
import com.github.andreyasadchy.xtra.model.helix.video.BroadcastType
import com.github.andreyasadchy.xtra.model.helix.video.Period
import com.github.andreyasadchy.xtra.model.helix.video.Period.*
import com.github.andreyasadchy.xtra.model.helix.video.Sort
import com.github.andreyasadchy.xtra.model.helix.video.Sort.TIME
import com.github.andreyasadchy.xtra.model.helix.video.Sort.VIEWS
import com.github.andreyasadchy.xtra.ui.clips.common.ClipsFragment
import com.github.andreyasadchy.xtra.ui.common.ExpandingBottomSheetDialogFragment
import com.github.andreyasadchy.xtra.ui.common.RadioButtonDialogFragment
import com.github.andreyasadchy.xtra.ui.streams.common.StreamsSortDialog
import com.github.andreyasadchy.xtra.ui.streams.common.StreamsSortDialogArgs
import com.github.andreyasadchy.xtra.ui.videos.channel.ChannelVideosFragment
import com.github.andreyasadchy.xtra.ui.videos.followed.FollowedVideosFragment
import com.github.andreyasadchy.xtra.ui.videos.game.GameVideosFragment
import com.github.andreyasadchy.xtra.util.C
import com.github.andreyasadchy.xtra.util.FragmentUtils
import com.github.andreyasadchy.xtra.util.gone

class VideosSortDialog : ExpandingBottomSheetDialogFragment(), RadioButtonDialogFragment.OnSortOptionChanged {

    interface OnFilter {
        fun onChange(sort: Sort, sortText: CharSequence, period: Period, periodText: CharSequence, type: BroadcastType, languageIndex: Int, saveSort: Boolean, saveDefault: Boolean)
    }

    companion object {
        private const val REQUEST_CODE_LANGUAGE = 0
    }

    private var _binding: DialogVideosSortBinding? = null
    private val binding get() = _binding!!
    private val args: VideosSortDialogArgs by navArgs()
    private lateinit var listener: OnFilter

    private var langIndex = 0

    override fun onAttach(context: Context) {
        super.onAttach(context)
        //listener = requireParentFragment().childFragmentManager.fragments[0] as OnFilter
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogVideosSortBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            when (parentFragment) {
                is ClipsFragment -> {
                    if (args.clipChannel) {
                        sort.gone()
                        sortType.gone()
                        selectLang.gone()
                        saveSort.text = requireContext().getString(R.string.save_sort_channel)
                        saveSort.isVisible = parentFragment?.arguments?.getString(C.CHANNEL_ID).isNullOrBlank() == false
                    } else {
                        sort.gone()
                        sortType.gone()
                        saveSort.text = requireContext().getString(R.string.save_sort_game)
                        saveSort.isVisible = parentFragment?.arguments?.getString(C.GAME_ID).isNullOrBlank() == false
                    }
                }
                is ChannelVideosFragment -> {
                    period.gone()
                    selectLang.gone()
                    saveSort.text = requireContext().getString(R.string.save_sort_channel)
                    saveSort.isVisible = parentFragment?.arguments?.getString(C.CHANNEL_ID).isNullOrBlank() == false
                }
                is FollowedVideosFragment -> {
                    period.gone()
                    selectLang.gone()
                    saveSort.gone()
                }
                is GameVideosFragment -> {
                    if (User.get(requireContext()).helixToken.isNullOrBlank()) {
                        period.gone()
                    }
                    saveSort.text = requireContext().getString(R.string.save_sort_game)
                    saveSort.isVisible = parentFragment?.arguments?.getString(C.GAME_ID).isNullOrBlank() == false
                }
            }
            val originalSortId = if (args.sort == TIME) R.id.time else R.id.views
            val originalPeriodId = when (args.period) {
                DAY -> R.id.today
                WEEK -> R.id.week
                MONTH -> R.id.month
                ALL -> R.id.all
            }
            val originalTypeId = when (args.type) {
                BroadcastType.ARCHIVE -> R.id.typeArchive
                BroadcastType.HIGHLIGHT -> R.id.typeHighlight
                BroadcastType.UPLOAD -> R.id.typeUpload
                BroadcastType.ALL -> R.id.typeAll
            }
            val originalLanguageIndex = args.languageIndex
            val originalSaveSort = args.saveSort
            val originalSaveDefault = args.saveDefault
            sort.check(originalSortId)
            period.check(originalPeriodId)
            sortType.check(originalTypeId)
            langIndex = originalLanguageIndex
            saveSort.isChecked = originalSaveSort
            saveDefault.isChecked = originalSaveDefault
            apply.setOnClickListener {
                val checkedPeriodId = period.checkedRadioButtonId
                val checkedSortId = sort.checkedRadioButtonId
                val checkedTypeId = sortType.checkedRadioButtonId
                val checkedSaveSort = saveSort.isChecked
                val checkedSaveDefault = saveDefault.isChecked
                if (checkedPeriodId != originalPeriodId || checkedSortId != originalSortId || checkedTypeId != originalTypeId || langIndex != originalLanguageIndex || checkedSaveSort != originalSaveSort || checkedSaveDefault != originalSaveDefault) {
                    val sortBtn = view.findViewById<RadioButton>(checkedSortId)
                    val periodBtn = view.findViewById<RadioButton>(checkedPeriodId)
                    listener.onChange(
                        if (checkedSortId == R.id.time) TIME else VIEWS,
                        sortBtn.text,
                        when (checkedPeriodId) {
                            R.id.today -> DAY
                            R.id.week -> WEEK
                            R.id.month -> MONTH
                            else -> ALL
                        },
                        periodBtn.text,
                        when (checkedTypeId) {
                            R.id.typeArchive -> BroadcastType.ARCHIVE
                            R.id.typeHighlight -> BroadcastType.HIGHLIGHT
                            R.id.typeUpload -> BroadcastType.UPLOAD
                            else -> BroadcastType.ALL
                        },
                        langIndex,
                        checkedSaveSort,
                        checkedSaveDefault
                    )
                    //parentFragment?.scrollTop?.gone()
                }
                dismiss()
            }
            val langArray = resources.getStringArray(R.array.gqlUserLanguageEntries).toList()
            selectLang.setOnClickListener {
                //FragmentUtils.showRadioButtonDialogFragment(childFragmentManager, langArray, langIndex, REQUEST_CODE_LANGUAGE)
            }
        }
    }

    override fun onChange(requestCode: Int, index: Int, text: CharSequence, tag: Int?) {
        when (requestCode) {
            REQUEST_CODE_LANGUAGE -> {
                langIndex = index
            }
        }
    }
}
