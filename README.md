# PSRM Forms (Android)

An open-source companion app for the **PSBDx Smart Report Management** WordPress
plugin, targeted for F-Droid.

- Package: `com.psrm.forms.psbdx`
- License: GPL-3.0-or-later (see `LICENSE`)
- Kotlin, Jetpack Compose, MVVM, Room, Retrofit/Moshi, Coroutines/Flow
- Auth: WordPress **Application Passwords** (WP core 5.6+, HTTP Basic) — no
  OAuth app registration, no proprietary auth SDK

## F-Droid / dependency notes

Every dependency in `app/build.gradle.kts` is pulled from Google's or Maven
Central's public repositories: AndroidX (Compose, Room, Security-Crypto,
Navigation, Lifecycle), Retrofit/OkHttp/Moshi (Square, Apache-2.0), and
Kotlin coroutines. Nothing here touches Google Play Services, Firebase, or
any closed-source/telemetry SDK, so `settings.gradle.kts` also fails the
build (`FAIL_ON_PROJECT_REPOS`) if any module adds an extra repository —
that's deliberate, to keep a stray private/proprietary Maven repo from
sneaking into an F-Droid build.

## Application Passwords — requirements & Multisite notes

- Requires WordPress **5.6+** and **HTTPS**. WP disables Application
  Passwords by default over plain HTTP (there's a
  `wp_is_application_passwords_available` filter a site could use to
  override this, but assume it's off unless you know otherwise).
- The Application Password is generated per-user under **Users → Profile →
  Application Passwords** — it is a distinct value from that user's normal
  login password, formatted as space-separated groups
  (`xxxx xxxx xxxx xxxx xxxx xxxx`). Paste it exactly as generated.
- **WordPress Multisite**: Application Passwords work per-site exactly like
  a single install, but two things commonly trip this up —
  1. Enter the **specific site's URL** you want to manage forms on (e.g.
     `https://site2.example.com` or `https://example.com/site2`), not the
     network's main domain — each site has its own `/wp-json/`.
  2. A network admin can disable Application Passwords **network-wide**
     with the same `wp_is_application_passwords_available` filter, and
     some multisite hardening/security plugins do this by default. If
     login fails with "Application Passwords aren't enabled," check
     Network Settings and any such plugin before assuming the credential
     is wrong.
- The app runs a pre-flight check against the unauthenticated `/wp-json/`
  index before attempting login (`AuthRepository.login()`), specifically
  so these cases produce a distinct, actionable error instead of the same
  generic 401 a wrong password would give.

## ⚠️ Logged in but the Forms list is empty — read this first

This is expected, not a bug in Application Password handling, if the
plugin's REST routes below don't exist on your site yet. Login succeeds
because `wp/v2/users/me` is real WordPress core — but the forms list
calls `psbdx-srm/v1/forms`, which 404s until that namespace is added to
the plugin. The app now surfaces this distinctly (a red banner on the
Forms screen naming the missing routes) rather than just showing "No
forms yet," so if you see that banner, the fix is server-side, not a
credential problem.

## ⚠️ Server-side dependency — read before building against a real site

This app is written against a `psbdx-srm/v1` REST namespace
(`WordPressApi.kt`) for forms/responses/replies. **That namespace does not
exist in the plugin yet** — the plugin currently drives its own admin UI
over `admin-ajax.php`, not a REST controller. Only `wp/v2/users/me` (used
for login + the header) is real, working WordPress core today.

To make this app functional against a live site, the plugin needs a small
`WP_REST_Controller`-based addition exposing:

| Method | Route | Purpose |
|---|---|---|
| GET | `/psbdx-srm/v1/forms` | list forms |
| GET/POST | `/psbdx-srm/v1/forms/{id}` | read / create |
| PUT | `/psbdx-srm/v1/forms/{id}/fields` | save field schema (used by field duplicate) |
| DELETE | `/psbdx-srm/v1/forms/{id}` | delete |
| GET | `/psbdx-srm/v1/forms/{id}/responses` | list responses |
| GET | `/psbdx-srm/v1/responses/{id}` | response detail |
| PATCH | `/psbdx-srm/v1/responses/{id}/status` | change status |
| POST | `/psbdx-srm/v1/responses/{id}/replies` | send a reply (reuse the existing agent-reply/email pipeline) |

