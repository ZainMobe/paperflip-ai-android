package com.wapp.paperflipai

import android.app.Application
import com.wapp.paperflipai.app.AppEnvironment

/**
 * Owns the [AppEnvironment] for the process. The activity, the widget and
 * the broadcast receivers all reach the same services through here — the
 * Android counterpart of the `@State private var environment` the iOS
 * `Paperflip_AIApp` holds.
 */
class PaperflipApplication : Application() {

    lateinit var environment: AppEnvironment
        private set

    override fun onCreate() {
        super.onCreate()
        environment = AppEnvironment(this)
        environment.bootstrap()
    }
}
