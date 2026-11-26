package com.soumit.technovision.ui.screens.update

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soumit.technovision.data.model.AppUpdateInfo
import com.soumit.technovision.data.model.DownloadStatus
import com.soumit.technovision.data.repository.AppUpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppUpdateUiState(
    val currentVersionName: String = "",
    val currentVersionCode: Long = 0L,
    val isChecking: Boolean = false,
    val updateInfo: AppUpdateInfo? = null,
    val isUpToDate: Boolean = false,
    val downloadStatus: DownloadStatus = DownloadStatus.Idle,
    val errorMessage: String? = null
)

@HiltViewModel
class AppUpdateViewModel @Inject constructor(
    private val appUpdateRepository: AppUpdateRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppUpdateUiState())
    val uiState: StateFlow<AppUpdateUiState> = _uiState.asStateFlow()

    init {
        loadCurrentVersion()
        observeRepository()
        checkForUpdates(silent = false)
    }

    private fun loadCurrentVersion() {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        val versionName = packageInfo.versionName ?: "1.0.0"
        val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
        _uiState.update {
            it.copy(
                currentVersionName = versionName,
                currentVersionCode = versionCode
            )
        }
    }

    private fun observeRepository() {
        viewModelScope.launch {
            appUpdateRepository.latestUpdateInfo.collect { info ->
                if (info != null) {
                    _uiState.update { it.copy(updateInfo = info, isUpToDate = false) }
                }
            }
        }
        viewModelScope.launch {
            appUpdateRepository.downloadStatus.collect { status ->
                _uiState.update { it.copy(downloadStatus = status) }
            }
        }
    }

    fun checkForUpdates(silent: Boolean = false) {
        if (_uiState.value.isChecking) return
        viewModelScope.launch {
            _uiState.update { it.copy(isChecking = true, errorMessage = null) }
            val result = appUpdateRepository.checkForUpdate()
            result.fold(
                onSuccess = { info ->
                    _uiState.update {
                        it.copy(
                            isChecking = false,
                            updateInfo = info,
                            isUpToDate = (info == null)
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isChecking = false,
                            errorMessage = if (silent) null else (error.message ?: "Failed to check for updates")
                        )
                    }
                }
            )
        }
    }

    fun startDownload() {
        val info = _uiState.value.updateInfo ?: return
        viewModelScope.launch {
            appUpdateRepository.downloadApk(info.downloadUrl, info.apkName).collect()
        }
    }
}
