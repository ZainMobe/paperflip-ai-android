package com.wapp.paperflipai.app

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import com.wapp.paperflipai.core.Secrets
import com.wapp.paperflipai.core.auth.AuthService
import com.wapp.paperflipai.core.auth.GoogleSignInCoordinator
import com.wapp.paperflipai.core.auth.MockAuthService
import com.wapp.paperflipai.core.auth.SessionStore
import com.wapp.paperflipai.core.data.PaperflipDatabase
import com.wapp.paperflipai.core.data.PaperflipRepository
import com.wapp.paperflipai.core.monetization.EntitlementStore
import com.wapp.paperflipai.core.monetization.MockEntitlementStore
import com.wapp.paperflipai.core.network.MockRemoteStore
import com.wapp.paperflipai.core.network.SupabaseRemoteStore
import com.wapp.paperflipai.core.network.SupabaseClient
import com.wapp.paperflipai.core.auth.SupabaseAuthService
import com.wapp.paperflipai.core.network.NetworkMonitor
import com.wapp.paperflipai.core.network.RemoteStore
import com.wapp.paperflipai.core.notifications.NotificationsClient
import com.wapp.paperflipai.core.settings.SettingsStore
import com.wapp.paperflipai.core.srs.StreakTracker
import com.wapp.paperflipai.core.srs.StudyQueue
import com.wapp.paperflipai.core.sync.SyncEngine
import com.wapp.paperflipai.widget.WidgetSnapshotStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Single source of truth for app-wide services — the Android counterpart of
 * `AppEnvironment.swift`.
 *
 * Constructed once by [com.wapp.paperflipai.PaperflipApplication] and read
 * from composables through [LocalAppEnvironment]. Each service picks its real
 * implementation when the matching secret is present and silently falls back
 * to a mock otherwise, so the app always runs.
 */
class AppEnvironment(context: Context) {

    val appContext: Context = context.applicationContext

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val settings = SettingsStore(appContext)
    val sessionStore = SessionStore(appContext)
    val database = PaperflipDatabase(appContext, scope)
    val networkMonitor = NetworkMonitor(appContext)
    val streaks = StreakTracker(appContext)
    val notifications = NotificationsClient(appContext, settings)
    val google = GoogleSignInCoordinator(appContext)

    /**
     * Live services when `local.properties` carries the Supabase config,
     * Mocks otherwise — so a fresh clone still runs, fully offline, exactly
     * like the iOS target does without `Config.xcconfig`.
     *
     * Auth is constructed first because the REST client resolves its bearer
     * token from it on every request (and refreshes an expiring one there),
     * which is why [supabaseAuth] is held separately rather than being read
     * back off the [AuthService] interface.
     */
    private val supabaseAuth: SupabaseAuthService? =
        if (Secrets.hasSupabaseConfig) {
            SupabaseAuthService(Secrets.supabaseUrl!!, Secrets.supabaseAnonKey!!, sessionStore)
        } else {
            null
        }

    val auth: AuthService = supabaseAuth ?: MockAuthService(sessionStore)

    val remote: RemoteStore =
        if (supabaseAuth != null) {
            SupabaseRemoteStore(
                SupabaseClient(
                    baseUrl = Secrets.supabaseUrl!!,
                    anonKey = Secrets.supabaseAnonKey!!,
                    accessToken = { supabaseAuth.validAccessToken() },
                )
            )
        } else {
            MockRemoteStore()
        }

    // Still mocked: Google Play Billing / RevenueCat has no implementation
    // yet, so entitlement stays local. Everything else is live.
    val entitlements: EntitlementStore = MockEntitlementStore(appContext)

    val sync = SyncEngine(remote, database, networkMonitor)
    val repository = PaperflipRepository(database, remote)

    /** True until services have completed their cold-start work. */
    private val _isBootstrapping = MutableStateFlow(true)
    val isBootstrapping: StateFlow<Boolean> = _isBootstrapping.asStateFlow()

    fun bootstrap() {
        Secrets.logConfigStatus()
        database.start()
        networkMonitor.start()
        notifications.applyReminderState()

        scope.launch {
            entitlements.configure()
            auth.restoreSession()
            auth.session.value?.userId?.let { userId ->
                sync.pullAll(userId)
            }
            // A short beat so the launch animation doesn't blink out.
            delay(250)
            _isBootstrapping.value = false
        }
    }

    /** Recomputes the home-screen widget payload from the current store. */
    fun refreshWidget() {
        scope.launch {
            // Re-evaluate the streak against the current clock first: this
            // runs on every resume, so it is where a streak that lapsed
            // overnight stops being advertised on the widget.
            streaks.refresh()
            val cards = database.snapshot.value.cards
            WidgetSnapshotStore.update(
                context = appContext,
                dueToday = StudyQueue.dueCount(cards),
                streakDays = streaks.count.value,
                lastStudiedAt = streaks.lastStudyDate.value,
            )
        }
    }

    /** Clears every trace of the signed-in user. */
    fun onSignedOut() {
        database.clear()
        streaks.reset()
        sessionStore.clear()
    }
}

val LocalAppEnvironment = staticCompositionLocalOf<AppEnvironment> {
    error("AppEnvironment not provided. Wrap your content in PaperflipApp { }.")
}
