package com.github.andreyasadchy.xtra.model.ui

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ChannelViewerList(
        val broadcasters: List<String>,
        val moderators: List<String>,
        val vips: List<String>,
        val viewers: List<String>,
        val count: Int?) : Parcelable