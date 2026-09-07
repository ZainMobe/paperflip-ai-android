# PaperFlip Android — Build Notes

A Kotlin / Jetpack Compose port of the SwiftUI app in `Paperflip AI iOS`.
Same screens, same flows, same design tokens; Material 3 components,
Android navigation, Android motion.

**116 Kotlin files · ~20,800 lines · 10 phases · full mock backend.**

---

## 1. Read this first: the app has never been compiled

Everything here was written without a compiler. The machine this was ported
on could not reach `dl.google.com`, Maven Central or `services.gradle.org`
(HTTP 403 through the sandbox proxy), so Gradle could not resolve a single
dependency, let alone build.

That constraint shaped the whole port:

* **No annotation processors.** No Room, no KSP, no Hilt, no Dagger. Every
  one of those fails at *generation* time in ways that are impossible to
  predict from source. Persistence is hand-rolled JSON; DI is a plain object
  graph.
* **Conservative, pinned versions.** Nothing floats, nothing uses `+`, no
  alphas. See §3.
* **One indirection for icons.** Every Material icon in the app is referenced
  exactly once, in `designsystem/PFIcons.kt`. If an icon name is wrong, it is
  a one-line fix in one file, not a hunt through 116 files.
* **Static verification instead of a build.** See §2.

**So: expect to fix things in Android Studio.** The realistic failure modes,
in order of likelihood, are (a) a Material icon that has been renamed, (b) a
Compose API whose signature moved in the BOM you resolve, (c) a deprecation
warning you'd rather clean up. All are minutes of work, and all are localised.

### First build

```bash
# 1. Secrets (optional — without them the app runs on mocks, like iOS
#    does when Config.xcconfig is absent)
cat >> local.properties <<'EOF'
SUPABASE_URL=
SUPABASE_ANON_KEY=
REVENUECAT_API_KEY=
GOOGLE_WEB_CLIENT_ID=
EOF

# 2. Build
./gradlew :app:assembleDebug
```

`local.properties` is gitignored. Secrets reach the app only through
`buildConfigField`, never through a checked-in file, and an empty value is a
supported state — `AppEnvironment` falls back to the Mock service.

---

## 2. What *was* verified, without compiling

Scripted checks that were run over the whole source tree, all currently clean:

| Check | Result |
|---|---|
| Every `R.string/plurals/drawable/mipmap/raw/xml/style/color` reference resolves to a defined resource | 0 missing |
| Brace / paren / bracket balance, comment- and string-aware | 0 imbalances |
| Every `env.<service>.<member>` call exists on the declared interface | 0 missing |
| Unresolved type or function names per file (imports + same-package + stdlib) | 0 real |
| Experimental Compose APIs used without a matching `@OptIn` | 0 remaining |
| `by` state delegates missing `getValue` / `setValue` imports | 0 remaining |
| `@Composable`-only calls inside `onClick` / `launch {}` / non-composable lambdas | 0 |
| Duplicate top-level declarations, duplicate resource names | 0 |
| Every XML resource parses | clean |

Bugs these checks actually caught and that are now fixed: a missing
`getValue` import in `PFLottie`, a missing `@OptIn(ExperimentalFoundationApi)`
in `pfPressable`, a `widthIn` cap that an outer `fillMaxWidth()` silently
defeated (see §7), and a fully-qualified `LocalContext` left inline.

What these checks **cannot** see: type mismatches, wrong argument counts,
nullability, generic inference, and lambda-scope receivers. The first real
build proved the point — see §2a for exactly what got through.

### 2a. What the first real build caught

Five things, all fixed. They are recorded here because each one is a class of
bug the static checks structurally could not see.

1. **`compileSdk` / `targetSdk` → 37.** `androidx.core:core:1.19.0` requires
   API 37 to compile. Note that `compileSdk` was the *forced* half:
   `targetSdk = 37` is a separate, behavioural promise — it opts the app into
   every API 37 behaviour change at runtime, which no compiler will warn about.
   If anything misbehaves on a new device (background work, notifications,
   edge-to-edge insets), dropping `targetSdk` back to 36 while leaving
   `compileSdk = 37` is a legitimate and reversible first test.

2. **`R.string.new` → `R.string.new_status`.** `new` is a Java keyword, so
   `aapt2` refuses to generate the field. This is a **generator bug, not a
   one-off**: the xcstrings → XML converter renames `import`, `continue`, `in`
   and `public`, but its keyword list was incomplete. Anyone regenerating
   `strings_generated.xml` from the iOS catalogue must add `new` to that list —
   along with the rest of the Java reserved words (`class`, `int`, `long`,
   `float`, `double`, `boolean`, `char`, `byte`, `short`, `void`, `if`, `else`,
   `for`, `while`, `do`, `switch`, `case`, `default`, `break`, `return`, `try`,
   `catch`, `finally`, `throw`, `throws`, `this`, `super`, `null`, `true`,
   `false`, `static`, `final`, `abstract`, `native`, `package`, `private`,
   `protected`, `synchronized`, `transient`, `volatile`, `extends`,
   `implements`, `interface`, `enum`, `instanceof`, `assert`, `goto`, `const`)
   — or this will come back on the next regeneration.

