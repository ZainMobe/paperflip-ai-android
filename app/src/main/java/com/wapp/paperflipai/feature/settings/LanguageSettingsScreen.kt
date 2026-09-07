package com.wapp.paperflipai.feature.settings

import android.app.Activity
import android.app.LocaleManager
import android.os.LocaleList
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
import androidx.compose.ui.platform.LocalContext
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
import com.wapp.paperflipai.designsystem.theme.PFLanguage
import com.wapp.paperflipai.designsystem.theme.PFTheme

/**
 * System / English / Arabic / French / Spanish — the Android counterpart of
 * `LanguageSettingsView.swift`.
 *
 * Where iOS overrides the SwiftUI locale environment, Android hands the
 * choice to the platform's per-app language API, so it also shows up in
 * system Settings → Apps → PaperFlip → Language.
 */
@Composable
fun LanguageSettingsScreen(onBack: () -> Unit) {
    val env = LocalAppEnvironment.current
    val context = LocalContext.current
    val selected by env.settings.language.collectAsStateWithLifecycle()

    PFScreen(
        topBar = { PFTopBar(title = stringResource(R.string.language), onBack = onBack) },
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
                PFLanguage.entries.forEachIndexed { index, language ->
                    PFListRow(
                        title = if (language == PFLanguage.System) stringResource(R.string.system)
                        else language.displayName,
                        subtitle = stringResource(
                            when (language) {
                                PFLanguage.System -> R.string.follows_your_device_language
                                PFLanguage.En -> R.string.english
                                PFLanguage.Ar -> R.string.arabic
                                PFLanguage.Fr -> R.string.french
                                PFLanguage.Es -> R.string.spanish
                            }
                        ),
                        icon = language.icon,
                        showChevron = false,
                        onClick = {
                            env.settings.setLanguage(language)
                            applyLocale(context, language)
                        },
                        trailing = if (selected == language) {
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
                    if (index < PFLanguage.entries.lastIndex) PFRowDivider()
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

private fun applyLocale(context: android.content.Context, language: PFLanguage) {
    val manager = context.getSystemService(LocaleManager::class.java) ?: return
    manager.applicationLocales = if (language.localeTag.isEmpty()) {
        LocaleList.getEmptyLocaleList()
    } else {
        LocaleList.forLanguageTags(language.localeTag)
    }
}
