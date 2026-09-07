package com.wapp.paperflipai.feature.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wapp.paperflipai.R
import com.wapp.paperflipai.core.data.PFProjectRole
import com.wapp.paperflipai.designsystem.PFIcons
import com.wapp.paperflipai.designsystem.theme.PFTheme
import java.util.Locale

/** Display name, icon, tint and sort order for a project role. */
val PFProjectRole.displayNameRes: Int
    get() = when (this) {
        PFProjectRole.Owner -> R.string.owner_2
        PFProjectRole.Editor -> R.string.editor
        PFProjectRole.Viewer -> R.string.viewer
    }

val PFProjectRole.icon: ImageVector
    get() = when (this) {
        PFProjectRole.Owner -> PFIcons.Pro
        PFProjectRole.Editor -> PFIcons.Edit
        PFProjectRole.Viewer -> PFIcons.Reveal
    }

val PFProjectRole.sortKey: Int
    get() = when (this) {
        PFProjectRole.Owner -> 0
        PFProjectRole.Editor -> 1
        PFProjectRole.Viewer -> 2
    }

@Composable
fun PFProjectRole.tint(): Color = when (this) {
    PFProjectRole.Owner -> PFTheme.colors.warning
    PFProjectRole.Editor -> PFTheme.colors.accent
    PFProjectRole.Viewer -> PFTheme.colors.onSurfaceMuted
}

@Composable
fun RolePill(role: PFProjectRole, modifier: Modifier = Modifier) {
    val tint = role.tint()
    Text(
        text = stringResource(role.displayNameRes).uppercase(Locale.getDefault()),
        style = PFTheme.type.caption,
        color = tint,
        modifier = modifier
            .background(tint.copy(alpha = 0.14f), CircleShape)
            .padding(horizontal = PFTheme.spacing.sm, vertical = 3.dp),
    )
}
