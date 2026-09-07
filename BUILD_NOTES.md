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
| Unit tests (`:app:testDebugUnitTest`) | 43, see §2c — never executed here |
| Every string iOS renders exists on Android | 3 unmatched, all benign (§2d) |
| Swallowed errors on user-initiated actions | 9 found and fixed (§2e) |

Bugs these checks actually caught and that are now fixed: a missing
`getValue` import in `PFLottie`, a missing `@OptIn(ExperimentalFoundationApi)`
in `pfPressable`, a `widthIn` cap that an outer `fillMaxWidth()` silently
defeated (see §7), and a fully-qualified `LocalContext` left inline.

What these checks **cannot** see: type mismatches, wrong argument counts,
nullability, generic inference, and lambda-scope receivers. The first real
build proved the point — see §2a for exactly what got through.

**The sharpest blind spot is member access on the wrong type**, because the
symbol resolves and only the *field* is wrong. It has bitten once already:
`member.displayName` compiled fine in the author's head because
`PFProjectMember` has that field — but the variable was a
`ProjectMemberDto`, which mirrors the table, and the table has no such
column. The domain model gets `displayName`/`email` from the profile join;
the DTO never has them.

The rule this implies, worth applying to any new backend code: **a `*Dto`
carries exactly the table's columns, nothing more.** Anything richer comes
from `toModel()` or from the `memberProfiles` / `profiles` map that travels
alongside it. When a field feels like it should be there and isn't, that is
the DTO being right, not incomplete.

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

### 2b. Runtime-risk review (post-build)

Compiling clean says nothing about behaviour. A pass over the four areas
where a port most often looks right and behaves wrong found five defects.
All are fixed; they are recorded because four of the five are the *same
mistake* and it will recur in any new date code.

**The recurring one: fixed-millisecond day arithmetic.** `86_400_000L` is not
a day. It is a day except at DST transitions, where it is off by an hour —
and the iOS original used `Calendar.date(byAdding: .day)` everywhere, so
these were also silent divergences from the source of truth.

1. **`StreakTracker` wiped streaks after spring-forward.** "Was the last
   session yesterday?" was `isSameDay(last, now - 86_400_000)`. The day after
   a spring-forward is 23 hours long, so for any session between 00:00 and
   01:00 that lands on the day *before* yesterday — streak silently reset to
   1. Now uses `Calendar.add(DAY_OF_YEAR, -1)`, matching iOS.

2. **`SM2` due dates drifted an hour per DST change**, cumulatively over long
   intervals. Now schedules with calendar days.

3. **The daily reminder drifted permanently.** `setRepeating(INTERVAL_DAY)` is
   a fixed 24-hour period, so a 20:00 reminder becomes 19:00 after one DST
   change and stays there. Replaced with a one-shot `setAndAllowWhileIdle`
   that `ReminderReceiver` re-arms after each fire, recomputing the wall-clock
   time each day. As a side benefit the chain now self-heals:
   `refreshAuthorization()` re-applies it, so a force-stop (which clears
   pending alarms) or a permission granted after the toggle was switched on no
   longer leaves the reminder enabled-but-silent.

**And two that were not about dates:**

4. **The study card slid the wrong way in Arabic.** `Modifier.offset { }` is
   `rtlAware` — it places with `placeRelative`, so under RTL the card moved
   *away* from the finger, while the `rotationZ` lean (never mirrored) tilted
   the other way. Swipe gestures are physical, not directional, so the fix is
   `graphicsLayer { translationX / translationY }`, which is never mirrored —
   and skips a relayout on every frame. If you ever need a mirrored offset,
   `Modifier.absoluteOffset {}` is the non-mirroring twin; this file wants the
   graphicsLayer version.

5. **The reminder notification was English for everyone.** Title and body were
   hardcoded string literals. iOS has the same bug; it was not worth porting.
   Now `R.string.reminder_*`, translated into all four languages.

