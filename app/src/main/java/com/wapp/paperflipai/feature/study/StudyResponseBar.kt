package com.wapp.paperflipai.feature.study

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wapp.paperflipai.core.data.PFFlashcard
import com.wapp.paperflipai.core.srs.SM2
import com.wapp.paperflipai.core.srs.SRSResponse
import com.wapp.paperflipai.designsystem.modifier.PFHaptic
import com.wapp.paperflipai.designsystem.modifier.pfPressable
import com.wapp.paperflipai.designsystem.theme.PFRadius
import com.wapp.paperflipai.designsystem.theme.PFTheme

/**
 * The four-button row under the study card — the Android counterpart of
 * `StudyResponseBar.swift`. Each button shows the interval SM-2 would
 * assign, borrowed from Anki, so the choice is legible before committing.
 */
@Composable
fun StudyResponseBar(
    card: PFFlashcard,
    onResponse: (SRSResponse) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PFTheme.spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm),
    ) {
        SRSResponse.entries.forEach { response ->
            val preview = SM2.update(SM2.stateOf(card), response)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(response.color(), RoundedCornerShape(PFRadius.lg))
                    .pfPressable(
                        onClick = { onResponse(response) },
                        haptic = PFHaptic.Medium,
                        pressedScale = 0.94f,
                    )
                    .padding(vertical = PFTheme.spacing.md),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.xs),
            ) {
                Icon(
                    imageVector = response.icon,
                    contentDescription = null,
                    tint = PFTheme.colors.onAccent,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = stringResource(response.labelRes),
                    style = PFTheme.type.footnoteBold,
                    color = PFTheme.colors.onAccent,
                    maxLines = 1,
                )
                Text(
                    text = intervalLabel(preview.interval),
                    style = PFTheme.type.caption,
                    color = PFTheme.colors.onAccent,
                    modifier = Modifier.alpha(0.7f),
                )
            }
        }
    }
}
