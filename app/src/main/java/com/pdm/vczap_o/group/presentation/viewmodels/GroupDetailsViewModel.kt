package com.pdm.vczap_o.group.presentation.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.pdm.vczap_o.auth.domain.GetUserDataUseCase
import com.pdm.vczap_o.core.model.User
// Importe o repositório diretamente
import com.pdm.vczap_o.group.data.GroupRepository
import com.pdm.vczap_o.group.domain.usecase.GetGroupDetailsUseCase
import com.pdm.vczap_o.group.domain.usecase.RemoveMemberUseCase
import com.pdm.vczap_o.group.presentation.state.GroupDetailsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupDetailsViewModel @Inject constructor(
    private val getGroupDetailsUseCase: GetGroupDetailsUseCase,
    private val removeMemberUseCase: RemoveMemberUseCase,
    private val getUserDataUseCase: GetUserDataUseCase,
    private val auth: FirebaseAuth,
    // Adicionamos o repositório aqui para o nosso "hack"
    private val groupRepository: GroupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupDetailsUiState())
    val uiState = _uiState.asStateFlow()

    fun loadGroupDetails(groupId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            getGroupDetailsUseCase(groupId).collect { result ->
                result.onSuccess { group ->

                    // =================================================================
                    // ===== CÓDIGO TEMPORÁRIO PARA SE TORNAR ADMIN ====================
                    // =================================================================
                    val currentUserId = auth.currentUser?.uid
                    if (currentUserId != null) {
                        Log.d("AdminFix", "Tentando me promover a admin no grupo ${group.id}...")
                        val adminData = mapOf("isAdmin" to true)
                        try {
                            groupRepository.updateMemberData(group.id, currentUserId, adminData)
                            Log.d("AdminFix", "Comando para promover enviado com sucesso!")
                        } catch (e: Exception) {
                            Log.e("AdminFix", "Falha ao tentar me promover a admin", e)
                        }
                    }
                    // =================================================================
                    // ===== APAGUE OU COMENTE ESTE BLOCO DEPOIS DE USAR 1 VEZ =========
                    // =================================================================

                    val memberUserIds = group.members.keys.toList()
                    val membersAsNewUsers = memberUserIds.mapNotNull { userId ->
                        getUserDataUseCase(userId).getOrNull()
                    }

                    val membersAsUsers = membersAsNewUsers.map { newUser ->
                        User(
                            userId = newUser.userId,
                            username = newUser.username,
                            profileUrl = newUser.profileUrl,
                            deviceToken = newUser.deviceToken
                        )
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentGroup = group,
                            groupMembers = membersAsUsers,
                            errorMessage = null
                        )
                    }
                }.onFailure { exception ->
                    // ... (código de falha)
                }
            }
        }
    }

    // ... (resto do seu ViewModel permanece igual)
    fun removeMember(userId: String) {
        val currentGroup = _uiState.value.currentGroup ?: return
        viewModelScope.launch {
            removeMemberUseCase(currentGroup.id, userId)
        }
    }

    fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: ""
    }

    fun getAdminIds(): List<String> {
        val group = uiState.value.currentGroup ?: return emptyList()
        return group.members.filter {
            (it.value["isAdmin"] as? Boolean) == true
        }.keys.toList()
    }

    fun isCurrentUserAdmin(): Boolean {
        val currentUserId = auth.currentUser?.uid ?: return false
        val group = uiState.value.currentGroup ?: return false
        val currentUserMemberData = group.members[currentUserId] ?: return false
        return (currentUserMemberData["isAdmin"] as? Boolean) == true
    }
}