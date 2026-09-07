package com.wapp.paperflipai.designsystem.component

import com.wapp.paperflipai.designsystem.PFIcons
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.wapp.paperflipai.R
import com.wapp.paperflipai.designsystem.modifier.pfReadableWidth
import com.wapp.paperflipai.designsystem.theme.PFTheme

/**
 * The app's one modal sheet. Every "…Sheet" screen the iOS app presents
 * with `.sheet(isPresented:)` maps onto this, so they all share the same
 * drag handle, insets, title row and theming.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PFSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    showCloseButton: Boolean = true,
    trailingAction: @Composable (() -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = sheetState,
        containerColor = PFTheme.colors.surface,
        contentColor = PFTheme.colors.onSurface,
        scrimColor = Color.Black.copy(alpha = 0.42f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .pfReadableWidth()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            if (title != null || showCloseButton || trailingAction != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = PFTheme.spacing.lg,
                            end = PFTheme.spacing.sm,
                            bottom = PFTheme.spacing.sm,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = title.orEmpty(),
                        style = PFTheme.type.title3,
                        color = PFTheme.colors.onSurface,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                    )
                    trailingAction?.invoke()
                    if (showCloseButton) {
                        PFIconButton(
                            icon = PFIcons.Close,
                            contentDescription = stringResource(R.string.nav_close),
                            onClick = onDismiss,
                            tint = PFTheme.colors.onSurfaceMuted,
                        )
                    }
                }
            }
            content()
            Spacer(Modifier.padding(bottom = PFTheme.spacing.lg))
        }
    }
}
