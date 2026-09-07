package com.wapp.paperflipai.designsystem.modifier

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Caps content to a comfortable line length on wide screens (tablets,
 * foldables, landscape phones) — the Android counterpart of
 * `ReadableWidth.swift`. A no-op on compact widths so phone layouts stay
 * edge-to-edge.
 *
 * Order matters: `widthIn(max = ...)` on its own is defeated by an outer
 * `fillMaxWidth()` (the tight incoming constraint wins), so this deliberately
 * re-loosens with `wrapContentWidth` before applying the cap - and that
 * doubles as the horizontal centring, which a bare `widthIn` would otherwise
 * leave pinned to the start edge.
 *
 * Use on detail / settings / form / reading screens. Don't use on dashboards
 * or grids that *want* the extra width - those adapt their column count
 * instead.
 */
@Composable
fun Modifier.pfReadableWidth(maxWidth: Dp = 700.dp): Modifier {
    val widthDp = LocalConfiguration.current.screenWidthDp
    return if (widthDp >= EXPANDED_WIDTH_DP) {
        this
            .fillMaxWidth()
            .wrapContentWidth(Alignment.CenterHorizontally)
            .widthIn(max = maxWidth)
    } else {
        this
    }
}

/** True on tablet / unfolded / landscape-tablet widths. */
@Composable
fun isExpandedWidth(): Boolean =
    LocalConfiguration.current.screenWidthDp >= EXPANDED_WIDTH_DP

/** Material's compact -> medium window-size-class breakpoint. */
const val EXPANDED_WIDTH_DP = 600
