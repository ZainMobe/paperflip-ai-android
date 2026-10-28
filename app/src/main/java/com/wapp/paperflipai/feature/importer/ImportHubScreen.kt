package com.wapp.paperflipai.feature.importer

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wapp.paperflipai.R
import com.wapp.paperflipai.app.IntentInbox
import com.wapp.paperflipai.app.LocalAppEnvironment
import com.wapp.paperflipai.app.PFAction
import com.wapp.paperflipai.core.data.DeckDto
import com.wapp.paperflipai.core.data.PFSourceType
import com.wapp.paperflipai.core.data.newId
import com.wapp.paperflipai.core.importing.AnkiPackageImporter
import com.wapp.paperflipai.core.importing.UrlExtractor
import com.wapp.paperflipai.designsystem.PFIcons
import com.wapp.paperflipai.designsystem.component.PFScreen
import com.wapp.paperflipai.designsystem.component.PFTagPill
import com.wapp.paperflipai.designsystem.component.PFToast
import com.wapp.paperflipai.designsystem.component.PFTopBar
import com.wapp.paperflipai.designsystem.component.tabBarContentPadding
import com.wapp.paperflipai.designsystem.modifier.pfPressable
import com.wapp.paperflipai.designsystem.modifier.pfReadableWidth
import com.wapp.paperflipai.designsystem.theme.PFElevation
import com.wapp.paperflipai.designsystem.theme.PFRadius
import com.wapp.paperflipai.designsystem.theme.PFTheme
import com.wapp.paperflipai.designsystem.theme.pfElevation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.wapp.paperflipai.core.analytics.PaywallSource
import com.wapp.paperflipai.core.analytics.AnalyticsEvent
import com.wapp.paperflipai.core.analytics.PFAnalytics

/**
 * Entry point for creating a deck — the Android counterpart of
 * `ImportHubView.swift`. Three source cards plus the Pro-only shared-link
 * import; each one routes into [SourceInputScreen] with the type pre-set.
 *
 * A URL or PDF shared into the app from another app arrives through the
 * [IntentInbox] and pre-fills the matching source.
 */
