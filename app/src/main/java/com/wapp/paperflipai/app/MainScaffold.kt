package com.wapp.paperflipai.app

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wapp.paperflipai.R
import com.wapp.paperflipai.core.data.QuizMode
import com.wapp.paperflipai.designsystem.PFIcons
import com.wapp.paperflipai.designsystem.modifier.PFHaptic
import com.wapp.paperflipai.designsystem.modifier.pfPressable
import com.wapp.paperflipai.designsystem.theme.PFRadius
import com.wapp.paperflipai.designsystem.theme.PFTheme
import com.wapp.paperflipai.feature.importer.ImportHubScreen
import com.wapp.paperflipai.feature.importer.ImportReviewScreen
import com.wapp.paperflipai.feature.importer.PasteCardsScreen
import com.wapp.paperflipai.feature.importer.SourceInputScreen
import com.wapp.paperflipai.feature.library.DeckDetailScreen
import com.wapp.paperflipai.feature.library.LibraryHomeScreen
import com.wapp.paperflipai.feature.notifications.NotificationsInboxScreen
import com.wapp.paperflipai.feature.paywall.PaywallScreen
import com.wapp.paperflipai.feature.paywall.ProUnlockedScreen
import com.wapp.paperflipai.feature.projects.PendingInvitesScreen
import com.wapp.paperflipai.feature.projects.ProjectDetailScreen
import com.wapp.paperflipai.feature.quiz.MatchGameScreen
import com.wapp.paperflipai.feature.quiz.QuizConfiguration
import com.wapp.paperflipai.feature.quiz.QuizSessionScreen
import com.wapp.paperflipai.feature.settings.AppearanceSettingsScreen
import com.wapp.paperflipai.feature.settings.DeleteAccountScreen
import com.wapp.paperflipai.feature.settings.EditProfileScreen
import com.wapp.paperflipai.feature.settings.LanguageSettingsScreen
import com.wapp.paperflipai.feature.settings.ManageSubscriptionScreen
import com.wapp.paperflipai.feature.settings.NotificationSettingsScreen
import com.wapp.paperflipai.feature.settings.SettingsHomeScreen
import com.wapp.paperflipai.feature.stats.StatsHomeScreen
import com.wapp.paperflipai.feature.study.StudyHomeScreen
import com.wapp.paperflipai.feature.study.StudySessionScreen
import com.wapp.paperflipai.feature.support.SupportChatScreen
import androidx.compose.runtime.DisposableEffect
import com.wapp.paperflipai.core.analytics.AnalyticsEvent
import com.wapp.paperflipai.core.analytics.PFAnalytics
import com.wapp.paperflipai.core.analytics.PaywallSource

/**
 * The post-auth root — the Android counterpart of `RootTabBar.swift`.
 *
 * A single [NavHost] holds every authenticated destination; the bottom bar
 * shows only on the five top-level tabs and slides away for detail screens,
 * which is the Android convention (iOS keeps its tab bar pinned).
 */
