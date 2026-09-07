package com.wapp.paperflipai.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * Animation presets — the Android counterpart of `PFMotion.swift`.
 *
 * SwiftUI springs are expressed as (response, dampingFraction); Compose
 * springs as (dampingRatio, stiffness). Stiffness here is derived with
 * `k = (2π / response)²` so the two platforms land on visually identical
 * motion.
 */
object PFMotion {

    val SmoothEasing: Easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)

    /** 0.4s smooth — default for content swaps, sheet transitions. */
    fun <T> smooth(durationMillis: Int = 400): TweenSpec<T> =
        tween(durationMillis, easing = SmoothEasing)

    /** Slow and gentle — onboarding parallax, hero animations. */
    fun <T> gentle(durationMillis: Int = 800): TweenSpec<T> =
        tween(durationMillis, easing = FastOutSlowInEasing)

    /** Slightly springy — buttons, toggles, small UI feedback. (response 0.32) */
    fun <T> snappy(): SpringSpec<T> = spring(dampingRatio = 0.78f, stiffness = 386f)

    /** Lively bounce — celebrations, success states. (response 0.42) */
    fun <T> bouncy(): SpringSpec<T> = spring(dampingRatio = 0.62f, stiffness = 224f)

    /** Card swipe — quick to respond, soft landing. (response 0.34) */
    fun <T> cardSwipe(): SpringSpec<T> = spring(dampingRatio = 0.74f, stiffness = 341f)

    /** Card flip — slightly slower so the rotation reads. (response 0.55) */
    fun <T> cardFlip(): SpringSpec<T> = spring(dampingRatio = 0.72f, stiffness = 130f)
}