@Composable
fun ImportHubScreen(
    onPickSource: (sourceType: String, shared: String?) -> Unit,
    onOpenPaywall: (PaywallSource) -> Unit,
    onOpenPasteCards: () -> Unit,
    onOpenDraftReview: () -> Unit,
) {
    val env = LocalAppEnvironment.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val entitlement by env.entitlements.entitlement.collectAsStateWithLifecycle()
    val pendingAction by IntentInbox.pending.collectAsStateWithLifecycle()
    var showSharedLink by remember { mutableStateOf(false) }
    var isImportingAnki by remember { mutableStateOf(false) }

    val ankiSummary = stringResource(R.string.anki_deck_summary)
    val ankiErrors = mapOf(
        AnkiPackageImporter.ImportError.Reason.NewFormat to stringResource(R.string.anki_err_new_format),
        AnkiPackageImporter.ImportError.Reason.NoCollection to stringResource(R.string.anki_err_no_collection),
        AnkiPackageImporter.ImportError.Reason.Database to stringResource(R.string.anki_err_database),
        AnkiPackageImporter.ImportError.Reason.Empty to stringResource(R.string.anki_err_empty),
        AnkiPackageImporter.ImportError.Reason.NotAnArchive to stringResource(R.string.anki_err_not_archive),
    )
    val ankiReadFailed = stringResource(R.string.anki_err_read)
    val ankiSkippedTemplate = stringResource(R.string.anki_skipped_notes)

    // Anki .apkg: parsed on the device, then straight to review. No AI, no credit.
    val ankiPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        isImportingAnki = true
        scope.launch {
            try {
                val (name, bytes) = withContext(Dispatchers.IO) {
                    val name = context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                        val i = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (i >= 0 && c.moveToFirst()) c.getString(i) else null
                    } ?: "Anki import.apkg"
                    name to (context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: error("unreadable"))
                }
                val imported = withContext(Dispatchers.IO) {
                    AnkiPackageImporter.import(bytes, name, context.cacheDir)
                }
                ImportDraft.put(
                    DeckDto(
                        id = newId(),
                        userId = env.auth.session.value?.userId.orEmpty(),
                        title = imported.title,
                        summary = ankiSummary.format(imported.cards.size),
                        sourceType = PFSourceType.Text.raw,
                        cards = imported.cards.map { it.copy(id = newId()) },
                    )
                )
                PFAnalytics.log(AnalyticsEvent.ankiImportCompleted(imported.cards.size, imported.skipped))
                if (imported.skipped > 0) PFToast.info(ankiSkippedTemplate.format(imported.skipped))
                onOpenDraftReview()
            } catch (failure: AnkiPackageImporter.ImportError) {
                PFAnalytics.log(AnalyticsEvent.importFailed("anki", "parse", failure.reason.name))
                PFToast.error(ankiErrors[failure.reason] ?: ankiReadFailed)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                PFAnalytics.error(failure, "anki_import")
                PFToast.error(ankiReadFailed)
            } finally {
                isImportingAnki = false
            }
        }
    }

    // Share-sheet hand-off: pick the source type from the shared payload.
    LaunchedEffect(pendingAction) {
        when (val action = pendingAction) {
            is PFAction.ImportSharedText -> {
                IntentInbox.clear()
                val text = action.text
                val type = when {
                    text.contains("youtube.com", true) || text.contains("youtu.be", true) ->
                        PFSourceType.Youtube
                    UrlExtractor.firstUrl(text) != null -> PFSourceType.Article
                    // Shared text with no link at all: treat it as notes.
                    else -> PFSourceType.Text
                }
                onPickSource(type.raw, text)
            }
            is PFAction.ImportSharedPdf -> {
                IntentInbox.clear()
                onPickSource(PFSourceType.Pdf.raw, action.uri)
            }
            else -> Unit
        }
    }

    PFScreen(topBar = { PFTopBar(title = stringResource(R.string.import_action)) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .pfReadableWidth()
                .padding(horizontal = PFTheme.spacing.lg)
                .padding(bottom = tabBarContentPadding(PFTheme.spacing.lg)),
            verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.xl),
        ) {
            Column(
                modifier = Modifier.padding(top = PFTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm),
            ) {
                Text(
                    text = stringResource(R.string.create_a_new_deck),
                    style = PFTheme.type.overline,
                    color = PFTheme.colors.accent,
                )
                Text(
                    text = stringResource(R.string.what_are_you_learning),
                    style = PFTheme.type.title1,
                    color = PFTheme.colors.onSurface,
                )
                Text(
                    text = stringResource(R.string.pick_a_source_we_ll_handle_the_rest),
                    style = PFTheme.type.callout,
                    color = PFTheme.colors.onSurfaceMuted,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.md)) {
                SourceCard(
                    icon = PFIcons.Pdf,
                    tint = PFTheme.colors.accent,
                    title = stringResource(R.string.from_a_pdf),
                    body = stringResource(R.string.drop_in_a_pdf_and_we_ll_generate_cards_from_ea),
                    onClick = { onPickSource(PFSourceType.Pdf.raw, null) },
                )
                SourceCard(
                    icon = PFIcons.Video,
                    tint = PFTheme.colors.danger,
                    title = stringResource(R.string.from_a_youtube_video),
                    body = stringResource(R.string.paste_a_url_or_share_from_the_youtube_app),
                    onClick = { onPickSource(PFSourceType.Youtube.raw, null) },
                )
                SourceCard(
                    icon = PFIcons.Article,
                    tint = PFTheme.colors.warning,
                    title = stringResource(R.string.from_an_article),
                    body = stringResource(R.string.hub_article_body),
                    onClick = { onPickSource(PFSourceType.Article.raw, null) },
                )
                SourceCard(
                    icon = PFIcons.DocumentScan,
                    tint = PFTheme.colors.info,
                    title = stringResource(R.string.source_title_photo),
                    body = stringResource(R.string.hub_photo_body),
                    onClick = { onPickSource(PFSourceType.Photo.raw, null) },
                )
                SourceCard(
                    icon = PFIcons.Waveform,
                    tint = PFTheme.colors.hard,
                    title = stringResource(R.string.source_title_lecture),
                    body = stringResource(R.string.hub_lecture_body),
                    onClick = { onPickSource(PFSourceType.Audio.raw, null) },
                )
                SourceCard(
                    icon = PFIcons.Notes,
                    tint = PFTheme.colors.success,
                    title = stringResource(R.string.source_title_notes),
                    body = stringResource(R.string.hub_notes_body),
                    onClick = { onPickSource(PFSourceType.Text.raw, null) },
                )
                SourceCard(
                    icon = PFIcons.Paste,
                    tint = PFTheme.colors.onSurface,
                    title = stringResource(R.string.paste_title),
                    body = stringResource(R.string.hub_paste_body),
                    onClick = onOpenPasteCards,
                )
                SourceCard(
                    icon = PFIcons.Archive,
                    tint = PFTheme.colors.onSurface,
                    title = stringResource(R.string.anki_title),
                    body = stringResource(
                        if (isImportingAnki) R.string.anki_importing else R.string.hub_anki_body
                    ),
                    onClick = {
                        if (!isImportingAnki) {
                            // .apkg has no registered MIME type on most devices.
                            PFAnalytics.log(AnalyticsEvent.importSourceSelected("anki"))
                            ankiPicker.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                        }
                    },
                )
                SourceCard(
                    icon = PFIcons.Link,
                    tint = PFTheme.colors.success,
                    title = stringResource(R.string.from_a_shared_link),
                    body = stringResource(
                        if (entitlement.isActive) R.string.paste_a_paperflip_ai_share_link_no_credit_need
                        else R.string.import_shared_decks_instantly_pro_feature
                    ),
                    proBadge = !entitlement.isActive,
                    onClick = {
                        if (entitlement.isActive) showSharedLink = true else onOpenPaywall(PaywallSource.SharedLinkImport)
                    },
                )
            }

            if (!entitlement.isActive) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PFTheme.colors.accentSoft, RoundedCornerShape(PFRadius.lg))
                        .pfPressable(onClick = { onOpenPaywall(PaywallSource.ImportProHint) }, pressedScale = 0.985f)
                        .padding(PFTheme.spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = PFIcons.Sparkle,
                        contentDescription = null,
                        tint = PFTheme.colors.accent,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(PFTheme.spacing.sm))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.free_plan),
                            style = PFTheme.type.footnoteBold,
                            color = PFTheme.colors.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.n_5_decks_per_month_20_cards_per_deck),
                            style = PFTheme.type.caption,
                            color = PFTheme.colors.onSurfaceMuted,
                        )
                    }
                    PFTagPill(title = stringResource(R.string.pro), prominent = true)
                }
            }
        }
    }

    if (showSharedLink) {
        SharedLinkImportSheet(onDismiss = { showSharedLink = false })
    }
}