**Verified correct, no change needed:** `SM2`'s easiness-factor and interval
maths (including using the *new* ease for the interval, which is easy to get
backwards), `StudyQueue`'s 1:3 new-card interleave and session cap, and
`SyncEngine.preserveSrs` — which matches on `id` then lowercased front text,
so a remote-driven deck rebuild keeps every card's interval, ease and due
date. That last one is the single most damaging thing that could silently
break; it is worth a unit test before the first real sync.

**One product-level issue, fixed but worth knowing about:** the stored streak
was only rewritten when a session was recorded, so a user who studied twelve
days and then stopped kept seeing "12" forever — on the Stats screen and, far
more conspicuously, on the home-screen widget. `StreakTracker.count` now
reads 0 once the last session is older than yesterday, and `refreshWidget()`
re-evaluates on every resume. Nothing is mutated on read; `recordStudy` is
still the only writer, and it already handled the reset correctly. iOS has
the original behaviour and the same fix applies there.

### 2c. Unit tests

`app/src/test` — 43 tests over the four classes whose failure modes are
silent. Plain JUnit on the JVM: no Robolectric, no instrumentation, no
coroutine test library, so `./gradlew :app:testDebugUnitTest` runs in seconds
and can gate CI from day one.

| Test class | Covers |
|---|---|
| `SM2Test` | quality scale, easiness-factor maths and its 1.3 floor, the 1 / 6 / ×EF interval progression, `Again` restart, calendar-day due dates |
| `StudyQueueTest` | due-ness boundaries, most-overdue-first ordering, the 1:3 new-card interleave, session cap, mastery buckets |
| `StreakMathTest` | same-day / yesterday rules across both DST transitions, and streak decay |
| `SnapshotMergerTest` | SRS preservation by id and by front text, last-write-wins, local-only fields, offline-created decks, empty-payload ambiguity |

Three of these needed a small refactor to become testable, and the refactor is
worth keeping either way:

* `StreakMath` — the calendar rules, split out of the SharedPreferences-bound
  `StreakTracker`.
* `SnapshotMerger` — `merge` and `preserveSrs`, split out of `SyncEngine`
  (which holds a `PaperflipDatabase` and calls `android.util.Log`, neither of
  which works in a JVM test). `SyncEngine.merge` is now a one-line delegate.

Both are `internal`, which the `test` source set can still see — AGP wires it
as a friend module.

**Two tests are regression pins, not coverage.** They encode the DST bugs from
§2b as executable statements, including an assertion that the *old* arithmetic
would have failed, so re-introducing `86_400_000L` breaks the build rather
than someone's streak:

* `session on a spring-forward Sunday still counts as yesterday early Monday`
* `due date keeps its wall-clock hour across a DST transition`

Both pin `TimeZone.setDefault("America/New_York")` in `@Before` and restore it
in `@After`, and build every timestamp from local wall-clock fields. A test
written in UTC would pass against the broken code — which is exactly why the
bugs survived the first review.

**Note on `assertEquals` in Kotlin:** JUnit's `assertEquals(long, long)` will
not accept an `Int`, so a bare `assertEquals(999, card.srsDueDate)` silently
resolves to the `Any?` overload and compares `Integer` to `Long` — a test that
fails for a reason that has nothing to do with the code. Long literals in
assertions carry the `L`.

### 2d. Content-parity audit

Two automated passes over the iOS source and the Android string corpus,
because "full parity" was asserted from file names and never actually
checked.

**Pass 1 — does every string iOS displays exist somewhere on Android?**
Extract every literal iOS renders (`Text`, `Label`, `Button`,
`.navigationTitle`, `.alert`, `.confirmationDialog`, and the labelled
initialisers), normalise both sides, then look for a match anywhere in
Android's 998-string corpus. **Result: 3 unmatched**, and two of those were a
Swift interpolation and an email placeholder. Copy parity is effectively
complete.

**Pass 2 — is every ported string actually rendered?** Sharper, and it is the
one that found real bugs: any key in `strings_generated.xml` that no Kotlin or
XML file references is copy that exists on iOS and appears nowhere on Android.
That was **81 of 521**, now 72. The remainder is accounted for:

* ~16 are correctly absent — Apple-billing wording, "Open iOS Settings", iOS
  notification authorisation levels (`provisional`, `ephemeral`, App Clips),
  App Intents descriptions. Android has its own wording for each.
* ~13 belong to `DesignSystemPreview.swift`, a developer-only gallery screen
  that was deliberately not ported.
* 4 (`%d-week/-month/-year free trial`) are waiting on
  `RevenueCatEntitlementStore`, which has no Android implementation yet
  (§11). They are already translated — whoever wires RevenueCat should use
  them rather than hardcoding a day-based trial, which is the shape the mock
  happens to return.

**What it actually caught: five destructive actions that iOS confirms and
Android performed immediately, with no undo.**

| Action | Was | Now |
|---|---|---|
| Delete folder | deleted on tap | confirms, and says decks move to "All" rather than being deleted |
| Remove project member | removed on tap | confirms, naming the member and project |
| Revoke sent invite | revoked on tap | confirms, naming the invitee |
| Decline join request | declined on tap | confirms, noting they can request again |
| Remove profile picture | removed on tap | confirms |

Delete-project and leave-project were already confirmed, which is what made
the omissions easy to miss by reading: the file *looked* like it handled
confirmation properly.

All five now build a `PFConfirmation` and render through the screen's
`PFConfirmationHost`, matching the existing pattern. Two screens needed the
host adding. Note the mechanical constraint that shapes this code:
`stringResource` cannot be called inside a non-composable `onClick` lambda, so
any message needing runtime arguments resolves its format template in the
composable body and calls `String.format` at the point of use.

**Not ported deliberately:** the iOS accessibility hints
("Double tap to open", "Double tap to reveal the answer"). TalkBack already
announces "double-tap to activate" for anything with a click action, so
repeating it is noise. The Android equivalents are `onClick(label = …)` and
the custom actions already on `StudyCard`.

### 2e. Failure-path audit

Same trick as §2d — ask a different question of the same code. This one:
*what does the user see when a call fails?* Every `runCatching { }` with no
`.onFailure`, `.fold` or result binding is a swallowed error.

31 such blocks. Most are legitimately best-effort (registering a network
callback, requesting focus, updating the widget). Nine were not.

**Two were actively misleading, not merely silent:**

* **"Restore purchases" toasted success unconditionally.** `Manage
  Subscription` fired `PFToast.info("Restore purchases")` after the call
  regardless of outcome, so a subscriber whose restore threw was told it had
  worked. On the paywall the same button said nothing at all, leaving "no
  purchase found" indistinguishable from "the network failed". Both now branch
  three ways — restored / nothing found / failed — and the first checks that
  the entitlement actually went active rather than assuming it. iOS has done
  this since day one (`PFToast.success` / `PFToast.error` around a `do/catch`).

* **Accepting an invite navigated on failure.** `PendingInvitesScreen` removed
  the invite from the list and called `onOpenProject(...)` outside the
  `runCatching`, so a failed accept dropped the invite *and* pushed the user
  into a project they had no access to. Approving a join request had the same
  shape: the row vanished whether or not the person was approved. Both now
  mutate and navigate only on success.

**Seven were silent where iOS is not:** role change, remove member, resend
invite, revoke invite, decline invite, decline request, approve request. All
now surface `couldnt_complete_that` with the underlying message.

**And one channel had no consumer at all:** `SyncEngine` records `lastError`
("Offline — using cached data", the failure message) and *nothing read it*.
A failed pull-to-refresh in the library looked exactly like a successful one
with nothing new. `LibraryHomeScreen` now watches it and warns.

The shape used throughout, because it is the one that composes with the
early-return-on-failure requirement:

```kotlin
val result = runCatching { … }
processingId = null
if (result.isSuccess) { mutateUi() } else { PFToast.error(label, result.exceptionOrNull()?.message) }
```

