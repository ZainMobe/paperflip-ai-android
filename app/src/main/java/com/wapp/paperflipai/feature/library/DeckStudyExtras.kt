package com.wapp.paperflipai.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wapp.paperflipai.R
import com.wapp.paperflipai.app.LocalAppEnvironment
import com.wapp.paperflipai.core.data.PFDeck
import com.wapp.paperflipai.core.data.PFFlashcard
import com.wapp.paperflipai.core.data.PFQuizResult
import com.wapp.paperflipai.core.data.QuizMode
import com.wapp.paperflipai.core.srs.StudyPlan
import com.wapp.paperflipai.designsystem.PFIcons
import com.wapp.paperflipai.designsystem.component.PFButton
import com.wapp.paperflipai.designsystem.component.PFButtonSize
import com.wapp.paperflipai.designsystem.component.PFButtonVariant
import com.wapp.paperflipai.designsystem.component.PFProgressRing
import com.wapp.paperflipai.designsystem.component.PFSheet
import com.wapp.paperflipai.designsystem.modifier.pfPressable
import com.wapp.paperflipai.designsystem.theme.PFRadius
import com.wapp.paperflipai.designsystem.theme.PFTheme
import com.wapp.paperflipai.util.asRelativeLabel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import com.wapp.paperflipai.core.analytics.TrackScreen

/*
 * Deck-page additions for phase 2 and 3: the exam-date study plan row,
 * the sheet that sets it, and the quiz history list. Counterparts of the
 * matching blocks in `DeckDetailView.swift` and `ExamDateSheet.swift`.
 */

/** "Exam in 9 days · 12 new cards a day…" or a prompt to set a date. */
@Composable
internal fun StudyPlanRow(plan: StudyPlan?, onClick: () -> Unit) {
    val shape = RoundedCornerShape(PFRadius.lg)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PFTheme.colors.elevated, shape)
            .border(0.7.dp, PFTheme.colors.border, shape)
            .pfPressable(onClick = onClick, pressedScale = 0.985f)
            .padding(PFTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(36.dp).background(PFTheme.colors.accentSoft, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(PFIcons.Exam, null, tint = PFTheme.colors.accent, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(PFTheme.spacing.md))
        Column(Modifier.weight(1f)) {
            if (plan != null) {
                Text(
                    text = if (plan.daysLeft <= 1) stringResource(R.string.plan_exam_tomorrow)
                    else stringResource(R.string.plan_exam_in_n_days, plan.daysLeft),
                    style = PFTheme.type.bodyEmphasis,
                    color = PFTheme.colors.onSurface,
                )
                Text(text = planSummary(plan), style = PFTheme.type.caption, color = PFTheme.colors.onSurfaceMuted)
            } else {
                Text(
                    text = stringResource(R.string.plan_set_exam_date),
                    style = PFTheme.type.bodyEmphasis,
                    color = PFTheme.colors.onSurface,
                )
                Text(
                    text = stringResource(R.string.plan_set_exam_date_body),
                    style = PFTheme.type.caption,
                    color = PFTheme.colors.onSurfaceMuted,
                )
            }
        }
        if (plan != null) {
            PFProgressRing(
                progress = plan.coverage,
                size = 32.dp,
                lineWidth = 4.dp,
                tint = if (plan.isBehind) PFTheme.colors.warning else PFTheme.colors.accent,
                showsLabel = false,
            )
        } else {
            Icon(PFIcons.Chevron, null, tint = PFTheme.colors.onSurfaceFaint, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun planSummary(plan: StudyPlan): String {
    val date = formatExamDate(plan.examDate)
    return when {
        plan.isComplete -> stringResource(R.string.plan_summary_complete, date)
        plan.daysLeft <= 1 -> stringResource(R.string.plan_summary_tomorrow, plan.unseenCards)
        else -> stringResource(R.string.plan_summary_pace, plan.newPerDay, date)
    }
}

private fun formatExamDate(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
        .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

/**
 * Pick or clear the exam date. The pace preview updates as the date
 * changes; Save sits at the bottom of the sheet.
 */
@Composable
internal fun ExamDateSheet(
    deck: PFDeck,
    cards: List<PFFlashcard>,
    onDismiss: () -> Unit,
) {
    TrackScreen("deck_exam_date", "ExamDateSheet")
    val env = LocalAppEnvironment.current
    val zone = ZoneId.systemDefault()
    val todayUtc = remember { LocalDate.now(zone).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }
    // The Material picker speaks UTC midnight; the plan stores local midnight.
    val initialUtc = remember(deck.examDate) {
        val day = deck.examDate?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
            ?: LocalDate.now(zone).plusDays(14)
        day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialUtc,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis >= todayUtc
            override fun isSelectableYear(year: Int): Boolean = year >= LocalDate.now(zone).year
        },
    )
    val selectedDay = pickerState.selectedDateMillis?.let {
        Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
    }
    val preview = selectedDay?.let { StudyPlan.make(StudyPlan.startOfDay(it, zone), cards) }

    PFSheet(onDismiss = onDismiss, title = stringResource(R.string.plan_sheet_title)) {
        // The calendar scrolls on short screens; the buttons stay pinned below it.
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = PFTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.lg),
        ) {
            Text(
                text = stringResource(R.string.plan_sheet_body),
                style = PFTheme.type.callout,
                color = PFTheme.colors.onSurfaceMuted,
            )
            DatePicker(
                state = pickerState,
                title = null,
                headline = null,
                showModeToggle = false,
                colors = DatePickerDefaults.colors(
                    containerColor = PFTheme.colors.elevated,
                    selectedDayContainerColor = PFTheme.colors.accent,
                    todayDateBorderColor = PFTheme.colors.accent,
                    todayContentColor = PFTheme.colors.accent,
                ),
                modifier = Modifier.background(PFTheme.colors.elevated, RoundedCornerShape(PFRadius.lg)),
            )
            preview?.let { p ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PFTheme.colors.accentSoft, RoundedCornerShape(PFRadius.lg))
                        .padding(PFTheme.spacing.lg),
                ) {
                    PaceCell(p.daysLeft.toString(), stringResource(R.string.plan_days_left), Modifier.weight(1f))
                    PaceCell(p.unseenCards.toString(), stringResource(R.string.plan_unseen_cards), Modifier.weight(1f))
                    PaceCell(p.newPerDay.toString(), stringResource(R.string.plan_new_per_day), Modifier.weight(1f))
                }
            }
        }
        Column(
            modifier = Modifier.padding(horizontal = PFTheme.spacing.lg, vertical = PFTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm),
        ) {
            PFButton(
                title = stringResource(R.string.plan_save),
                onClick = {
                    selectedDay?.let { env.repository.setExamDate(deck.id, StudyPlan.startOfDay(it, zone)) }
                    env.refreshWidget()
                    onDismiss()
                },
                size = PFButtonSize.Lg,
                trailingIcon = PFIcons.Check,
                enabled = selectedDay != null,
            )
            if (deck.examDate != null) {
                PFButton(
                    title = stringResource(R.string.plan_remove),
                    onClick = {
                        env.repository.setExamDate(deck.id, null)
                        env.refreshWidget()
                        onDismiss()
                    },
                    variant = PFButtonVariant.Ghost,
                    leadingIcon = PFIcons.ExamRemove,
                    isDestructive = true,
                )
            }
        }
    }
}

