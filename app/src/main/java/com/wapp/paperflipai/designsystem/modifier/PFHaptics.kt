package com.wapp.paperflipai.designsystem.modifier

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * Haptic vocabulary — the Android counterpart of `HapticModifier.swift`.
 *
 * Use sparingly: over-haptic-ing a UI is the fastest way to feel cheap
 * rather than premium.
 */
enum class PFHaptic {
    Light, Medium, Heavy, Selection, Success, Warning, Error;

    internal fun constant(): Int = when (this) {
        Light -> HapticFeedbackConstants.CLOCK_TICK
        Medium -> HapticFeedbackConstants.CONTEXT_CLICK
        Heavy -> HapticFeedbackConstants.LONG_PRESS
        Selection -> HapticFeedbackConstants.CLOCK_TICK
        Success -> HapticFeedbackConstants.CONFIRM
        Warning -> HapticFeedbackConstants.GESTURE_END
        Error -> HapticFeedbackConstants.REJECT
    }
}

@Immutable
class PFHapticPerformer internal constructor(private val view: View?) {
    fun perform(haptic: PFHaptic) {
        val v = view ?: return
        v.performHapticFeedback(haptic.constant())
    }

    operator fun invoke(haptic: PFHaptic) = perform(haptic)
}

@Composable
fun rememberPFHaptics(): PFHapticPerformer {
    val view = LocalView.current
    return remember(view) { PFHapticPerformer(view) }
}