3. **`StudyCard.kt`** — a `Density` object used where a `Float` was wanted;
   the multiplication needs `density.density`.

4. **`PFTextField.kt`** — `LocalTextSelectionColors` and `TextSelectionColors`
   live in `androidx.compose.foundation.text.selection`, not where they were
   imported from.

5. **A project-wide Material 3 opt-in**, added as a compiler argument:

   ```kotlin
   freeCompilerArgs.add("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
   ```

   This works and is what most Compose projects end up doing. The trade-off is
   worth knowing: it silences the opt-in requirement for the *whole module*, so
   a genuinely unstable Material 3 API can now be adopted anywhere without
   anyone noticing. The per-file `@OptIn` annotations are still in the source
   and are now redundant, not wrong — if you ever want the warnings back,
   delete the compiler argument rather than the annotations.

---

## 3. Why these versions

`gradle/libs.versions.toml` is deliberately boring:

| | | why |
|---|---|---|
| AGP `9.2.1`, Kotlin `2.2.20` | as generated | untouched from the empty project you created — the one part of the build that was known-good |
| Compose BOM `2025.06.00` | stable | the BOM pins every Compose artifact together; bumping *only* this line is the safest way to move Compose forward |
| navigation-compose `2.9.0`, lifecycle `2.9.0` | stable | `2.9.x` is the last line before the nav3 rewrite; the `NavHost` here is the classic string-route API |
| coroutines `1.9.0`, serialization `1.7.3` | stable | conservative on purpose: `kotlinx-serialization` 1.7.x has the plugin/runtime compatibility that Kotlin 2.2 expects |
| lottie-compose `6.4.0` | stable | 6.x is the Compose-first line |
| glance `1.1.1` | stable | the widget API surface below is 1.1.x; 1.2 alphas move `GlanceTheme` |

### AGP 9 has built-in Kotlin — do not re-add `kotlin.android`

The first build failed on this, so it is worth stating plainly. Since AGP 9.0,
`com.android.application` compiles Kotlin itself, and applying
`org.jetbrains.kotlin.android` next to it is a **hard error**, not a warning:

```
The 'org.jetbrains.kotlin.android' plugin is no longer required for Kotlin
support since AGP 9.0.
```

So the `plugins {}` block in `app/build.gradle.kts` — and in the root build
file — deliberately does *not* contain it, and the alias has been deleted from
`libs.versions.toml` so it cannot creep back in via autocomplete.

What stays, because AGP does **not** absorb them:

* `org.jetbrains.kotlin.plugin.compose` — the Compose compiler is still its own
  plugin; `buildFeatures { compose = true }` does not apply it.
* `org.jetbrains.kotlin.plugin.serialization` — needed for the `@Serializable`
  model graph.

Both are pinned to `kotlin = "2.2.20"`. AGP 9 carries a floor of KGP 2.2.10 and
will *upgrade* to a higher declared version but refuses to downgrade, so 2.2.20
is a legal ask. If you ever see a "Compose compiler version does not match
Kotlin version" error, that one `kotlin` line in the catalog is the fix.

Compiler options use the top-level `kotlin { compilerOptions { … } }` block —
`android { kotlinOptions { … } }` is deprecated under built-in Kotlin. The
`jvmTarget` set there is technically redundant (it defaults to
`compileOptions.targetCompatibility`, which is 17) but it is left explicit so
the Java and Kotlin targets can't silently drift apart.

`minSdk 33` is what the empty project shipped with, and the port leans on it:
per-app language uses the **platform** `LocaleManager` rather than AppCompat,
and `Modifier.cornerRadius` in Glance needs API 31+.

If Gradle can't resolve something, bump *one* line at a time — the BOM first.

---

## 4. Architecture map (and where iOS maps to)

