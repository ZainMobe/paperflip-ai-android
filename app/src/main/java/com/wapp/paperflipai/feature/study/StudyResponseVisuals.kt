package com.wapp.paperflipai.feature.study

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.wapp.paperflipai.R
import com.wapp.paperflipai.core.srs.SRSResponse
import com.wapp.paperflipai.designsystem.PFIcons
import com.wapp.paperflipai.designsystem.theme.PFTheme

/**
 * Shared presentation for the four SM-2 responses, so the response bar,
 * the swipe badges and the card glow can't drift apart.
 */
val SRSResponse.icon: ImageVector
    get() = when (this) {
        SRSResponse.Again -> PFIcons.Refresh
        SRSResponse.Hard -> PFIcons.Timer
        SRSResponse.Good -> PFIcons.Check
        SRSResponse.Easy -> PFIcons.Bolt
    }

val SRSResponse.labelRes: Int
    get() = when (this) {
        SRSResponse.Again -> R.string.again
        SRSResponse.Hard -> R.string.hard
        SRSResponse.Good -> R.string.good
        SRSResponse.Easy -> R.string.easy
    }

@Composable
fun SRSResponse.color(): Color = when (this) {
    SRSResponse.Again -> PFTheme.colors.again
    SRSResponse.Hard -> PFTheme.colors.hard
    SRSResponse.Good -> PFTheme.colors.good
    SRSResponse.Easy -> PFTheme.colors.easy
}

/** "1d", "3d", "2w", "5mo", "1y" — the next interval for a response. */
fun intervalLabel(days: Int): String = when {
    days <= 0 -> "<1d"
    days == 1 -> "1d"
    days < 7 -> "${days}d"
    days < 30 -> "${days / 7}w"
    days < 365 -> "${days / 30}mo"
    else -> "${days / 365}y"
}
