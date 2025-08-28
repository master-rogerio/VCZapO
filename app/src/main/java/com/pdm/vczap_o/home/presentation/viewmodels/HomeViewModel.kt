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
    context: Context,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    private val tag = "HomeViewModel"
    private val cacheHelper = RoomsCache(context = context)
    private val auth = FirebaseAuth.getInstance()
    private var roomsListenerJob = viewModelScope.launch { } // Initialize an empty job
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
            // ALTERAÇÃO 28/08/2025 R - Carrega salas do cache primeiro, depois escuta mudanças
            loadCachedRooms()
            listenToRooms()
            // FIM ALTERAÇÃO 28/08/2025 R
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
                        // ALTERAÇÃO 28/08/2025 R - Atualiza estado e salva no cache
                        _uiState.update { it.copy(rooms = roomsList, isLoading = false, error = null) }
                        
                        // Salva as salas no cache para uso offline
                        viewModelScope.launch {
                            try {
                                cacheHelper.saveRooms(roomsList)
                                logger(tag, "Rooms saved to cache: ${roomsList.size}")
                            } catch (e: Exception) {
                                logger(tag, "Error saving rooms to cache: ${e.message}")
                            }
                        }
                        
                        logger(tag, "Rooms updated: ${roomsList.size}")
                        // FIM ALTERAÇÃO 28/08/2025 R
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

    fun retryLoadRooms() {
        roomsListenerJob.cancel()
        listenToRooms()
    }

    override fun onCleared() {
        roomsListenerJob.cancel()
        super.onCleared()
    }
}