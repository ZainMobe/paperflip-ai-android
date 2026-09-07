package com.wapp.paperflipai.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wapp.paperflipai.R
import com.wapp.paperflipai.designsystem.component.PFButton
import com.wapp.paperflipai.designsystem.component.PFButtonSize
import com.wapp.paperflipai.designsystem.component.PFSheet
import com.wapp.paperflipai.designsystem.theme.PFTheme

/**
 * Time-of-day picker for the daily reminder. iOS uses an inline
 * `DatePicker(displayedComponents: .hourAndMinute)`; Android's convention is
 * a modal clock, so this wraps Material's [TimePicker] in a PF sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderTimePicker(
    initialMinuteOfDay: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initialMinuteOfDay / 60,
        initialMinute = initialMinuteOfDay % 60,
        is24Hour = false,
    )

    PFSheet(onDismiss = onDismiss, title = stringResource(R.string.time)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PFTheme.spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.lg),
        ) {
            TimePicker(
                state = state,
                colors = TimePickerDefaults.colors(
                    clockDialColor = PFTheme.colors.elevated,
                    selectorColor = PFTheme.colors.accent,
                    containerColor = PFTheme.colors.surface,
                    periodSelectorSelectedContainerColor = PFTheme.colors.accentSoft,
                    periodSelectorSelectedContentColor = PFTheme.colors.accent,
                    timeSelectorSelectedContainerColor = PFTheme.colors.accentSoft,
                    timeSelectorSelectedContentColor = PFTheme.colors.accent,
                ),
            )
            PFButton(
                title = stringResource(R.string.done),
                onClick = { onConfirm(state.hour * 60 + state.minute) },
                size = PFButtonSize.Lg,
            )
        }
    }
}
