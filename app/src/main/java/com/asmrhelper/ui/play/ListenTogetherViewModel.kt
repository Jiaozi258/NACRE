package com.asmrhelper.ui.play

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asmrhelper.domain.model.ListenTogetherState
import com.asmrhelper.domain.repository.ListenTogetherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ListenTogetherViewModel @Inject constructor(
    private val repository: ListenTogetherRepository
) : ViewModel() {

    val state: StateFlow<ListenTogetherState> = repository.state

    fun inviteText(): String = repository.inviteText()

    fun parseRoomId(text: String): String? = repository.parseRoomId(text)

    fun setNickname(nickname: String) = viewModelScope.launch { repository.setNickname(nickname) }

    fun createRoom(onCreated: (String) -> Unit = {}) = viewModelScope.launch { onCreated(repository.createRoom()) }

    fun joinRoom(roomId: String, onJoined: (Boolean) -> Unit = {}) = viewModelScope.launch { onJoined(repository.joinRoom(roomId)) }

    fun leaveRoom() = viewModelScope.launch { repository.leaveRoom() }
}
