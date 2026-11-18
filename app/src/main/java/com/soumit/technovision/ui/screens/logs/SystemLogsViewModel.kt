package com.soumit.technovision.ui.screens.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soumit.technovision.data.model.LogModel
import com.soumit.technovision.data.repository.AuditLogRepository
import com.soumit.technovision.data.repository.StaffRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SystemLogsUiState(
    val logs: List<LogModel> = emptyList(),
    val userMap: Map<String, String> = emptyMap(),
    val isLoading: Boolean = true,
    val showAllHistory: Boolean = false,
    val selectedCustomDate: String? = null,
    val selectedTimeSlot: String = "All Slots",
    val selectedCategory: String = "All",
    val errorMessage: String? = null
)

@dagger.hilt.android.lifecycle.HiltViewModel
class SystemLogsViewModel @javax.inject.Inject constructor(
    private val auditLogRepository: AuditLogRepository,
    private val staffRepository: StaffRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SystemLogsUiState())
    val uiState: StateFlow<SystemLogsUiState> = _uiState.asStateFlow()

    init {
        observeLogs()
    }

    private fun observeLogs() {
        viewModelScope.launch {
            combine(
                auditLogRepository.getLogsStream(),
                staffRepository.getUsersMapStream()
            ) { logs, users ->
                Pair(logs, users)
            }.catch { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = "Error: ${e.localizedMessage}") }
            }.collect { (logs, users) ->
                _uiState.update {
                    it.copy(
                        logs = logs,
                        userMap = users,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun setShowAllHistory(showAll: Boolean) {
        _uiState.update {
            it.copy(
                showAllHistory = showAll,
                selectedCustomDate = if (showAll) null else it.selectedCustomDate
            )
        }
    }

    fun setSelectedCustomDate(date: String?) {
        _uiState.update {
            it.copy(
                selectedCustomDate = date,
                showAllHistory = if (date != null) false else it.showAllHistory
            )
        }
    }

    fun setSelectedTimeSlot(slot: String) {
        _uiState.update { it.copy(selectedTimeSlot = slot) }
    }

    fun setSelectedCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