`.onFailure { }` alone is fine where the only thing missing was feedback, but
not where UI mutation has to be conditional — which was the actual bug in the
two cases above.

**Related, and checked while here:** Pro gating is at parity. Both platforms
define `cardCap` (20 free / 50 Pro) and `monthlyDeckCap` (5 free / unlimited),
and *neither* enforces the monthly cap client-side — it is a server
concern, surfaced as `RemoteStoreError.PlanLimitReached`. Android has that
error case and the `%d-week/-month/-year free trial` strings already
translated, both waiting on the RevenueCat implementation.

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

## 5a. Supabase is live

`AppEnvironment` now builds real services when `local.properties` carries
`SUPABASE_URL` + `SUPABASE_ANON_KEY`, and Mocks when it doesn't — so a fresh
clone still runs fully offline, exactly like the iOS target without
`Config.xcconfig`. Project `bkixauovgbcinaagmusi`.

**Three files, one dependency.**

* `core/network/SupabaseClient.kt` — PostgREST, Edge Functions and Storage
  over OkHttp. Not the official Kotlin SDK: it pulls Ktor plus a dozen
  transitive modules, and everything here is REST with two headers. Against a
  build that cannot be verified locally, one artifact is a far better trade
  than a dependency graph.
* `core/network/SupabaseRemoteStore.kt` — all 38 `RemoteStore` methods.
* `core/auth/SupabaseAuthService.kt` — all 13 `AuthService` methods against
  GoTrue.

**Why auth is constructed first.** The REST client resolves its bearer token
from the auth service on *every* request, through a `suspend` lambda rather
than a captured value, because that is where an expiring token gets
refreshed. `AppEnvironment` therefore holds `supabaseAuth` as its own field
instead of reading it back off the `AuthService` interface.

**The refresh is mutex-guarded on purpose.** Supabase rotates the refresh
token on every use, so two parallel refreshes mean the second invalidates the
first and the user is silently signed out. `validAccessToken()` refreshes
inside a lock and re-checks after acquiring it, so a burst of concurrent
requests produces exactly one refresh.

**Two schema traps worth knowing**, both inherited from iOS:

* Storage paths must use the **lowercased** user id. Postgres renders
  `auth.uid()::text` in lowercase and the bucket policy compares it as text,
  so an uppercase UUID fails every upload.
* `createProject` is three steps — mint the id locally, insert, add the owner
  membership row, then read back. The SELECT policy on `projects` requires
  membership, so a `return=representation` insert would write the row and then
  fail to read it.

**What is still mocked:** `EntitlementStore`. Pro state is local until
Play Billing / RevenueCat exists, even though `profiles.plan` is the server's
truth and `snapshot()` already fetches it. Reading the entitlement off the
profile would be a small, real improvement before billing lands.

### Security fix applied to the database

`increment_deck_generation(uuid)` and `get_decks_generated_this_month(uuid)`
were `SECURITY DEFINER`, took an arbitrary user id, and had `EXECUTE` granted
to `anon`. Anyone holding the anon key — which ships inside every client
binary — could `POST /rest/v1/rpc/increment_deck_generation` with any user's
UUID and exhaust their monthly quota, unauthenticated. Both are now
`service_role` only; the `generate-flashcards` function that legitimately
calls them is unaffected.

`is_project_member()` and `project_role_for_user()` carry the same advisor
warning and were deliberately **left alone**: they are RLS helpers, and
Postgres evaluates policy expressions as the invoking role, so revoking
`EXECUTE` there breaks row-level security rather than tightening it. Don't
"fix" all five at once.

Still open on the project: leaked-password protection is disabled (one toggle
in Auth settings), and `handle_new_user()` is still callable by `anon` — it is
a trigger function that should not be reachable over the API, but revoking it
needs a signup test first.

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
* No instrumentation or Compose UI tests. `src/test` now covers the four
  pure-logic classes (§2c); nothing exercises navigation, the widget, or a
  screen. `PFButton`, `StudyCard` and `MainScaffold`'s route handling are the
  places a Compose test would earn its keep first.
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
