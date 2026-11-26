package com.soumit.technovision

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soumit.technovision.data.repository.AuthRepository
import com.soumit.technovision.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val startDestination: Any? = null,
    val isLoadingRole: Boolean = true,
    val isLoadingData: Boolean = false,
    val loadingMessage: String = "Loading...",
    val isOnline: Boolean = true,
    val updateInfo: com.soumit.technovision.data.model.AppUpdateInfo? = null,
    val downloadStatus: com.soumit.technovision.data.model.DownloadStatus = com.soumit.technovision.data.model.DownloadStatus.Idle,
    val showUpdateDialog: Boolean = false
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val studentRepository: com.soumit.technovision.data.repository.StudentRepository? = null,
    private val attendanceRepository: com.soumit.technovision.data.repository.AttendanceRepository? = null,
    private val feeRepository: com.soumit.technovision.data.repository.FeeRepository? = null,
    private val deltaSyncManager: com.soumit.technovision.data.local.DeltaSyncManager? = null,
    private val networkMonitor: com.soumit.technovision.util.NetworkMonitor? = null,
    private val appUpdateRepository: com.soumit.technovision.data.repository.AppUpdateRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        determineStartDestination()
        observeNetwork()
        checkForAppUpdate()
    }

    private fun checkForAppUpdate() {
        if (appUpdateRepository == null) return
        viewModelScope.launch {
            val result = appUpdateRepository.checkForUpdate()
            result.getOrNull()?.let { updateInfo ->
                _uiState.update {
                    it.copy(
                        updateInfo = updateInfo,
                        showUpdateDialog = true
                    )
                }
            }
        }
    }

    fun startUpdateDownload() {
        val info = _uiState.value.updateInfo ?: return
        val repo = appUpdateRepository ?: return
        viewModelScope.launch {
            repo.downloadApk(info.downloadUrl, info.apkName).collect { status ->
                _uiState.update { it.copy(downloadStatus = status) }
            }
        }
    }

    fun dismissUpdateDialog() {
        _uiState.update { it.copy(showUpdateDialog = false) }
    }

    private fun observeNetwork() {
        if (networkMonitor == null) return
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                _uiState.update { it.copy(isOnline = online) }
            }
        }
    }

    private fun determineStartDestination() {
        val currentUser = authRepository.currentUser
        if (currentUser == null) {
            _uiState.update { it.copy(startDestination = Screen.Auth, isLoadingRole = false, isLoadingData = false) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingData = true, loadingMessage = "Setting up your workspace...") }

            // 1. Determine user role
            val roleResult = authRepository.getUserRole(currentUser.uid)
            val destination: Any = if (roleResult.isSuccess) {
                when (roleResult.getOrThrow().lowercase()) {
                    "owner" -> Screen.OwnerHome
                    "faculty" -> Screen.FacultyHome
                    "receptionist" -> Screen.ReceptionistHome
                    else -> Screen.Auth
                }
            } else {
                Screen.Auth
            }

            // 2. Pre-warm local database records into memory
            _uiState.update { it.copy(loadingMessage = "Loading records into memory...") }
            runCatching {
                studentRepository?.prewarm()
                attendanceRepository?.prewarm()
                feeRepository?.prewarm()
            }

            // 3. Catch-up delta sync with Firestore bounded by timeout
            _uiState.update { it.copy(loadingMessage = "Syncing latest updates...") }
            kotlinx.coroutines.withTimeoutOrNull(2500L) {
                runCatching {
                    deltaSyncManager?.syncAll(force = false)
                }
            }

            // 4. Re-prewarm to ensure newly fetched records are in RAM
            runCatching {
                studentRepository?.prewarm()
                attendanceRepository?.prewarm()
                feeRepository?.prewarm()
            }

            _uiState.update {
                it.copy(
                    startDestination = destination,
                    isLoadingRole = false,
                    isLoadingData = false
                )
            }
        }
    }
}
