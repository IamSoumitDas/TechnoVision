package com.soumit.technovision

import android.app.Application
import com.soumit.technovision.di.AppContainer
import com.soumit.technovision.di.DefaultAppContainer

class TechnoVisionApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        container = DefaultAppContainer()
    }

    companion object {
        var instance: TechnoVisionApplication? = null
            private set
    }
}
