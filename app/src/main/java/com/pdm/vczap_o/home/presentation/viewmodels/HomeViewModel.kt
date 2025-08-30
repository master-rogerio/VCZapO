package com.pdm.vczap_o.home.presentation.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdm.vczap_o.core.domain.logger
import com.pdm.vczap_o.home.data.RoomsCache
import com.pdm.vczap_o.home.domain.model.HomeUiState
import com.pdm.vczap_o.home.domain.usecase.GetFCMTokenUseCase
import com.pdm.vczap_o.home.domain.usecase.GetUnreadMessagesUseCase
import com.pdm.vczap_o.home.domain.usecase.ListenToRoomsUseCase
import com.google.firebase.auth.FirebaseAuth
import com.pdm.vczap_o.group.domain.usecase.GetGroupsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getUnreadMessagesUseCase: GetUnreadMessagesUseCase,
    private val getFCMTokenUseCase: GetFCMTokenUseCase,
    private val listenToRoomsUseCase: ListenToRoomsUseCase,
    // INÍCIO DA ADIÇÃO - PASSO 5
    private val getGroupsUseCase: GetGroupsUseCase,
    // FIM DA ADIÇÃO - PASSO 5
    context: Context,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    private val tag = "HomeViewModel"
    private val cacheHelper = RoomsCache(context = context)
    private val auth = FirebaseAuth.getInstance()
    private var roomsListenerJob = viewModelScope.launch { } // Initialize an empty job
    // INÍCIO DA ADIÇÃO - PASSO 5
    private var groupsListenerJob = viewModelScope.launch { }
    // FIM DA ADIÇÃO - PASSO 5
    val uiState: StateFlow<HomeUiState> = _uiState

    private fun loadCachedRooms() {
        viewModelScope.launch {
            try {
                val cachedRooms = cacheHelper.loadRooms()
                _uiState.update { it.copy(rooms = cachedRooms, isLoading = false) }
                logger(tag, "Cached rooms loaded: ${cachedRooms.size}")
            } catch (e: Exception) {
                logger(tag, "Error loading cached rooms: ${e.message}")
                _uiState.update { it.copy(error = "Error loading cached rooms") }
            }
        }
    }

    init {
        if (auth.currentUser != null) {
            loadCachedRooms()
            listenToRooms()
            // INÍCIO DA ADIÇÃO - PASSO 5
            listenToGroups()
            // FIM DA ADIÇÃO - PASSO 5
        } else {
            _uiState.update { it.copy(error = "User not authenticated") }
        }
    }

    fun getUnreadMessages(
        roomId: String,
        otherUserId: String,
        callBack: (value: Int) -> Unit,
    ) {
        getUnreadMessagesUseCase(roomId, otherUserId, callBack)
    }

    fun getFCMToken(callBack: (value: String) -> Unit) {
        getFCMTokenUseCase(callBack)
    }

    private fun listenToRooms() {
        val userId = auth.currentUser?.uid ?: run {
            _uiState.update { it.copy(error = "User not authenticated") }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }
        roomsListenerJob = viewModelScope.launch {
            try {
                listenToRoomsUseCase(userId).collectLatest { result ->
                    result.onSuccess { roomsList ->
                        _uiState.update { it.copy(rooms = roomsList, isLoading = false, error = null) }

                        viewModelScope.launch {
                            try {
                                cacheHelper.saveRooms(roomsList)
                                logger(tag, "Rooms saved to cache: ${roomsList.size}")
                            } catch (e: Exception) {
                                logger(tag, "Error saving rooms to cache: ${e.message}")
                            }
                        }

                        logger(tag, "Rooms updated: ${roomsList.size}")
                    }
                    result.onFailure { error ->
                        _uiState.update {
                            it.copy(
                                error = "Failed to load rooms: ${error.message}",
                                isLoading = false
                            )
                        }
                        logger(tag, "Failed to load rooms: ${error.message}")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = "Error in rooms listener: ${e.message}",
                        isLoading = false
                    )
                }
                logger(tag, "Error in rooms listener: ${e.message}")
            }
        }
    }

    // INÍCIO DA ADIÇÃO - PASSO 5
    private fun listenToGroups() {
        val userId = auth.currentUser?.uid ?: run {
            _uiState.update { it.copy(error = "User not authenticated") }
            return
        }

        groupsListenerJob = viewModelScope.launch {
            try {
                getGroupsUseCase(userId).collectLatest { result ->
                    result.onSuccess { groupsList ->
                        _uiState.update { it.copy(groups = groupsList) }
                        logger(tag, "Groups updated: ${groupsList.size}")
                    }
                    result.onFailure { error ->
                        _uiState.update {
                            it.copy(error = "Failed to load groups: ${error.message}")
                        }
                        logger(tag, "Failed to load groups: ${error.message}")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Error in groups listener: ${e.message}")
                }
                logger(tag, "Error in groups listener: ${e.message}")
            }
        }
    }
    // FIM DA ADIÇÃO - PASSO 5

    fun retryLoadRooms() {
        roomsListenerJob.cancel()
        // INÍCIO DA ADIÇÃO - PASSO 5
        groupsListenerJob.cancel()
        // FIM DA ADIÇÃO - PASSO 5
        listenToRooms()
        // INÍCIO DA ADIÇÃO - PASSO 5
        listenToGroups()
        // FIM DA ADIÇÃO - PASSO 5
    }

    override fun onCleared() {
        roomsListenerJob.cancel()
        // INÍCIO DA ADIÇÃO - PASSO 5
        groupsListenerJob.cancel()
        // FIM DA ADIÇÃO - PASSO 5
        super.onCleared()
    }
}