@Composable
fun MainScaffold() {
    val env = LocalAppEnvironment.current
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in Route.topLevel

    DisposableEffect(currentRoute) {
        val name = currentRoute?.let(::screenNameFor)
        name?.let { PFAnalytics.screen(it, screenClassFor(currentRoute)) }
        onDispose { name?.let(PFAnalytics::screenLeft) }
    }

    // Deep links, notification taps and share hand-offs land here.
    val pendingAction by IntentInbox.pending.collectAsStateWithLifecycle()
    LaunchedEffect(pendingAction) {
        when (val action = pendingAction) {
            null -> Unit
            is PFAction.StartStudy -> {
                navController.navigateToTab(Route.STUDY)
                IntentInbox.clear()
            }
            is PFAction.OpenImport -> {
                navController.navigateToTab(Route.IMPORT)
                IntentInbox.clear()
            }
            is PFAction.OpenDeck -> {
                navController.navigate(Route.deck(action.deckId))
                IntentInbox.clear()
            }
            is PFAction.OpenProject -> {
                navController.navigate(Route.project(action.projectId))
                IntentInbox.clear()
            }
            is PFAction.JoinProject, is PFAction.ImportSharedDeck -> {
                // LibraryHome presents the matching sheet; it clears the inbox.
                navController.navigateToTab(Route.LIBRARY)
            }
            is PFAction.ImportSharedText, is PFAction.ImportSharedPdf -> {
                navController.navigateToTab(Route.IMPORT)
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Route.LIBRARY,
            modifier = Modifier.fillMaxSize(),
            enterTransition = {
                if (initialState.destination.route in Route.topLevel &&
                    targetState.destination.route in Route.topLevel
                ) {
                    fadeIn(tween(220))
                } else {
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(320)) +
                        fadeIn(tween(220))
                }
            },
            exitTransition = {
                if (initialState.destination.route in Route.topLevel &&
                    targetState.destination.route in Route.topLevel
                ) {
                    fadeOut(tween(160))
                } else {
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(320)) +
                        fadeOut(tween(180))
                }
            },
            popEnterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(320)) +
                    fadeIn(tween(220))
            },
            popExitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(320)) +
                    fadeOut(tween(180))
            },
        ) {
            // ── Tabs ──────────────────────────────────────────────────
            composable(Route.LIBRARY) {
                LibraryHomeScreen(
                    onOpenDeck = { navController.navigate(Route.deck(it)) },
                    onOpenProject = { navController.navigate(Route.project(it)) },
                    onOpenImport = { navController.navigateToTab(Route.IMPORT) },
                    onOpenNotifications = { navController.navigate(Route.NOTIFICATIONS_INBOX) },
                    onOpenInvites = { navController.navigate(Route.PENDING_INVITES) },
                    onOpenPaywall = { navController.navigate(Route.paywall(it)) },
                )
            }
            composable(Route.STUDY) {
                StudyHomeScreen(
                    onStartSession = { deckId ->
                        navController.navigate(Route.studySession(deckId = deckId))
                    },
                    onOpenImport = { navController.navigateToTab(Route.IMPORT) },
                    onOpenDeck = { navController.navigate(Route.deck(it)) },
                )
            }
            composable(Route.IMPORT) {
                ImportHubScreen(
                    onPickSource = { type, shared ->
                        PFAnalytics.log(AnalyticsEvent.importSourceSelected(type))
                        navController.navigate(Route.importSource(type, shared))
                    },
                    onOpenPaywall = { navController.navigate(Route.paywall(it)) },
                    onOpenPasteCards = {
                        PFAnalytics.log(AnalyticsEvent.importSourceSelected("paste_cards"))
                        navController.navigate(Route.IMPORT_PASTE)
                    },
                    onOpenDraftReview = { navController.navigate(Route.IMPORT_REVIEW) },
                )
            }
            composable(Route.STATS) {
                StatsHomeScreen(
                    onOpenDeck = { navController.navigate(Route.deck(it)) },
                    onStartSession = { navController.navigate(Route.studySession()) },
                )
            }
            composable(Route.SETTINGS) {
                SettingsHomeScreen(
                    onNavigate = { route -> navController.navigate(route) },
                )
            }

            // ── Details ───────────────────────────────────────────────
            composable(
                route = Route.DECK,
                arguments = listOf(navArgument("deckId") { type = NavType.StringType }),
            ) { entry ->
                DeckDetailScreen(
                    deckId = entry.arguments?.getString("deckId").orEmpty(),
                    onBack = { navController.popBackStack() },
                    onStudy = { deckId ->
                        navController.navigate(Route.studySession(deckId = deckId))
                    },
                    onOpenPaywall = { navController.navigate(Route.paywall(it)) },
                    onOpenProject = { navController.navigate(Route.project(it)) },
                    onStartQuiz = { deckId, config ->
                        navController.navigate(
                            Route.quiz(deckId, config.mode.raw, config.questionCount, config.timeLimitSeconds)
                        )
                    },
                    onStartMatch = { deckId -> navController.navigate(Route.match(deckId)) },
                )
            }

            composable(
                route = Route.PROJECT,
                arguments = listOf(navArgument("projectId") { type = NavType.StringType }),
            ) { entry ->
                ProjectDetailScreen(
                    projectId = entry.arguments?.getString("projectId").orEmpty(),
                    onBack = { navController.popBackStack() },
                    onOpenDeck = { navController.navigate(Route.deck(it)) },
                    onOpenPaywall = { navController.navigate(Route.paywall(PaywallSource.Unknown)) },
                )
            }

            composable(
                route = Route.STUDY_SESSION,
                arguments = listOf(
                    navArgument("deckId") { type = NavType.StringType; defaultValue = "" },
                    navArgument("projectId") { type = NavType.StringType; defaultValue = "" },
                ),
            ) { entry ->
                StudySessionScreen(
                    deckId = entry.arguments?.getString("deckId").orEmpty().ifBlank { null },
                    onFinish = { navController.popBackStack() },
                )
            }

            composable(
                route = Route.IMPORT_SOURCE,
                arguments = listOf(
                    navArgument("sourceType") { type = NavType.StringType },
                    navArgument("shared") { type = NavType.StringType; defaultValue = "" },
                ),
            ) { entry ->
                SourceInputScreen(
                    sourceTypeRaw = entry.arguments?.getString("sourceType").orEmpty(),
                    sharedValue = Route.decode(entry.arguments?.getString("shared")).ifBlank { null },
                    onBack = { navController.popBackStack() },
                    onOpenDeck = { deckId ->
                        navController.popBackStack()
                        navController.navigate(Route.deck(deckId))
                    },
                    onOpenPaywall = { navController.navigate(Route.paywall(it)) },
                )
            }

            composable(Route.IMPORT_PASTE) {
                PasteCardsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenDeck = { deckId ->
                        navController.popBackStack()
                        navController.navigate(Route.deck(deckId))
                    },
                    onOpenPaywall = { navController.navigate(Route.paywall(PaywallSource.ReviewProjectChip)) },
                )
            }

            composable(Route.IMPORT_REVIEW) {
                ImportReviewScreen(
                    onBack = { navController.popBackStack() },
                    onOpenDeck = { deckId ->
                        navController.popBackStack()
                        navController.navigate(Route.deck(deckId))
                    },
                    onOpenPaywall = { navController.navigate(Route.paywall(PaywallSource.ReviewProjectChip)) },
                )
            }

            // ── Quizzes ───────────────────────────────────────────────
            composable(
                route = Route.QUIZ,
                arguments = listOf(
                    navArgument("deckId") { type = NavType.StringType },
                    navArgument("mode") { type = NavType.StringType; defaultValue = "quiz" },
                    navArgument("count") { type = NavType.IntType; defaultValue = 0 },
                    navArgument("time") { type = NavType.IntType; defaultValue = 0 },
                ),
            ) { entry ->
                QuizSessionScreen(
                    deckId = entry.arguments?.getString("deckId").orEmpty(),
                    configuration = QuizConfiguration(
                        mode = QuizMode.from(entry.arguments?.getString("mode")),
                        questionCount = entry.arguments?.getInt("count")?.takeIf { it > 0 },
                        timeLimitSeconds = entry.arguments?.getInt("time")?.takeIf { it > 0 },
                    ),
                    onFinish = { navController.popBackStack() },
                )
            }

            composable(
                route = Route.MATCH,
                arguments = listOf(navArgument("deckId") { type = NavType.StringType }),
            ) { entry ->
                MatchGameScreen(
                    deckId = entry.arguments?.getString("deckId").orEmpty(),
                    onFinish = { navController.popBackStack() },
                )
            }

            // ── Settings sub-screens ──────────────────────────────────
            composable(Route.SETTINGS_PROFILE) {
                EditProfileScreen(
                    onBack = { navController.popBackStack() },
                    onOpenDeleteAccount = { navController.navigate(Route.SETTINGS_DELETE) },
                )
            }
            composable(Route.SETTINGS_APPEARANCE) {
                AppearanceSettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(Route.SETTINGS_LANGUAGE) {
                LanguageSettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(Route.SETTINGS_NOTIFICATIONS) {
                NotificationSettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(Route.SETTINGS_SUBSCRIPTION) {
                ManageSubscriptionScreen(
                    onBack = { navController.popBackStack() },
                    onOpenPaywall = { navController.navigate(Route.paywall(PaywallSource.ManageSubscription)) },
                )
            }
            composable(Route.SETTINGS_DELETE) {
                DeleteAccountScreen(onBack = { navController.popBackStack() })
            }

            // ── Standalone ────────────────────────────────────────────
            composable(
                route = Route.PAYWALL,
                arguments = listOf(navArgument("source") { type = NavType.StringType; defaultValue = "" }),
            ) { entry ->
                PaywallScreen(
                    source = PaywallSource.from(entry.arguments?.getString("source")),
                    onClose = { navController.popBackStack() },
                    onPurchased = {
                        navController.popBackStack()
                        navController.navigate(Route.PRO_UNLOCKED)
                    },
                )
            }
            composable(Route.PRO_UNLOCKED) {
                ProUnlockedScreen(onDone = { navController.popBackStack() })
            }
            composable(Route.NOTIFICATIONS_INBOX) {
                NotificationsInboxScreen(
                    onBack = { navController.popBackStack() },
                    onOpenLink = { action ->
                        when (action) {
                            is PFAction.OpenDeck -> navController.navigate(Route.deck(action.deckId))
                            is PFAction.OpenProject -> navController.navigate(Route.project(action.projectId))
                            else -> Unit
                        }
                    },
                )
            }
            composable(Route.SUPPORT) {
                SupportChatScreen(onBack = { navController.popBackStack() })
            }
            composable(Route.PENDING_INVITES) {
                PendingInvitesScreen(
                    onBack = { navController.popBackStack() },
                    onOpenProject = { navController.navigate(Route.project(it)) },
                )
            }
        }

        AnimatedVisibility(
            visible = showBottomBar,
            enter = slideInVertically(tween(260)) { it } + fadeIn(tween(200)),
            exit = slideOutVertically(tween(220)) { it } + fadeOut(tween(160)),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            PFBottomBar(
                currentRoute = currentRoute,
                onSelect = {
                    PFAnalytics.log(AnalyticsEvent.tabSelected(tabAnalyticsName(it)))
                    navController.navigateToTab(it)
                },
            )
        }
    }
}