Each route should gate on the same WP capabilities the admin screens already
check (`manage_options` / `edit_others_posts` for form CRUD,
`moderate_comments` for responses), consistent with `AuthRepository`'s
capability checks.

## What's implemented in this scaffold

- **Auth** — `LoginScreen` → `AuthRepository.login()` verifies the
  Application Password via `wp/v2/users/me`, stores it encrypted
  (`CredentialStore`, Jetpack Security), and every request thereafter
  carries it via `WordPressAuthInterceptor` (HTTP Basic).
- **Header** — `PsrmTopBar` shows `username · site-host` on every screen
  (spec requirement), plus a logout action.
- **Offline-first forms list** — Room (`FormDao`) is the read source for
  `FormsListScreen`; `refresh()` pulls the server list into it. Per-form
  actions: edit fields, view responses, copy shortcode, copy share link,
  delete.
- **Form Builder — field duplicate** — `FormBuilderScreen` lists a form's
  fields with a per-card Duplicate button; `FormsRepository.duplicateField()`
  mirrors the PC editor's `duplicateField()` convention exactly (new ID,
  `"(Copy)"` label, re-derived handle, inserted right after the original)
  and persists the whole field list back to the server.
- **Responses + replies** — `ResponsesScreen` lists submissions with
  answers and reply threads; sending a reply (`repliesEnabled` gate) posts
  through `ResponsesRepository.sendReply()` with an email-notify flag.

## Before your first build

This sandbox has no JDK/Gradle/network access, so it could produce the
Gradle Kotlin scripts but not run Gradle itself — meaning the wrapper
binary (`gradle/wrapper/gradle-wrapper.jar`) and the `gradlew`/`gradlew.bat`
launcher scripts aren't included. `gradle-wrapper.properties` (pinning
Gradle 8.9) is already in place.

**CI needs nothing from you here** — `android-build.yml` has a "Generate
Gradle wrapper if missing" step that bootstraps `gradlew` on the runner
itself (via the `gradle` binary `gradle/actions/setup-gradle` puts on PATH)
whenever it isn't already committed, so a push works even before you've
generated the wrapper locally. That step is a one-time no-op once you do
commit the real wrapper files — it only acts when `gradlew` is missing.

For local development (Android Studio, or running Gradle from your own
machine), generate the wrapper once:

```bash
gradle wrapper --gradle-version 8.9
git add gradlew gradlew.bat gradle/wrapper/gradle-wrapper.jar
```

Android Studio does this automatically the first time you open the project,
if you'd rather not use the CLI.

## What's intentionally left as follow-up

- Full field *editing* (type/label/required/choices) in the mobile builder,
  drag-reorder, and multi-select/bulk actions — the mobile counterpart to
  the PC editor's Task 1 additions. The `PsrmField` model and repository
  are already shaped to support this; only the settings-sheet UI is missing.
  - Note: if bulk *field-level* multi-select is wanted on mobile too (not
    just the PC editor canvas), it slots into `FormBuilderScreen` the same
    way it was added there — long-press to enter selection mode, a
    bottom bar for Duplicate/Delete selected.
- Create-form flow (`onCreateForm` is a stub) — straightforward once
  `POST /psbdx-srm/v1/forms` exists server-side.
- Push notifications for new responses — deliberately not attempted here:
  a privacy-respecting, F-Droid-clean implementation (no FCM) would need
  either a self-hosted push relay or a polling `WorkManager` job with a
  visible battery-usage tradeoff; worth a dedicated design pass rather than
  bolting on now.
- Room migrations (currently `version = 1` — add a real `Migration` before
  ever shipping a schema change).