@Composable
private fun PaceCell(value: String, label: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = PFTheme.type.title2, color = PFTheme.colors.accent)
        Text(label, style = PFTheme.type.caption, color = PFTheme.colors.onSurfaceMuted, textAlign = TextAlign.Center)
    }
}

/** Last three quiz / test results with the best score. */
@Composable
internal fun QuizHistory(results: List<PFQuizResult>) {
    val best = results.maxOfOrNull { it.scorePercent } ?: 0
    Column(
        modifier = Modifier.padding(top = PFTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.quiz_history),
                style = PFTheme.type.overline,
                color = PFTheme.colors.onSurfaceMuted,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.quiz_best_percent, best),
                style = PFTheme.type.caption,
                color = PFTheme.colors.onSurfaceFaint,
            )
        }
        results.take(3).forEach { result ->
            val tint = when {
                result.scorePercent >= 80 -> PFTheme.colors.success
                result.scorePercent >= 50 -> PFTheme.colors.warning
                else -> PFTheme.colors.danger
            }
            val title = stringResource(
                if (result.mode == QuizMode.Test) R.string.quiz_practice_test else R.string.quiz_title
            )
            val detail = stringResource(
                R.string.quiz_history_detail,
                result.correctCount,
                result.questionCount,
                result.endedAt.asRelativeLabel(),
            )
            val description = "$title, ${result.scorePercent}%, $detail"
            val shape = RoundedCornerShape(PFRadius.lg)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PFTheme.colors.elevated, shape)
                    .border(0.7.dp, PFTheme.colors.border, shape)
                    .clearAndSetSemantics { contentDescription = description }
                    .padding(PFTheme.spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PFProgressRing(
                    progress = result.scorePercent / 100f,
                    size = 36.dp,
                    lineWidth = 4.dp,
                    tint = tint,
                    showsLabel = false,
                )
                Spacer(Modifier.width(PFTheme.spacing.md))
                Column(Modifier.weight(1f)) {
                    Text(title, style = PFTheme.type.bodyEmphasis, color = PFTheme.colors.onSurface)
                    Text(detail, style = PFTheme.type.caption, color = PFTheme.colors.onSurfaceMuted)
                }
                Text("${result.scorePercent}%", style = PFTheme.type.title3, color = PFTheme.colors.onSurface)
            }
        }
    }
}