private fun tabAnalyticsName(route: String): String = when (route) {
    Route.IMPORT -> "importer"
    else -> route
}

private fun screenNameFor(route: String): String = when (route) {
    Route.LIBRARY -> "library"
    Route.STUDY -> "study_home"
    Route.IMPORT -> "import_hub"
    Route.STATS -> "stats"
    Route.SETTINGS -> "settings"
    Route.DECK -> "deck_detail"
    Route.PROJECT -> "project_detail"
    Route.STUDY_SESSION -> "study_session"
    Route.IMPORT_SOURCE -> "import_source_input"
    Route.IMPORT_PASTE -> "import_paste_cards"
    Route.IMPORT_REVIEW -> "import_review"
    Route.QUIZ -> "quiz_session"
    Route.MATCH -> "match_game"
    Route.SETTINGS_PROFILE -> "settings_profile"
    Route.SETTINGS_APPEARANCE -> "settings_appearance"
    Route.SETTINGS_LANGUAGE -> "settings_language"
    Route.SETTINGS_NOTIFICATIONS -> "settings_notifications"
    Route.SETTINGS_SUBSCRIPTION -> "settings_subscription"
    Route.SETTINGS_DELETE -> "settings_delete_account"
    Route.PAYWALL -> "paywall"
    Route.PRO_UNLOCKED -> "pro_unlocked"
    Route.NOTIFICATIONS_INBOX -> "notifications_inbox"
    Route.SUPPORT -> "support_chat"
    Route.PENDING_INVITES -> "pending_invites"
    else -> route.substringBefore('/').substringBefore('?')
}

