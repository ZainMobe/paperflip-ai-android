package com.wapp.paperflipai.feature.support

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wapp.paperflipai.R
import com.wapp.paperflipai.designsystem.PFIcons
import com.wapp.paperflipai.designsystem.component.PFButton
import com.wapp.paperflipai.designsystem.component.PFButtonSize
import com.wapp.paperflipai.designsystem.component.PFSheet
import com.wapp.paperflipai.designsystem.theme.PFTheme

/**
 * One-time notice before the AI support chat — the Android counterpart of
 * `AIConsentSheet.swift`.
 */
@Composable
fun AIConsentSheet(
    onDismiss: () -> Unit,
    onAcknowledge: () -> Unit,
) {
    PFSheet(onDismiss = onDismiss, title = stringResource(R.string.before_you_chat)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PFTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.lg),
        ) {
            Text(
                text = stringResource(R.string.paperflip_support_uses_ai_to_answer_product_qu),
                style = PFTheme.type.callout,
                color = PFTheme.colors.onSurfaceMuted,
            )

            ConsentPoint(
                icon = PFIcons.Sparkle,
                title = stringResource(R.string.ai_generated_answers),
                body = stringResource(R.string.responses_are_produced_by_an_ai_model_they_may),
            )
            ConsentPoint(
                icon = PFIcons.Support,
                title = stringResource(R.string.human_help_available),
                body = stringResource(R.string.for_refunds_billing_disputes_or_account_specif),
            )
            ConsentPoint(
                icon = PFIcons.Flag,
                title = stringResource(R.string.flag_bad_answers),
                body = stringResource(R.string.long_press_any_ai_message_and_choose_flag_we_r),
            )
            ConsentPoint(
                icon = PFIcons.Password,
                title = stringResource(R.string.what_we_send),
                body = stringResource(R.string.your_messages_current_plan_and_standard_suppor),
            )

            PFButton(
                title = stringResource(R.string.i_understand_continue),
                onClick = onAcknowledge,
                size = PFButtonSize.Lg,
                trailingIcon = PFIcons.Forward,
            )
        }
    }
}

@Composable
private fun ConsentPoint(icon: ImageVector, title: String, body: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(PFTheme.colors.accentSoft, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = PFTheme.colors.accent, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(PFTheme.spacing.md))
        Column(Modifier.weight(1f)) {
            Text(text = title, style = PFTheme.type.bodyEmphasis, color = PFTheme.colors.onSurface)
            Text(text = body, style = PFTheme.type.footnote, color = PFTheme.colors.onSurfaceMuted)
        }
    }
}
