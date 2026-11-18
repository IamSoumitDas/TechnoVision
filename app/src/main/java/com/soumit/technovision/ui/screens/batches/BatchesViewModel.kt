package com.soumit.technovision.ui.screens.batches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soumit.technovision.data.model.StudentModel
import com.soumit.technovision.data.repository.StudentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BatchesUiState(
    val studentList: List<StudentModel> = emptyList(),
    val selectedView: String = "Today",
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@dagger.hilt.android.lifecycle.HiltViewModel
class BatchesViewModel @javax.inject.Inject constructor(
    private val studentRepository: StudentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BatchesUiState())
    val uiState: StateFlow<BatchesUiState> = _uiState.asStateFlow()

    init {
        observeStudents()
    }

    private fun observeStudents() {
        viewModelScope.launch {
            studentRepository.getStudentsStream()
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Error: ${e.localizedMessage}") }
                }
                .collect { students ->
                    _uiState.update { it.copy(studentList = students, isLoading = false) }
                }
        }
    }

    fun onViewSelected(view: String) {
        _uiState.update { it.copy(selectedView = view) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