private fun screenClassFor(route: String): String = when (route) {
    Route.LIBRARY -> "LibraryHomeScreen"
    Route.STUDY -> "StudyHomeScreen"
    Route.IMPORT -> "ImportHubScreen"
    Route.STATS -> "StatsHomeScreen"
    Route.SETTINGS -> "SettingsHomeScreen"
    Route.DECK -> "DeckDetailScreen"
    Route.PROJECT -> "ProjectDetailScreen"
    Route.STUDY_SESSION -> "StudySessionScreen"
    Route.IMPORT_SOURCE -> "SourceInputScreen"
    Route.IMPORT_PASTE -> "PasteCardsScreen"
    Route.IMPORT_REVIEW -> "ImportReviewScreen"
    Route.QUIZ -> "QuizSessionScreen"
    Route.MATCH -> "MatchGameScreen"
    Route.SETTINGS_PROFILE -> "EditProfileScreen"
    Route.SETTINGS_APPEARANCE -> "AppearanceSettingsScreen"
    Route.SETTINGS_LANGUAGE -> "LanguageSettingsScreen"
    Route.SETTINGS_NOTIFICATIONS -> "NotificationSettingsScreen"
    Route.SETTINGS_SUBSCRIPTION -> "ManageSubscriptionScreen"
    Route.SETTINGS_DELETE -> "DeleteAccountScreen"
    Route.PAYWALL -> "PaywallScreen"
    Route.PRO_UNLOCKED -> "ProUnlockedScreen"
    Route.NOTIFICATIONS_INBOX -> "NotificationsInboxScreen"
    Route.SUPPORT -> "SupportChatScreen"
    Route.PENDING_INVITES -> "PendingInvitesScreen"
    else -> screenNameFor(route)
}

