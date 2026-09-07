package com.wapp.paperflipai.widget

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.wapp.paperflipai.MainActivity
import com.wapp.paperflipai.R

/**
 * Home-screen widget — the Android counterpart of `PaperFlipWidget.swift`.
 *
 * Same two layouts as iOS:
 *  - "small"  (≈2x2 cell)  → big due count + a streak pill in the corner
 *  - "medium" (≈4x2 cell)  → streak block on the left, due count + CTA on the right
 *
 * iOS reads its numbers out of an App Group; on Android the widget runs in
 * the app's own process, so [WidgetSnapshotStore] (SharedPreferences) is the
 * shared surface. The app calls `WidgetSnapshotStore.update(...)` after every
 * sync and study session, which re-renders every placed widget.
 *
 * Tapping anywhere opens `paperflip://study`, exactly like the iOS
 * `.widgetURL(...)`, which `IntentInbox` routes into a study session.
 */
class PaperflipWidget : GlanceAppWidget() {

    /**
     * Glance hands us the actual cell size, so we can pick the iOS "small"
     * vs "medium" body instead of shipping two separate widget providers.
     */
    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(SMALL_SIZE, MEDIUM_SIZE)
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetSnapshotStore.read(context)
        val palette = WidgetPalette.forContext(context)
        val copy = WidgetCopy.forContext(context, snapshot.dueToday)

        provideContent {
            GlanceTheme {
                WidgetBody(
                    due = snapshot.dueToday,
                    streak = snapshot.streakDays,
                    palette = palette,
                    copy = copy,
                )
            }
        }
    }

    companion object {
        val SMALL_SIZE = DpSize(150.dp, 110.dp)
        val MEDIUM_SIZE = DpSize(280.dp, 110.dp)
    }
}

/* ------------------------------------------------------------------ */
/*  Palette                                                            */
/* ------------------------------------------------------------------ */

/**
 * The widget process can't read `LocalPFColors`, so — exactly like the iOS
 * widget target, which inlines its own copy of the tokens — the handful of
 * colors it needs are duplicated here. Keep in sync with `PFColor.kt`.
 */
internal data class WidgetPalette(
    val paper: Color,
    val ink: Color,
    val muted: Color,
    val accent: Color,
    val accentSoft: Color,
    val flame: Color,
    val flameSoft: Color,
    val divider: Color,
) {
    companion object {
        private val Light = WidgetPalette(
            paper = Color(0xFFFBF9F4),
            ink = Color(0xFF0A0A12),
            muted = Color(0xFF6B6B7A),
            accent = Color(0xFF5B5BD6),
            accentSoft = Color(0xFFEEEEFB),
            flame = Color(0xFFEA580C),
            flameSoft = Color(0x1FEA580C),
            divider = Color(0x146B6B7A),
        )

        private val Dark = WidgetPalette(
            paper = Color(0xFF1A1814),
            ink = Color(0xFFF3F3F8),
            muted = Color(0xFF9A9AA8),
            accent = Color(0xFF8A8AFF),
            accentSoft = Color(0xFF252543),
            flame = Color(0xFFFB923C),
            flameSoft = Color(0x33FB923C),
            divider = Color(0x1FFFFFFF),
        )

        fun forContext(context: Context): WidgetPalette {
            val night = (context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            return if (night) Dark else Light
        }
    }
}

/** Pre-resolved strings — `stringResource` isn't available inside Glance. */
internal data class WidgetCopy(
    val dueTodayLong: String,
    val dueShort: String,
    val todayLabel: String,
    val dayStreak: String,
    val cta: String,
) {
    companion object {
        fun forContext(context: Context, due: Int): WidgetCopy {
            val res = context.resources
            return WidgetCopy(
                dueTodayLong = res.getQuantityString(R.plurals.widget_cards_due_today, due),
                dueShort = res.getQuantityString(R.plurals.widget_cards_due, due),
                todayLabel = res.getString(R.string.widget_today_label),
                dayStreak = res.getString(R.string.widget_day_streak),
                cta = res.getString(
                    if (due > 0) R.string.widget_tap_to_study else R.string.widget_open_app
                ),
            )
        }
    }
}

/* ------------------------------------------------------------------ */
/*  Body                                                               */
/* ------------------------------------------------------------------ */

@Composable
private fun WidgetBody(
    due: Int,
    streak: Int,
    palette: WidgetPalette,
    copy: WidgetCopy,
) {
    val size = LocalSize.current
    val wide = size.width >= 230.dp

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(palette.paper))
            .cornerRadius(20.dp)
            .padding(14.dp)
            .clickable(actionStartActivity(studyIntent(LocalContext.current)))
    ) {
        if (wide) {
            MediumBody(due = due, streak = streak, palette = palette, copy = copy)
        } else {
            SmallBody(due = due, streak = streak, palette = palette, copy = copy)
        }
    }
}

