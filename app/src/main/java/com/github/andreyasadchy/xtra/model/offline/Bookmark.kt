package com.github.andreyasadchy.xtra.model.offline

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "bookmarks")
data class Bookmark(
    val videoId: String? = null,
    val userId: String? = null,
    var userLogin: String? = null,
    var userName: String? = null,
    var userType: String? = null,
    var userBroadcasterType: String? = null,
    var userLogo: String? = null,
    val gameId: String? = null,
    val gameName: String? = null,
    val title: String? = null,
    val createdAt: String? = null,
    val thumbnail: String? = null,
    val type: String? = null,
    val duration: String? = null,
    val animatedPreviewURL: String? = null) : Parcelable {

    @IgnoredOnParcel
    @PrimaryKey(autoGenerate = true)
    var id = 0
}
