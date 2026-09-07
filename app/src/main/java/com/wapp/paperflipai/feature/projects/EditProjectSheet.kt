package com.wapp.paperflipai.feature.projects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.wapp.paperflipai.R
import com.wapp.paperflipai.app.LocalAppEnvironment
import com.wapp.paperflipai.core.data.PFProject
import com.wapp.paperflipai.core.data.ProjectDto
import com.wapp.paperflipai.designsystem.PFIcons
import com.wapp.paperflipai.designsystem.component.PFButton
import com.wapp.paperflipai.designsystem.component.PFButtonSize
import com.wapp.paperflipai.designsystem.component.PFErrorBanner
import com.wapp.paperflipai.designsystem.component.PFSheet
import com.wapp.paperflipai.designsystem.component.PFTextField
import com.wapp.paperflipai.designsystem.theme.PFTheme
import kotlinx.coroutines.launch

/**
 * Owner-only rename / re-describe — the Android counterpart of
 * `EditProjectSheet.swift`.
 */
@Composable
fun EditProjectSheet(
    project: PFProject,
    onDismiss: () -> Unit,
) {
    val env = LocalAppEnvironment.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(project.name) }
    var details by remember { mutableStateOf(project.description.orEmpty()) }
    var isSaving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val trimmedName = name.trim()
    val trimmedDetails = details.trim()
    val canSave = !isSaving && trimmedName.length >= 2 &&
        (trimmedName != project.name || trimmedDetails != project.description.orEmpty())

    PFSheet(onDismiss = onDismiss, title = stringResource(R.string.edit_project)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PFTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.xl),
        ) {
            PFTextField(
                label = stringResource(R.string.project_name),
                value = name,
                onValueChange = { name = it; error = null },
                placeholder = "e.g. Biology 101",
                icon = PFIcons.Group,
                imeAction = ImeAction.Next,
            )
            PFTextField(
                label = stringResource(R.string.description),
                value = details,
                onValueChange = { details = it },
                placeholder = stringResource(R.string.optional),
                icon = PFIcons.Document,
                imeAction = ImeAction.Done,
            )

            PFErrorBanner(error)

            PFButton(
                title = stringResource(if (isSaving) R.string.saving else R.string.save_2),
                onClick = {
                    scope.launch {
                        isSaving = true
                        error = null
                        val updated = project.copy(
                            name = trimmedName,
                            description = trimmedDetails.ifEmpty { null },
                            updatedAt = System.currentTimeMillis(),
                        )
                        try {
                            env.remote.updateProject(
                                ProjectDto(
                                    id = updated.id,
                                    ownerUserId = updated.ownerUserId,
                                    name = updated.name,
                                    description = updated.description,
                                    joinLinkToken = updated.joinLinkToken,
                                    joinLinkEnabled = updated.joinLinkEnabled,
                                    createdAt = updated.createdAt,
                                    updatedAt = updated.updatedAt,
                                )
                            )
                            env.database.upsertProject(updated)
                            onDismiss()
                        } catch (failure: Exception) {
                            error = failure.message
                        } finally {
                            isSaving = false
                        }
                    }
                },
                size = PFButtonSize.Lg,
                isLoading = isSaving,
                enabled = canSave,
            )
        }
    }
}
