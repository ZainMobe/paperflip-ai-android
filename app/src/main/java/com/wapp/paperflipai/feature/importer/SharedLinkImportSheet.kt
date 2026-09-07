package com.wapp.paperflipai.feature.importer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.wapp.paperflipai.R
import com.wapp.paperflipai.app.LocalAppEnvironment
import com.wapp.paperflipai.designsystem.PFIcons
import com.wapp.paperflipai.designsystem.component.PFButton
import com.wapp.paperflipai.designsystem.component.PFButtonSize
import com.wapp.paperflipai.designsystem.component.PFSheet
import com.wapp.paperflipai.designsystem.component.PFTextField
import com.wapp.paperflipai.designsystem.component.PFToast
import com.wapp.paperflipai.designsystem.modifier.pfPressable
import com.wapp.paperflipai.designsystem.theme.PFTheme
import kotlinx.coroutines.launch

private val UUID_REGEX =
    Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", RegexOption.IGNORE_CASE)

/**
 * Pro-only: paste a paperflip.ai/share/{uuid} URL and the server copies the
 * deck into the user's library, no generation credit spent — the Android
 * counterpart of `SharedLinkImportSheet.swift`.
 */
@Composable
fun SharedLinkImportSheet(onDismiss: () -> Unit) {
    val env = LocalAppEnvironment.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    var url by remember { mutableStateOf("") }
    var isImporting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val trimmed = url.trim()
    val looksValid = UUID_REGEX.containsMatchIn(trimmed)
    val importedLabel = stringResource(R.string.import_deck)

    PFSheet(onDismiss = onDismiss, title = stringResource(R.string.shared_link)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PFTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.xl),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(PFTheme.colors.success.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = PFIcons.Link,
                        contentDescription = null,
                        tint = PFTheme.colors.success,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.width(PFTheme.spacing.md))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.import_a_shared_deck),
                        style = PFTheme.type.title3,
                        color = PFTheme.colors.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.we_ll_copy_it_into_your_library_no_generation),
                        style = PFTheme.type.footnote,
                        color = PFTheme.colors.onSurfaceMuted,
                    )
                }
            }

            PFTextField(
                label = stringResource(R.string.paperflip_share_link),
                value = url,
                onValueChange = { url = it; error = null },
                placeholder = "https://www.paperflip.ai/share/…",
                icon = PFIcons.Link,
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Done,
                error = error,
                helper = if (error == null) stringResource(R.string.paste_the_share_url_you_received) else null,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm)) {
                MiniChipPublic(
                    icon = PFIcons.Copy,
                    label = stringResource(R.string.paste),
                    onClick = { clipboard.getText()?.text?.let { url = it } },
                )
                if (url.isNotEmpty()) {
                    MiniChipPublic(
                        icon = PFIcons.Close,
                        label = stringResource(R.string.clear),
                        muted = true,
                        onClick = { url = "" },
                    )
                }
            }

            PFButton(
                title = stringResource(R.string.import_deck),
                onClick = {
                    scope.launch {
                        isImporting = true
                        error = null
                        try {
                            env.remote.importSharedDeck(trimmed)
                            env.auth.session.value?.userId?.let { env.sync.pullAll(it) }
                            PFToast.success(importedLabel)
                            onDismiss()
                        } catch (failure: Exception) {
                            error = failure.message
                        } finally {
                            isImporting = false
                        }
                    }
                },
                size = PFButtonSize.Lg,
                trailingIcon = PFIcons.Forward,
                isLoading = isImporting,
                enabled = looksValid && !isImporting,
            )
        }
    }
}

@Composable
private fun MiniChipPublic(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    muted: Boolean = false,
) {
    val foreground = if (muted) PFTheme.colors.onSurfaceMuted else PFTheme.colors.accent
    val background = if (muted) PFTheme.colors.elevated else PFTheme.colors.accentSoft
    Row(
        modifier = Modifier
            .background(background, CircleShape)
            .pfPressable(onClick = onClick, pressedScale = 0.96f)
            .padding(horizontal = PFTheme.spacing.md, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = foreground, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(PFTheme.spacing.xs))
        Text(text = label, style = PFTheme.type.footnoteBold, color = foreground)
    }
}
