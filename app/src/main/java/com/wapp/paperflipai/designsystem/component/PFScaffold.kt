package com.wapp.paperflipai.designsystem.component

import com.wapp.paperflipai.designsystem.PFIcons
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wapp.paperflipai.R
import com.wapp.paperflipai.designsystem.modifier.pfPressable
import com.wapp.paperflipai.designsystem.theme.PFTheme

/**
 * The standard screen shell. Paints the PaperFlip surface, keeps the
 * status/navigation bar handling in one place, and hands the content the
 * scaffold insets so every screen scrolls edge-to-edge the Android way.
 */
@Composable
fun PFScreen(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    containerColor: Color = PFTheme.colors.surface,
    contentWindowInsets: WindowInsets = WindowInsets(0, 0, 0, 0),
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = topBar,
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        containerColor = containerColor,
        contentColor = PFTheme.colors.onSurface,
        contentWindowInsets = contentWindowInsets,
        content = content,
    )
}

/**
 * PaperFlip top app bar. Title uses the rounded display face so screen
 * headers read as brand rather than as stock Material.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PFTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    navigationIcon: ImageVector = PFIcons.Back,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Text(
                text = title,
                style = PFTheme.type.title3,
                color = PFTheme.colors.onSurface,
                maxLines = 1,
            )
        },
        navigationIcon = {
            if (onBack != null) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .pfPressable(onClick = onBack, pressedScale = 0.9f),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = navigationIcon,
                        contentDescription = stringResource(R.string.nav_back),
                        tint = PFTheme.colors.onSurface,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = PFTheme.colors.elevated,
            titleContentColor = PFTheme.colors.onSurface,
            navigationIconContentColor = PFTheme.colors.onSurface,
            actionIconContentColor = PFTheme.colors.onSurface,
        ),
        scrollBehavior = scrollBehavior,
    )
}

/** Small uppercase eyebrow above a group of rows. */
@Composable
fun PFSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = PFTheme.type.overline,
        color = PFTheme.colors.onSurfaceFaint,
        modifier = modifier.padding(
            start = PFTheme.spacing.xs,
            top = PFTheme.spacing.lg,
            bottom = PFTheme.spacing.sm,
        ),
    )
}

/** A round icon button sized for a top bar action slot. */
@Composable
fun PFIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = PFTheme.colors.onSurface,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .pfPressable(onClick = onClick, enabled = enabled, pressedScale = 0.88f),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else tint.copy(alpha = 0.4f),
            modifier = Modifier.size(24.dp),
        )
    }
}

/**
 * Bottom inset for the five top-level tab screens: the floating tab bar's
 * height plus the system navigation inset, so the last row of a list is
 * never hidden behind it.
 */
@Composable
fun tabBarContentPadding(extra: Dp = 0.dp): Dp =
    64.dp + extra + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
