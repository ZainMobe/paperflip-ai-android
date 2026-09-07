package com.wapp.paperflipai.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wapp.paperflipai.R
import com.wapp.paperflipai.app.LocalAppEnvironment
import com.wapp.paperflipai.designsystem.PFIcons
import com.wapp.paperflipai.designsystem.component.PFGroupedCard
import com.wapp.paperflipai.designsystem.component.PFListRow
import com.wapp.paperflipai.designsystem.component.PFRowDivider
import com.wapp.paperflipai.designsystem.component.PFScreen
import com.wapp.paperflipai.designsystem.component.PFTopBar
import com.wapp.paperflipai.designsystem.modifier.pfReadableWidth
import com.wapp.paperflipai.designsystem.theme.PFAppearance
import com.wapp.paperflipai.designsystem.theme.PFTheme

/**
 * System / Light / Dark — the Android counterpart of
 * `AppearanceSettingsView.swift`. Applied instantly and remembered.
 */
@Composable
fun AppearanceSettingsScreen(onBack: () -> Unit) {
    val env = LocalAppEnvironment.current
    val selected by env.settings.appearance.collectAsStateWithLifecycle()

    PFScreen(
        topBar = { PFTopBar(title = stringResource(R.string.appearance), onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .pfReadableWidth()
                .padding(PFTheme.spacing.lg),
        ) {
            PFGroupedCard {
                PFAppearance.entries.forEachIndexed { index, mode ->
                    PFListRow(
                        title = stringResource(mode.displayNameRes),
                        subtitle = stringResource(
                            when (mode) {
                                PFAppearance.System -> R.string.appearance_system_desc_android
                                PFAppearance.Light -> R.string.always_use_the_light_theme
                                PFAppearance.Dark -> R.string.always_use_the_dark_theme
                            }
                        ),
                        icon = mode.icon,
                        showChevron = false,
                        onClick = { env.settings.setAppearance(mode) },
                        trailing = if (selected == mode) {
                            {
                                Icon(
                                    imageVector = PFIcons.Check,
                                    contentDescription = null,
                                    tint = PFTheme.colors.accent,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        } else null,
                    )
                    if (index < PFAppearance.entries.lastIndex) PFRowDivider()
                }
            }
            Text(
                text = stringResource(R.string.your_choice_is_applied_immediately_and_remembe),
                style = PFTheme.type.caption,
                color = PFTheme.colors.onSurfaceFaint,
                modifier = Modifier.padding(top = PFTheme.spacing.md, start = PFTheme.spacing.xs),
            )
        }
    }
}
