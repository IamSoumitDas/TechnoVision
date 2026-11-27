package com.soumit.technovision

import com.soumit.technovision.data.model.AppUpdateInfo
import com.soumit.technovision.data.model.DownloadStatus
import com.soumit.technovision.data.model.GitHubReleaseDto
import com.soumit.technovision.data.repository.AppUpdateRepository
import com.soumit.technovision.data.repository.GitHubAppUpdateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppUpdateTest {

    private val testDispatcher = StandardTestDispatcher()
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testVersionComparison_datePattern_newerVersionDetected() {
        val repo = GitHubAppUpdateRepository(android.content.ContextWrapper(null))

        // Remote is newer date (26.11.27 vs 26.11.26)
        assertTrue(repo.isNewerVersion("v26.11.27", "26.11.26", 20261126))
        assertTrue(repo.isNewerVersion("26.12.01", "26.11.26", 20261126))
        assertTrue(repo.isNewerVersion("v27.01.01", "26.11.26", 20261126))

        // Same version
        assertFalse(repo.isNewerVersion("v26.11.26", "26.11.26", 20261126))
        assertFalse(repo.isNewerVersion("26.11.26", "26.11.26", 20261126))

        // Extract version from APK name test
        val extracted = repo.extractVersionFromApkName("TechnoVision_v26.11.27.apk")
        assertEquals("26.11.27", extracted)
        assertTrue(repo.isNewerVersion(extracted!!, "26.11.26", 20261126))

        // Older version
        assertFalse(repo.isNewerVersion("v26.11.25", "26.11.26", 20261126))
        assertFalse(repo.isNewerVersion("26.10.30", "26.11.26", 20261126))
    }

    @Test
    fun testVersionComparison_pureNumericCode_newerVersionDetected() {
        val repo = GitHubAppUpdateRepository(android.content.ContextWrapper(null))

        assertTrue(repo.isNewerVersion("20261127", "26.11.26", 20261126))
        assertFalse(repo.isNewerVersion("20261126", "26.11.26", 20261126))
        assertFalse(repo.isNewerVersion("20261125", "26.11.26", 20261126))
    }

    @Test
    fun testVersionComparison_semver_newerVersionDetected() {
        val repo = GitHubAppUpdateRepository(android.content.ContextWrapper(null))

        assertTrue(repo.isNewerVersion("v1.0.1", "1.0.0", 1))
        assertTrue(repo.isNewerVersion("v2.0.0", "1.9.9", 1))
        assertFalse(repo.isNewerVersion("v1.0.0", "1.0.0", 1))
        assertFalse(repo.isNewerVersion("v0.9.9", "1.0.0", 1))
    }

    @Test
    fun testGitHubReleaseJsonParsing() {
        val sampleJson = """
            {
                "tag_name": "v26.11.27",
                "name": "TechnoVision v26.11.27",
                "body": "Fixed attendance sync issue\nPerformance improvements",
                "assets": [
                    {
                        "name": "TechnoVision_v26.11.27.apk",
                        "browser_download_url": "https://github.com/IamSoumitDas/TechnoVision/releases/download/v26.11.27/TechnoVision_v26.11.27.apk",
                        "size": 15728640,
                        "content_type": "application/vnd.android.package-archive"
                    }
                ]
            }
        """.trimIndent()

        val release = json.decodeFromString<GitHubReleaseDto>(sampleJson)
        assertEquals("v26.11.27", release.tagName)
        assertEquals(1, release.assets.size)

        val asset = release.assets.first()
        assertEquals("TechnoVision_v26.11.27.apk", asset.name)
        assertTrue(asset.name.endsWith(".apk"))
        assertEquals(15728640L, asset.size)
    }

    @Test
    fun testMainViewModel_receivesUpdateAndControlsDialog() = runTest {
        val fakeUpdate = AppUpdateInfo(
            versionName = "26.11.27",
            changelog = "Bug fixes",
            downloadUrl = "https://example.com/test.apk",
            apkName = "TechnoVision_v26.11.27.apk",
            sizeBytes = 1024L
        )

        val fakeRepo = object : AppUpdateRepository {
            override val latestUpdateInfo: kotlinx.coroutines.flow.StateFlow<AppUpdateInfo?> =
                kotlinx.coroutines.flow.MutableStateFlow(fakeUpdate)
            override val downloadStatus: kotlinx.coroutines.flow.StateFlow<DownloadStatus> =
                kotlinx.coroutines.flow.MutableStateFlow(DownloadStatus.Idle)
            override suspend fun checkForUpdate(): Result<AppUpdateInfo?> = Result.success(fakeUpdate)
            override fun downloadApk(downloadUrl: String, apkName: String): Flow<DownloadStatus> =
                flowOf(DownloadStatus.Progress(512L, 1024L, 0.5f))
        }

        val fakeAuthRepo = object : com.soumit.technovision.data.repository.AuthRepository {
            override val currentUser: com.google.firebase.auth.FirebaseUser? = null
            override fun authStateStream(): Flow<com.google.firebase.auth.FirebaseUser?> = flowOf(null)
            override suspend fun getUserRole(userId: String): Result<String> = Result.success("owner")
            override suspend fun getCurrentUserName(): Result<String> = Result.success("Admin")
            override suspend fun signIn(email: String, password: String): Result<String> = Result.success("owner")
            override suspend fun sendPasswordReset(email: String): Result<Unit> = Result.success(Unit)
            override fun signOut() {}
        }

        val viewModel = MainViewModel(
            authRepository = fakeAuthRepo,
            appUpdateRepository = fakeRepo
        )

        // Wait for coroutine in init
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.showUpdateDialog)
        assertEquals(fakeUpdate, state.updateInfo)

        viewModel.startUpdateDownload()
        testScheduler.advanceUntilIdle()

        val downloadingState = viewModel.uiState.value
        assertTrue(downloadingState.downloadStatus is DownloadStatus.Progress)

        viewModel.dismissUpdateDialog()
        assertFalse(viewModel.uiState.value.showUpdateDialog)
    }
}
