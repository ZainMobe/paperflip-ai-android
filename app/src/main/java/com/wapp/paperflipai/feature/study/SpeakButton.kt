package com.wapp.paperflipai.feature.study

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wapp.paperflipai.R
import com.wapp.paperflipai.app.LocalAppEnvironment
import com.wapp.paperflipai.designsystem.PFIcons
import com.wapp.paperflipai.designsystem.modifier.PFHaptic
import com.wapp.paperflipai.designsystem.modifier.pfPressable
import com.wapp.paperflipai.designsystem.theme.PFTheme

/**
 * Small "read aloud" control used on card faces. Shows a stop glyph while
 * its own text is being spoken. Counterpart of `PFSpeakButton.swift`.
 */
@Composable
fun SpeakButton(text: String, id: String, modifier: Modifier = Modifier) {
    val speech = LocalAppEnvironment.current.speech
    val speakingId by speech.speakingId.collectAsStateWithLifecycle()
    val active = speakingId == id
    val description = stringResource(if (active) R.string.speak_stop else R.string.speak_read_aloud)
    Box(
        modifier = modifier
            .size(36.dp)
            .background(if (active) PFTheme.colors.accentSoft else PFTheme.colors.surface.copy(alpha = 0.6f), CircleShape)
            .pfPressable(onClick = { speech.toggle(text, id) }, haptic = PFHaptic.Light, pressedScale = 0.9f)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(targetState = active, label = "speak") { on ->
            Icon(
                imageVector = if (on) PFIcons.StopCircle else PFIcons.Speaker,
                contentDescription = null,
                tint = if (on) PFTheme.colors.accent else PFTheme.colors.onSurfaceMuted,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
