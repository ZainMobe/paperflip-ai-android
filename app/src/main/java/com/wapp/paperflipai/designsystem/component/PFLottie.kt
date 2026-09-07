package com.wapp.paperflipai.designsystem.component

import androidx.annotation.RawRes
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.wapp.paperflipai.R

/**
 * Thin wrapper around airbnb/lottie-compose — the Android counterpart of
 * `PFLottieView.swift` — so the rest of the app never imports Lottie
 * directly. Loads the same bundled JSON animations the iOS target ships,
 * copied into `res/raw`.
 *
 * Falls back to the static brand mark while the composition loads (or if
 * it fails), so a screen never renders an empty hole.
 */
@Composable
fun PFLottie(
    @RawRes resId: Int,
    modifier: Modifier = Modifier,
    iterations: Int = LottieConstants.IterateForever,
    speed: Float = 1f,
    isPlaying: Boolean = true,
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(resId))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = iterations,
        speed = speed,
        isPlaying = isPlaying,
    )

    if (composition == null) {
        Image(
            painter = painterResource(R.drawable.app_logo),
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Fit,
        )
    } else {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = modifier,
        )
    }
}