```
app/          PaperflipApp, MainScaffold, Routes, AppEnvironment, IntentInbox
              ← RootView, RootRouter, RootTabBar, AppEnvironment, IntentInbox
core/data     Models, Dtos, PaperflipDatabase, PaperflipRepository
              ← Models, DTOs, PersistenceController
core/srs      SM2, StudyQueue, StreakTracker            ← same names
core/network  RemoteStore + MockRemoteStore, NetworkMonitor
core/sync     SyncEngine                                 ← SyncEngine
core/auth     AuthService + Mock, SessionStore, GoogleSignInCoordinator
              ← AuthService, KeychainStore, AppleSignInCoordinator
core/monetization  EntitlementStore + Mock              ← same
core/notifications NotificationsClient, ReminderReceiver, BootReceiver
designsystem/ PFColor, PFSpacing, PFType, PFMotion, PFElevation, PFIcons,
              component/*, modifier/*                    ← the PF* token files
feature/      onboarding, auth, library, importer, study, projects, stats,
              settings, paywall, notifications, support
widget/       PaperflipWidget (Glance), WidgetSnapshotStore
              ← PaperFlipWidget.swift, WidgetSnapshotWriter.swift
```

**DI**: one `AppEnvironment` built in `PaperflipApplication`, handed down
through `LocalAppEnvironment`. Exactly the iOS pattern, no framework.

**Persistence**: the entire database is one `PaperflipSnapshot`
(`@Serializable`) held in a `StateFlow` and written to a single JSON file
with a debounce. It is honest about what it is — see §8 for swapping in Room.

**Navigation**: a single `NavHost`. `PFBottomBar` renders only on the five
top-level routes (`Route.topLevel`), which is why the bar animates away on
detail screens the way the iOS tab bar hides.

---

## 5. Where Android deliberately differs from iOS

These are decisions, not gaps. Each one is a place where a 1:1 port would
have produced something that felt wrong on Android.

| iOS | Android | why |
|---|---|---|
| Sign in with Apple | Google sign-in | Apple sign-in has no meaning here; see §6 to finish wiring it |
| Context menu (long-press) | Bottom sheet (`DeckActionsSheet`) | Android's long-press affordance is a sheet, not a floating menu |
| Drag-to-reorder | Explicit up / down buttons (`PFIcons.MoveUp/MoveDown`) | reorder-by-drag in a `LazyColumn` without a library is a bug farm, and buttons are more accessible |
| Inline `DatePicker` | Material `TimePicker` dialog | `ReminderTimePicker.kt` |
| App Store wording | Google Play wording | `strings.xml`, not the generated catalogue |
| `WidgetCenter.reloadTimelines` | `GlanceAppWidget.updateAll` | `WidgetSnapshotStore.update()` |
| App Intents / Siri | Launcher long-press shortcuts | `res/xml/shortcuts.xml`; both post to `IntentInbox` |
| App Group `UserDefaults` | `SharedPreferences` | the widget runs in the app's process on Android |

Additions with no iOS counterpart, because Android users expect them:
edge-to-edge with `enableEdgeToEdge()`, predictive back
(`enableOnBackInvokedCallback`), Material ripples on every pressable,
adaptive + monochrome launcher icon, per-app language in system settings,
and a `paperflip://import` deep link behind the "New deck" shortcut.

---

## 6. Google sign-in — the one thing left to wire

`core/auth/GoogleSignInCoordinator.kt` exposes the real contract —
`suspend fun signIn(): GoogleSignInResult`, plus the `rawNonce()` /
`hashedNonce()` pair Google's flow needs — but its body currently returns a
mock token, because the Credential Manager dependencies could not be resolved
here, let alone tested. To finish:

```kotlin
// libs.versions.toml
credentials = "1.3.0"
googleid = "1.1.1"

androidx-credentials = { group = "androidx.credentials", name = "credentials", version.ref = "credentials" }
androidx-credentials-play-services = { group = "androidx.credentials", name = "credentials-play-services-auth", version.ref = "credentials" }
googleid = { group = "com.google.android.libraries.identity.googleid", name = "googleid", version.ref = "googleid" }
```

Then:

1. Add all three to `dependencies {}`.
2. Put your **Web** OAuth client ID (not the Android one) in
   `local.properties` as `GOOGLE_WEB_CLIENT_ID`. It is already plumbed to
   `BuildConfig.GOOGLE_WEB_CLIENT_ID`.
3. Register your debug and release SHA-1 fingerprints on the Android OAuth
   client in the Google Cloud console.
4. Replace the body of `signIn()` with a `CredentialManager.getCredential`
   call built from `GetSignInWithGoogleOption` + `hashedNonce(rawNonce())`,
   and map the returned credential into the existing `GoogleSignInResult`.
   Nothing above it changes — `AuthService` and the auth screens already
   consume that type.

Until then the button routes to `MockAuthService` and signs in a seeded user,
which is exactly what the iOS mock does.

---

## 7. Two subtle things worth knowing

**`pfReadableWidth()` does more than `widthIn`.** A bare
`Modifier.fillMaxWidth().widthIn(max = 700.dp)` does *nothing*: `fillMaxWidth`
hands down a tight constraint, and `Constraints.constrain` clamps the cap
straight back up to the parent width. `pfReadableWidth` re-loosens with
`wrapContentWidth(CenterHorizontally)` before applying the cap — which is
also what centres the column instead of pinning it to the start edge. If you
add a screen, use the modifier, don't hand-roll `widthIn`.

