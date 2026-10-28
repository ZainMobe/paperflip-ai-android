package com.wapp.paperflipai.feature.quiz

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wapp.paperflipai.R
import com.wapp.paperflipai.core.data.QuizMode
import com.wapp.paperflipai.designsystem.PFIcons
import com.wapp.paperflipai.designsystem.component.PFButton
import com.wapp.paperflipai.designsystem.component.PFButtonSize
import com.wapp.paperflipai.designsystem.component.PFSheet
import com.wapp.paperflipai.designsystem.modifier.PFHaptic
import com.wapp.paperflipai.designsystem.modifier.pfPressable
import com.wapp.paperflipai.designsystem.theme.PFRadius
import com.wapp.paperflipai.designsystem.theme.PFTheme
import com.wapp.paperflipai.core.analytics.TrackScreen

/**
 * Pick the question count and whether to time the test. Start sits at the
 * bottom of the sheet. Counterpart of `PracticeTestSetupSheet.swift`.
 */
@Composable
fun PracticeTestSetupSheet(
    cardCount: Int,
    onDismiss: () -> Unit,
    onStart: (QuizConfiguration) -> Unit,
) {
    TrackScreen("practice_test_setup", "PracticeTestSetupSheet")
    val options = listOf(10, 20, 30).filter { it <= maxOf(cardCount, 10) }
    var questionCount by remember { mutableIntStateOf(options.first()) }
    var timed by remember { mutableStateOf(true) }
    val effective = minOf(questionCount, cardCount)
    // 30 seconds per question, rounded up to the minute.
    val timeLimit = ((effective * 30 + 59) / 60) * 60

    PFSheet(onDismiss = onDismiss, title = stringResource(R.string.quiz_practice_test)) {
        Column(
            modifier = Modifier.padding(horizontal = PFTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.xl),
        ) {
            Text(
                text = stringResource(R.string.test_setup_subtitle),
                style = PFTheme.type.callout,
                color = PFTheme.colors.onSurfaceMuted,
            )

            Column(verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm)) {
                Text(
                    text = stringResource(R.string.quiz_questions_overline),
                    style = PFTheme.type.overline,
                    color = PFTheme.colors.onSurfaceMuted,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm)) {
                    options.forEach { n ->
                        val selected = questionCount == n
                        val shape = RoundedCornerShape(PFRadius.md)
                        Text(
                            text = n.toString(),
                            style = PFTheme.type.bodyEmphasis,
                            color = if (selected) PFTheme.colors.accent else PFTheme.colors.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .weight(1f)
                                .background(if (selected) PFTheme.colors.accentSoft else PFTheme.colors.elevated, shape)
                                .border(
                                    if (selected) 1.5.dp else 0.7.dp,
                                    if (selected) PFTheme.colors.accent.copy(alpha = 0.5f) else PFTheme.colors.border,
                                    shape,
                                )
                                .pfPressable(
                                    onClick = { questionCount = n },
                                    haptic = PFHaptic.Selection,
                                    role = Role.RadioButton,
                                )
                                .semantics { this.selected = selected }
                                .padding(vertical = PFTheme.spacing.md),
                        )
                    }
                }
                if (cardCount < questionCount) {
                    Text(
                        text = stringResource(R.string.test_setup_uses_all, cardCount),
                        style = PFTheme.type.caption,
                        color = PFTheme.colors.onSurfaceMuted,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PFTheme.colors.elevated, RoundedCornerShape(PFRadius.lg))
                    .padding(PFTheme.spacing.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.test_setup_timed), style = PFTheme.type.bodyEmphasis, color = PFTheme.colors.onSurface)
                    Text(
                        text = stringResource(R.string.test_setup_minutes_for, timeLimit / 60, effective),
                        style = PFTheme.type.caption,
                        color = PFTheme.colors.onSurfaceMuted,
                    )
                }
                Switch(
                    checked = timed,
                    onCheckedChange = { timed = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = PFTheme.colors.accent),
                )
            }

            PFButton(
                title = stringResource(R.string.test_setup_start),
                onClick = {
                    onStart(
                        QuizConfiguration(
                            mode = QuizMode.Test,
                            questionCount = effective,
                            timeLimitSeconds = if (timed) timeLimit else null,
                        )
                    )
                },
                size = PFButtonSize.Lg,
                trailingIcon = PFIcons.Forward,
                modifier = Modifier.padding(bottom = PFTheme.spacing.lg),
            )
        }
    }
}
