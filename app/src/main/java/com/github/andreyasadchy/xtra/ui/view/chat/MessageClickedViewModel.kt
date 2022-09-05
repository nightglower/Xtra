package com.github.andreyasadchy.xtra.ui.view.chat

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.andreyasadchy.xtra.model.helix.user.User
import com.github.andreyasadchy.xtra.repository.ApiRepository
import com.github.andreyasadchy.xtra.util.C
import com.github.andreyasadchy.xtra.util.prefs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MessageClickedViewModel @Inject constructor(private val repository: ApiRepository) : ViewModel() {

    private val user = MutableLiveData<User?>()
    private var isLoading = false

    fun loadUser(context: Context, channelId: String, targetId: String? = null): MutableLiveData<User?> {
        if (user.value == null && !isLoading) {
            isLoading = true
            viewModelScope.launch {
                try {
                    val u = repository.loadUserMessageClicked(
                        channelId = channelId,
                        targetId = targetId,
                        helixClientId = context.prefs().getString(C.HELIX_CLIENT_ID, ""),
                        helixToken = com.github.andreyasadchy.xtra.model.User.get(context).helixToken,
                        gqlClientId = context.prefs().getString(C.GQL_CLIENT_ID, ""))
                    user.postValue(u)
                } catch (e: Exception) {
                } finally {
                    isLoading = false
                }
            }
        }
        return user
    }
}