@Composable
private fun SourceCard(
    icon: ImageVector,
    tint: Color,
    title: String,
    body: String,
    onClick: () -> Unit,
    proBadge: Boolean = false,
) {
    val shape = RoundedCornerShape(PFRadius.xl)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pfElevation(PFElevation.Card, shape)
            .background(PFTheme.colors.elevated, shape)
            .border(0.7.dp, PFTheme.colors.border, shape)
            .pfPressable(onClick = onClick, pressedScale = 0.985f)
            .padding(PFTheme.spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(tint.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.width(PFTheme.spacing.md))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = PFTheme.type.headline,
                    color = PFTheme.colors.onSurface,
                )
                if (proBadge) {
                    Spacer(Modifier.width(PFTheme.spacing.xs))
                    PFTagPill(
                        title = stringResource(R.string.pro),
                        icon = PFIcons.Pro,
                        prominent = true,
                    )
                }
            }
            Text(
                text = body,
                style = PFTheme.type.footnote,
                color = PFTheme.colors.onSurfaceMuted,
            )
        }
        Spacer(Modifier.width(PFTheme.spacing.sm))
        Icon(
            imageVector = PFIcons.Chevron,
            contentDescription = null,
            tint = PFTheme.colors.onSurfaceFaint,
            modifier = Modifier.size(18.dp),
        )
    }
}
