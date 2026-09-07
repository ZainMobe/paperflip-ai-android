package com.wapp.paperflipai.feature.settings

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wapp.paperflipai.R
import com.wapp.paperflipai.app.LocalAppEnvironment
import com.wapp.paperflipai.designsystem.PFIcons
import com.wapp.paperflipai.designsystem.component.PFButton
import com.wapp.paperflipai.designsystem.component.PFButtonSize
import com.wapp.paperflipai.designsystem.component.PFButtonVariant
import com.wapp.paperflipai.designsystem.component.PFErrorBanner
import com.wapp.paperflipai.designsystem.component.PFScreen
import com.wapp.paperflipai.designsystem.component.PFTextField
import com.wapp.paperflipai.designsystem.component.PFToast
import com.wapp.paperflipai.designsystem.component.PFTopBar
import com.wapp.paperflipai.designsystem.modifier.pfPressable
import com.wapp.paperflipai.designsystem.modifier.pfReadableWidth
import com.wapp.paperflipai.designsystem.theme.PFTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Display name and profile picture — the Android counterpart of
 * `EditProfileView.swift`.
 *
 * Avatar pipeline: photo picker → decode → downscale to 512px → JPEG 85 →
 * upload to storage → save the returned URL on the profile.
 */
@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    onOpenDeleteAccount: () -> Unit,
) {
    val env = LocalAppEnvironment.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val session by env.auth.session.collectAsStateWithLifecycle()

    var fullName by remember(session?.userId) { mutableStateOf(session?.fullName.orEmpty()) }
    var isSaving by remember { mutableStateOf(false) }
    var savedRecently by remember { mutableStateOf(false) }
    var isUploadingAvatar by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val canSave = !isSaving && fullName.trim().isNotEmpty() && fullName.trim() != session?.fullName

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            isUploadingAvatar = true
            error = null
            val bytes = withContext(Dispatchers.IO) { downscaleToJpeg(context, uri) }
            if (bytes == null) {
                error = context.getString(R.string.that_didn_t_work)
            } else {
                runCatching {
                    val userId = env.auth.session.value?.userId.orEmpty()
                    val url = env.remote.uploadAvatar(bytes, "image/jpeg", userId)
                    env.auth.updateProfileAvatarUrl(url)
                }.onFailure { error = it.message }
            }
            isUploadingAvatar = false
        }
    }

    PFScreen(
        topBar = { PFTopBar(title = stringResource(R.string.edit_profile), onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .pfReadableWidth()
                .padding(PFTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.xl),
        ) {
            // ── Avatar ────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(PFTheme.colors.accentSoft, CircleShape)
                        .border(0.7.dp, PFTheme.colors.border, CircleShape)
                        .pfPressable(
                            onClick = {
                                photoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            pressedScale = 0.94f,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isUploadingAvatar) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = PFTheme.colors.accent,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            text = session?.initials ?: "P",
                            style = PFTheme.type.title2,
                            color = PFTheme.colors.accent,
                        )
                    }
                }
                Spacer(Modifier.width(PFTheme.spacing.md))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = session?.email.orEmpty(),
                        style = PFTheme.type.bodyEmphasis,
                        color = PFTheme.colors.onSurface,
                    )
                    Spacer(Modifier.height(PFTheme.spacing.xs))
                    Row(horizontalArrangement = Arrangement.spacedBy(PFTheme.spacing.md)) {
                        Text(
                            text = stringResource(R.string.change_photo),
                            style = PFTheme.type.footnoteBold,
                            color = PFTheme.colors.accent,
                            modifier = Modifier.pfPressable(
                                onClick = {
                                    photoPicker.launch(
                                        PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                },
                                pressedScale = 0.94f,
                            ),
                        )
                        if (session?.avatarUrl != null) {
                            Text(
                                text = stringResource(R.string.remove),
                                style = PFTheme.type.footnoteBold,
                                color = PFTheme.colors.danger,
                                modifier = Modifier.pfPressable(
                                    onClick = {
                                        scope.launch {
                                            runCatching { env.auth.updateProfileAvatarUrl(null) }
                                        }
                                    },
                                    pressedScale = 0.94f,
                                ),
                            )
                        }
                    }
                }
            }

            PFTextField(
                label = stringResource(R.string.display_name),
                value = fullName,
                onValueChange = { fullName = it; savedRecently = false; error = null },
                placeholder = stringResource(R.string.your_name),
                icon = PFIcons.Person,
                imeAction = ImeAction.Done,
            )

            PFErrorBanner(error)

            Row(verticalAlignment = Alignment.CenterVertically) {
                PFButton(
                    title = stringResource(if (isSaving) R.string.saving else R.string.save_changes),
                    onClick = {
                        scope.launch {
                            isSaving = true
                            error = null
                            try {
                                env.auth.updateProfileName(fullName.trim())
                                savedRecently = true
                            } catch (failure: Exception) {
                                error = failure.message
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    leadingIcon = PFIcons.Check,
                    isLoading = isSaving,
                    enabled = canSave,
                    fullWidth = false,
                )
                if (savedRecently) {
                    Spacer(Modifier.width(PFTheme.spacing.md))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = PFIcons.CheckCircle,
                            contentDescription = null,
                            tint = PFTheme.colors.success,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(PFTheme.spacing.xs))
                        Text(
                            text = stringResource(R.string.saved),
                            style = PFTheme.type.footnoteBold,
                            color = PFTheme.colors.success,
                        )
                    }
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(0.7.dp)
                    .background(PFTheme.colors.divider)
            )

            // ── Danger zone ───────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm)) {
                Text(
                    text = stringResource(R.string.danger_zone),
                    style = PFTheme.type.overline,
                    color = PFTheme.colors.onSurfaceMuted,
                )
                PFButton(
                    title = stringResource(R.string.delete_account),
                    onClick = onOpenDeleteAccount,
                    variant = PFButtonVariant.Tonal,
                    leadingIcon = PFIcons.Delete,
                    isDestructive = true,
                )
                Text(
                    text = stringResource(R.string.permanently_remove_your_account_and_all_data),
                    style = PFTheme.type.caption,
                    color = PFTheme.colors.onSurfaceFaint,
                )
            }
        }
    }
}

/** Decodes, downscales to 512px on the long edge, and JPEG-encodes at 85%. */
private fun downscaleToJpeg(context: android.content.Context, uri: Uri): ByteArray? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

    val longest = maxOf(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
    var sample = 1
    while (longest / sample > 1024) sample *= 2

    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    val decoded = context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, options)
    } ?: return@runCatching null

    val scale = 512f / maxOf(decoded.width, decoded.height).toFloat()
    val scaled = if (scale < 1f) {
        Bitmap.createScaledBitmap(
            decoded,
            (decoded.width * scale).toInt().coerceAtLeast(1),
            (decoded.height * scale).toInt().coerceAtLeast(1),
            true,
        )
    } else {
        decoded
    }

    ByteArrayOutputStream().use { output ->
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, output)
        output.toByteArray()
    }
}.getOrNull()