/** Standard "switch tab" behaviour: single instance, restore state. */
private fun NavController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private data class TabItem(val route: String, val icon: ImageVector, val labelRes: Int)

private val tabs = listOf(
    TabItem(Route.LIBRARY, PFIcons.Library, R.string.library_2),
    TabItem(Route.STUDY, PFIcons.Study, R.string.study),
    TabItem(Route.IMPORT, PFIcons.Import, R.string.import_action),
    TabItem(Route.STATS, PFIcons.Stats, R.string.stats),
    TabItem(Route.SETTINGS, PFIcons.Settings, R.string.settings),
)

@Composable
private fun PFBottomBar(currentRoute: String?, onSelect: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PFTheme.colors.elevated),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(0.7.dp)
                .background(PFTheme.colors.divider)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(64.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                val selected = currentRoute == tab.route
                PFBottomBarItem(
                    tab = tab,
                    selected = selected,
                    onClick = { onSelect(tab.route) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun PFBottomBarItem(
    tab: TabItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint = if (selected) PFTheme.colors.accent else PFTheme.colors.onSurfaceMuted
    Column(
        modifier = modifier
            .fillMaxSize()
            .pfPressable(
                onClick = onClick,
                haptic = PFHaptic.Selection,
                pressedScale = 0.92f,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(PFRadius.pill))
                .background(
                    if (selected) PFTheme.colors.accentSoft
                    else androidx.compose.ui.graphics.Color.Transparent
                )
                .padding(horizontal = 16.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = stringResource(tab.labelRes),
            style = PFTheme.type.caption,
            color = tint,
            maxLines = 1,
        )
    }
}
