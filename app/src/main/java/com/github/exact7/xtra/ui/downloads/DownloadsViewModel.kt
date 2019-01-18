package com.github.exact7.xtra.ui.downloads


import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.github.exact7.xtra.model.offline.OfflineVideo
import com.github.exact7.xtra.repository.OfflineRepository
import com.github.exact7.xtra.util.DownloadUtils
import com.iheartradio.m3u8.Encoding
import com.iheartradio.m3u8.Format
import com.iheartradio.m3u8.PlaylistParser
import java.io.File
import javax.inject.Inject

class DownloadsViewModel @Inject internal constructor(
        application: Application,
        private val repository: OfflineRepository) : AndroidViewModel(application) {

    val list = repository.loadAllVideos()

    fun delete(video: OfflineVideo) {
        repository.deleteVideo(video)
        if (video.downloaded) {
            val file = File(video.url)
            if (video.vod) {
                val playlist = PlaylistParser(file.inputStream(), Format.EXT_M3U, Encoding.UTF_8).parse() //TODO check for other playlists in folder and don't remove shared tracks and images
//                GlideApp.with(context) //remove images from cache
//                        .load(item.channelLogo)
//                        .diskCacheStrategy(DiskCacheStrategy.NONE)
//                        .skipMemoryCache(true)
//                        .into(binding.userImage)
//                GlideApp.with(context)
//                        .load(item.thumbnail)
//                        .diskCacheStrategy(DiskCacheStrategy.NONE)
//                        .skipMemoryCache(true)
//                        .into(binding.thumbnail)
                for (track in playlist.mediaPlaylist.tracks) {
                    File(track.uri).delete()
                }
                val directory = file.parentFile
                if (directory.list().size == 1) {
                    file.delete()
                    directory.delete()
                } else {
                    file.delete()
                }
            } else {
                file.delete()
            }
        } else {
            DownloadUtils.getFetch(getApplication()).deleteGroup(video.id)
        }
    }
}
