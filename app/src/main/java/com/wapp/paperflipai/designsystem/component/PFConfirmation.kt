package com.wapp.paperflipai.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wapp.paperflipai.R
import com.wapp.paperflipai.designsystem.modifier.pfPressable
import com.wapp.paperflipai.designsystem.modifier.pfReadableWidth
import com.wapp.paperflipai.designsystem.theme.PFTheme
import kotlinx.coroutines.launch

/**
 * Themed confirmation sheet — the Android counterpart of
 * `PFConfirmation.swift`. Replaces the stock AlertDialog for destructive or
 * irreversible actions so it matches the rest of the design system: rounded
 * sheet, icon bubble, clear hierarchy, accent buttons.
 *
 * ```
 * var confirm by remember { mutableStateOf<PFConfirmation?>(null) }
 * PFConfirmationHost(confirm) { confirm = null }
 *
 * // …
 * confirm = PFConfirmation(
 *     title = "Revoke invite?",
 *     message = "alice@example.com won't be able to accept anymore.",
 *     icon = Icons.Outlined.MailOutline,
 *     confirmTitle = "Revoke",
 *     destructive = true,
 * ) { viewModel.revoke() }
 * ```
 */
data class PFConfirmation(
    val title: String,
    val message: String? = null,
    val icon: ImageVector? = null,
    val confirmTitle: String,
    val cancelTitle: String,
    val destructive: Boolean = false,
    val onConfirm: suspend () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PFConfirmationHost(
    confirmation: PFConfirmation?,
    onDismiss: () -> Unit,
) {
    if (confirmation == null) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var isWorking by remember(confirmation) { mutableStateOf(false) }
    val tint = if (confirmation.destructive) PFTheme.colors.danger else PFTheme.colors.accent

    ModalBottomSheet(
        onDismissRequest = { if (!isWorking) onDismiss() },
        sheetState = sheetState,
        containerColor = PFTheme.colors.surface,
        contentColor = PFTheme.colors.onSurface,
        scrimColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.42f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .pfReadableWidth()
                .padding(horizontal = PFTheme.spacing.lg)
                .padding(bottom = PFTheme.spacing.xl)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.lg),
        ) {
            confirmation.icon?.let {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(tint.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(it, contentDescription = null, tint = tint, modifier = Modifier.size(28.dp))
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm),
            ) {
                Text(
                    text = confirmation.title,
                    style = PFTheme.type.title3,
                    color = PFTheme.colors.onSurface,
                    textAlign = TextAlign.Center,
                )
                confirmation.message?.let {
                    Text(
                        text = it,
                        style = PFTheme.type.callout,
                        color = PFTheme.colors.onSurfaceMuted,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm),
            ) {
                PFButton(
                    title = confirmation.confirmTitle,
                    onClick = {
                        scope.launch {
                            isWorking = true
                            confirmation.onConfirm()
                            isWorking = false
                            onDismiss()
                        }
                    },
                    size = PFButtonSize.Lg,
                    isLoading = isWorking,
                    isDestructive = confirmation.destructive,
                    enabled = !isWorking,
                )
                Text(
                    text = confirmation.cancelTitle,
                    style = PFTheme.type.bodyEmphasis,
                    color = PFTheme.colors.onSurfaceMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .pfPressable(onClick = { if (!isWorking) onDismiss() }, enabled = !isWorking)
                        .padding(vertical = PFTheme.spacing.md),
                )
            }
        }
    }
}

/** Convenience builder that fills in the localised default button titles. */
@Composable
fun rememberPFConfirmation(
    title: String,
    message: String? = null,
    icon: ImageVector? = null,
    confirmTitle: String = stringResource(R.string.confirm_2),
    cancelTitle: String = stringResource(R.string.cancel),
    destructive: Boolean = false,
    onConfirm: suspend () -> Unit,
): PFConfirmation = PFConfirmation(
    title = title,
    message = message,
    icon = icon,
    confirmTitle = confirmTitle,
    cancelTitle = cancelTitle,
    destructive = destructive,
    onConfirm = onConfirm,
)
