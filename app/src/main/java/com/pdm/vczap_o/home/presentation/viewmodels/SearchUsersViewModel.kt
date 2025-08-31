package com.pdm.vczap_o.home.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdm.vczap_o.core.domain.logger
import com.pdm.vczap_o.home.domain.model.SearchUsersUiState
import com.google.firebase.auth.FirebaseAuth
import com.pdm.vczap_o.home.domain.usecase.SearchUsersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchUsersViewModel @Inject constructor(
    private val searchUsersUseCase: SearchUsersUseCase,
    auth: FirebaseAuth,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUsersUiState())
    val uiState: StateFlow<SearchUsersUiState> = _uiState

    private var searchJob: Job? = null
    private val tag = "SearchUsersViewModel"
    private val currentUserId = auth.currentUser?.uid

    init {
        logger(tag, "ViewModel created for user: $currentUserId")
    }

    fun onSearchTextChange(text: String) {
        _uiState.update { it.copy(searchText = text) }
        searchUsers(text)
    }

    private fun searchUsers(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    filteredUsers = emptyList(),
                    errorMessage = "Searching..."
                )
            }
            searchUsersUseCase(query, currentUserId).collect { result ->
                result.onSuccess { users ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            filteredUsers = users,
                            errorMessage = if (users.isEmpty() && query.isNotEmpty()) "No users found" else "Search users"
                        )
                    }
                }
                result.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "An error occurred\nPlease check your internet connection"
                        )
                    }
                    println("Error searching users: $error")
                }
            }
        }
    }
}