**A shared PDF's URI grant is per-intent.** `ACTION_SEND` with
`application/pdf` gives a `content://` URI whose read permission lives as long
as the receiving activity. `ImportHubScreen` consumes it immediately, which is
fine. If you ever persist that URI across process death, call
`takePersistableUriPermission` first or the import will fail silently later.

---

## 8. Swapping the JSON store for Room

The port avoids annotation processors, but nothing about the shape of the code
resists Room. The seam is `PaperflipRepository` — every feature talks to it,
nothing talks to `PaperflipDatabase` directly.

1. Add `androidx.room:room-runtime`, `room-ktx`, and `ksp(room-compiler)`
   plus the KSP plugin.
2. Annotate the classes in `core/data/Models.kt` with `@Entity`. They are
   already flat data classes with primitive columns and stable `id` keys,
   chosen with this in mind.
3. Write `@Dao` interfaces returning `Flow<List<T>>` — which is what the
   repository already exposes, so the feature layer does not change.
4. Reimplement `PaperflipDatabase` on top of the DAOs and delete the JSON
   snapshot writer. Keep `PaperflipSnapshot`: `SyncEngine` uses it as its wire
   format, independently of storage.
5. Write a one-shot migration that reads the old JSON file, inserts it, then
   deletes the file.

`SyncEngine.merge()` is the part not to break: it preserves per-card SRS
state (interval, ease, due date) across a remote-driven rebuild by matching on
`id` first and then on lowercased front text. Losing that means every remote
sync silently resets everyone's scheduling.

---

## 9. Localization

`res/values/strings_generated.xml` — 521 strings — is generated from the iOS
`Localizable.xcstrings` catalogue, with `values-ar`, `values-fr` and
`values-es` alongside it. Conversions applied: `%@` → `%s`, `%lld` → `%d`,
positional (`%1$s`) whenever a string has more than one placeholder, and
reserved-word collisions renamed (`import_action`, `continue_action`,
`in_label`, `public_label`, `new_status`). That last one was missed on the
first pass and only surfaced at `aapt2` — see §2a before regenerating.

**Do not hand-edit `strings_generated.xml`.** Regenerate it from the iOS
catalogue so the two platforms cannot drift. Android-only copy — Play Store
wording, system-settings wording, widget and shortcut labels, notification
channels — lives in `strings.xml` and the small `strings_widget.xml` files,
which are hand-maintained.

Arabic ships, so RTL is real: the app sets `supportsRtl`, and layouts use
start/end padding throughout. A handful of `PFIcons` entries still point at
the non-auto-mirrored Material icon (`OpenInNew`, `TrendingUp`, `HelpOutline`)
and will emit deprecation warnings; moving them to `Icons.AutoMirrored.Filled.*`
fixes both the warning and their direction in Arabic. One file, three lines.

---

## 10. Fonts

iOS uses SF Pro Rounded for display text, which has no Android equivalent
shipped with the OS, so `PFType.kt` splits the two roles and routes every
display style through a single value:

```kotlin
val PFDisplayFamily: FontFamily = FontFamily.SansSerif   // ← change this line
val PFTextFamily: FontFamily = FontFamily.Default
```

To match the brand, drop a rounded face (Nunito, Quicksand or Varela Round
from Google Fonts) into `res/font/` and redefine `PFDisplayFamily`:

```kotlin
val PFDisplayFamily: FontFamily = FontFamily(
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold),
)
```

Leave `PFTextFamily` alone — body copy on the system font is correct on
Android, and it is what keeps the app legible in Arabic.

---

## 11. Known gaps

* **Not compiled.** Stated once more because it is the only thing on this
  list that matters.
* No unit or instrumentation tests. There is no `src/test` source set at all;
  `SM2`, `StudyQueue`, `StreakTracker` and `SyncEngine.merge` are pure Kotlin
  and are the obvious first four test classes.
* `RevenueCatEntitlementStore` and `SupabaseRemoteStore` have no Android
  implementations — the interfaces and the Mocks are there, and the iOS files
  of the same names are the reference for filling them in.
* R8 is disabled for release (`optimization { enable = false }`), as it was
  in the generated project. The rules it will need are already written —
  `app/src/main/keepRules/paperflip.keep` covers the `@Serializable` model
  graph, the Glance widget receiver and the manifest-declared broadcast
  receivers — so turning it on should be a one-line change plus a smoke test
  of sync, the widget and the daily reminder.
* App Links (`https://paperflip.ai/...`) declare `autoVerify="true"` but
  need `assetlinks.json` hosted on the domain to actually verify.