/** iOS `.systemSmall`. */
@Composable
private fun SmallBody(
    due: Int,
    streak: Int,
    palette: WidgetPalette,
    copy: WidgetCopy,
) {
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppMark(palette)
            Spacer(modifier = GlanceModifier.defaultWeight())
            StreakPill(streak = streak, palette = palette)
        }

        Spacer(modifier = GlanceModifier.defaultWeight())

        Text(
            text = due.toString(),
            style = TextStyle(
                color = ColorProvider(palette.ink),
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        Text(
            text = copy.dueTodayLong,
            maxLines = 1,
            style = TextStyle(
                color = ColorProvider(palette.muted),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

/** iOS `.systemMedium`. */
@Composable
private fun MediumBody(
    due: Int,
    streak: Int,
    palette: WidgetPalette,
    copy: WidgetCopy,
) {
    Row(modifier = GlanceModifier.fillMaxSize()) {
        // LEFT — app mark + streak block
        Column(modifier = GlanceModifier.defaultWeight().fillMaxHeight()) {
            AppMark(palette)
            Spacer(modifier = GlanceModifier.defaultWeight())
            StreakBlock(streak = streak, palette = palette, label = copy.dayStreak)
        }

        // Hairline divider, matching the iOS `Divider().overlay(...)`
        Box(
            modifier = GlanceModifier
                .width(1.dp)
                .fillMaxHeight()
                .background(ColorProvider(palette.divider))
        ) {}

        Spacer(modifier = GlanceModifier.width(14.dp))

        // RIGHT — due count + study CTA
        Column(modifier = GlanceModifier.defaultWeight().fillMaxHeight()) {
            Text(
                text = copy.todayLabel,
                style = TextStyle(
                    color = ColorProvider(palette.accent),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = due.toString(),
                style = TextStyle(
                    color = ColorProvider(palette.ink),
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Text(
                text = copy.dueShort,
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(palette.muted),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            StudyChip(label = copy.cta, palette = palette)
        }
    }
}

/* ------------------------------------------------------------------ */
/*  Pieces                                                             */
/* ------------------------------------------------------------------ */

@Composable
private fun AppMark(palette: WidgetPalette) {
    Box(
        modifier = GlanceModifier
            .size(28.dp)
            .background(ColorProvider(palette.accentSoft))
            .cornerRadius(14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_widget_stack),
            contentDescription = null,
            colorFilter = ColorFilter.tint(ColorProvider(palette.accent)),
            modifier = GlanceModifier.size(15.dp),
        )
    }
}

@Composable
private fun StreakPill(streak: Int, palette: WidgetPalette) {
    Row(
        modifier = GlanceModifier
            .background(ColorProvider(palette.flameSoft))
            .cornerRadius(10.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_widget_flame),
            contentDescription = null,
            colorFilter = ColorFilter.tint(ColorProvider(palette.flame)),
            modifier = GlanceModifier.size(11.dp),
        )
        Spacer(modifier = GlanceModifier.width(3.dp))
        Text(
            text = streak.toString(),
            style = TextStyle(
                color = ColorProvider(palette.flame),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

@Composable
private fun StreakBlock(streak: Int, palette: WidgetPalette, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = GlanceModifier
                .size(34.dp)
                .background(ColorProvider(palette.flameSoft))
                .cornerRadius(17.dp),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_flame),
                contentDescription = null,
                colorFilter = ColorFilter.tint(ColorProvider(palette.flame)),
                modifier = GlanceModifier.size(16.dp),
            )
        }
        Spacer(modifier = GlanceModifier.width(8.dp))
        Column {
            Text(
                text = streak.toString(),
                style = TextStyle(
                    color = ColorProvider(palette.ink),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Text(
                text = label,
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(palette.muted),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
    }
}

@Composable
private fun StudyChip(label: String, palette: WidgetPalette) {
    Row(
        modifier = GlanceModifier
            .background(ColorProvider(palette.accent))
            .cornerRadius(12.dp)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            maxLines = 1,
            style = TextStyle(
                color = ColorProvider(Color.White),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        Spacer(modifier = GlanceModifier.width(4.dp))
        Image(
            provider = ImageProvider(R.drawable.ic_widget_arrow),
            contentDescription = null,
            colorFilter = ColorFilter.tint(ColorProvider(Color.White)),
            modifier = GlanceModifier.size(10.dp),
        )
    }
}

/** Mirrors the iOS `.widgetURL(URL(string: "paperflip://study"))`. */
private fun studyIntent(context: Context): Intent =
    Intent(context, MainActivity::class.java).apply {
        action = Intent.ACTION_VIEW
        data = Uri.parse("paperflip://study")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
