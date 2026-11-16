package com.soumit.technovision

import android.app.Application
import com.soumit.technovision.di.AppContainer
import com.soumit.technovision.di.DefaultAppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TechnoVisionApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        try {
            val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(
                    com.google.firebase.firestore.PersistentCacheSettings.newBuilder()
                        .setSizeBytes(com.google.firebase.firestore.FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                        .build()
                )
                .build()
            com.google.firebase.firestore.FirebaseFirestore.getInstance().firestoreSettings = settings
        } catch (_: Exception) {
            // In case firestore settings were already set or during tests
        }

        container = DefaultAppContainer(this)

        // Non-blocking background sync of deltas (only reads documents modified since last sync)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                container.deltaSyncManager.syncAll()
            } catch (_: Exception) {
                // Offline or sync error handled gracefully
            }
        }
    }

    companion object {
        var instance: TechnoVisionApplication? = null
            private set
    }
}
