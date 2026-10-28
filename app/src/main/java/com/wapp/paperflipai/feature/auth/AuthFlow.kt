package com.wapp.paperflipai.feature.auth

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wapp.paperflipai.designsystem.theme.PFTheme
import java.net.URLDecoder
import java.net.URLEncoder
import com.wapp.paperflipai.core.analytics.PFAnalytics
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.navigation.compose.currentBackStackEntryAsState

/**
 * Wraps the auth screens in their own nav graph — the Android counterpart of
 * `AuthFlow.swift`. Keeps sign-in / sign-up / OTP / recovery navigation from
 * leaking into the root router, which only cares about "is there a session".
 */
private object AuthRoute {
    const val WELCOME = "auth/welcome"
    const val SIGN_IN = "auth/signin"
    const val SIGN_UP = "auth/signup"
    const val OTP = "auth/otp/{email}"
    const val FORGOT = "auth/forgot/{email}"

    fun otp(email: String) = "auth/otp/${email.encode()}"
    fun forgot(email: String) = "auth/forgot/${email.ifBlank { " " }.encode()}"

    private fun String.encode(): String = URLEncoder.encode(this, "UTF-8")
    fun decode(value: String?): String = runCatching {
        URLDecoder.decode(value.orEmpty(), "UTF-8")
    }.getOrDefault("").trim()
}

@Composable
fun AuthFlow() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    DisposableEffect(currentRoute) {
        val screen = when (currentRoute) {
            AuthRoute.WELCOME -> "auth_welcome" to "WelcomeAuthScreen"
            AuthRoute.SIGN_IN -> "auth_email_sign_in" to "EmailSignInScreen"
            AuthRoute.SIGN_UP -> "auth_email_sign_up" to "EmailSignUpScreen"
            AuthRoute.OTP -> "auth_otp_verify" to "OtpVerifyScreen"
            AuthRoute.FORGOT -> "auth_forgot_password" to "ForgotPasswordFlow"
            else -> null
        }
        screen?.let { (name, screenClass) -> PFAnalytics.screen(name, screenClass) }
        onDispose { screen?.let { PFAnalytics.screenLeft(it.first) } }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(PFTheme.colors.surface)
    ) {
        NavHost(
            navController = navController,
            startDestination = AuthRoute.WELCOME,
            enterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(320)) +
                    fadeIn(tween(220))
            },
            exitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(320)) +
                    fadeOut(tween(180))
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
            composable(AuthRoute.WELCOME) {
                WelcomeAuthScreen(onChooseEmail = { navController.navigate(AuthRoute.SIGN_IN) })
            }

            composable(AuthRoute.SIGN_IN) {
                EmailSignInScreen(
                    onBack = { navController.popBackStack() },
                    onChooseSignUp = { navController.navigate(AuthRoute.SIGN_UP) },
                    onCodeSent = { email -> navController.navigate(AuthRoute.otp(email)) },
                    onForgotPassword = { email -> navController.navigate(AuthRoute.forgot(email)) },
                )
            }

            composable(AuthRoute.SIGN_UP) {
                EmailSignUpScreen(onBack = { navController.popBackStack() })
            }

            composable(
                route = AuthRoute.OTP,
                arguments = listOf(navArgument("email") { type = NavType.StringType }),
            ) { entry ->
                OtpVerifyScreen(
                    email = AuthRoute.decode(entry.arguments?.getString("email")),
                    mode = OtpMode.SignIn,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                route = AuthRoute.FORGOT,
                arguments = listOf(navArgument("email") { type = NavType.StringType }),
            ) { entry ->
                ForgotPasswordFlow(
                    initialEmail = AuthRoute.decode(entry.arguments?.getString("email")),
                    onDone = { navController.popBackStack(AuthRoute.SIGN_IN, inclusive = false) },
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
