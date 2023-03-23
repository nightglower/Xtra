package com.github.andreyasadchy.xtra.model.ui

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Tag(
    val id: String? = null,
    val name: String? = null,
    val scope: String? = null) : Parcelable