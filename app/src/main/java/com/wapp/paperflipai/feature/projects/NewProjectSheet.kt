package com.wapp.paperflipai.feature.projects

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.wapp.paperflipai.R
import com.wapp.paperflipai.app.LocalAppEnvironment
import com.wapp.paperflipai.designsystem.PFIcons
import com.wapp.paperflipai.designsystem.component.PFButton
import com.wapp.paperflipai.designsystem.component.PFButtonSize
import com.wapp.paperflipai.designsystem.component.PFErrorBanner
import com.wapp.paperflipai.designsystem.component.PFSheet
import com.wapp.paperflipai.designsystem.component.PFTextField
import com.wapp.paperflipai.designsystem.theme.PFTheme
import kotlinx.coroutines.launch

/**
 * Create a project — the Android counterpart of `NewProjectSheet.swift`.
 * Pro-only; the caller gates on entitlement before presenting it.
 */
@Composable
fun NewProjectSheet(
    onDismiss: () -> Unit,
    onCreated: (String) -> Unit,
) {
    val env = LocalAppEnvironment.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var isCreating by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val trimmedName = name.trim()
    val canCreate = !isCreating && trimmedName.length >= 2

    PFSheet(onDismiss = onDismiss, title = stringResource(R.string.new_project)) {
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
                        .background(PFTheme.colors.accentSoft, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = PFIcons.Group,
                        contentDescription = null,
                        tint = PFTheme.colors.accent,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.width(PFTheme.spacing.md))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.collaborate_on_decks),
                        style = PFTheme.type.title3,
                        color = PFTheme.colors.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.invite_teammates_by_email_decks_you_put_in_thi),
                        style = PFTheme.type.footnote,
                        color = PFTheme.colors.onSurfaceMuted,
                    )
                }
            }

            PFTextField(
                label = stringResource(R.string.project_name),
                value = name,
                onValueChange = { name = it; error = null },
                placeholder = "e.g. Biology 101",
                icon = PFIcons.Group,
                imeAction = ImeAction.Next,
            )
            PFTextField(
                label = stringResource(R.string.description_optional),
                value = details,
                onValueChange = { details = it },
                placeholder = "What's this project for?",
                icon = PFIcons.Document,
                imeAction = ImeAction.Done,
            )

            PFErrorBanner(error)

            PFButton(
                title = stringResource(if (isCreating) R.string.creating else R.string.create),
                onClick = {
                    scope.launch {
                        val userId = env.auth.session.value?.userId
                        if (userId == null) {
                            error = "You need to be signed in."
                            return@launch
                        }
                        isCreating = true
                        error = null
                        try {
                            val dto = env.remote.createProject(
                                name = trimmedName,
                                description = details.trim().ifEmpty { null },
                                ownerUserId = userId,
                            )
                            env.database.upsertProject(dto.toModel())
                            env.sync.pullProjects(userId)
                            onCreated(dto.id)
                        } catch (failure: Exception) {
                            error = failure.message
                        } finally {
                            isCreating = false
                        }
                    }
                },
                size = PFButtonSize.Lg,
                isLoading = isCreating,
                enabled = canCreate,
            )
        }
    }
}
