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

If you're on plugin version 2.0.0 or later (see the section right below —
the REST controller ships from that version on), this is either a genuine
"no forms on this site yet" or a real error, and the app will now tell
the difference: a failed request shows a red banner on the Forms screen
naming the actual problem (e.g. a permission error, or the routes still
being unreachable), rather than the old behavior of silently looking
identical to an empty list. If you're on an **older plugin version**
without the REST controller, the banner will name the missing
`psbdx-srm/v1` routes specifically — update the plugin to fix that, it's
not an Application Password problem.

## ✅ Server-side REST controller — now implemented

`psbdx-srm/v1` (forms/responses/replies) is now real, as of the plugin's
`includes/class-psbdx-srm-rest-controller.php` — the section below is kept
for reference (route table, and the previous gap this closed) rather than
because it's still outstanding.

| Method | Route | Purpose |
|---|---|---|
| GET | `/psbdx-srm/v1/forms` | list forms |
| POST | `/psbdx-srm/v1/forms` | create a blank draft form |
| GET | `/psbdx-srm/v1/forms/{id}` | read one form |
| PUT | `/psbdx-srm/v1/forms/{id}/fields` | save field schema (used by field duplicate) |
| DELETE | `/psbdx-srm/v1/forms/{id}` | trash a form |
| GET | `/psbdx-srm/v1/forms/{id}/responses` | list responses |
| GET | `/psbdx-srm/v1/responses/{id}` | response detail + reply thread |
| PATCH | `/psbdx-srm/v1/responses/{id}/status` | change status |
| POST | `/psbdx-srm/v1/responses/{id}/replies` | send a reply (reuses the plugin's existing `PSBDX_SRM_Replies::add_reply()` + its automatic email notification) |

Permission model actually implemented (not `manage_options` as an earlier
draft of this README assumed): forms use WordPress's real per-post
capabilities — `edit_posts` to list, `edit_post`/`delete_post` on the
specific form to read/edit/delete it, and the post type's own
`create_posts` cap to create one — same floor as the classic admin menu
(Contributor-level `edit_posts`, not admin-only). Responses use
`PSBDX_SRM_Replies::can_access_report()` to view (mirrors the same rule the
shortcode-rendered thread already uses) and `edit_post` on the report to
change status or reply.

**Known limitations of this first server-side pass**, straight from the
controller's own docblock:
- Status-change/reply access is gated on `edit_post`, not the full Support
  Agent claim/assignment workflow (`PSBDX_SRM_Agents`) — any admin/editor
  can reply to any report, not just the one it's assigned to. A future pass
  could layer that check on top for parity with the classic admin's
  `handle_agent_reply()`.
- `answers` on a response is reconstructed by parsing the plugin's own
  HTML report summary (`post_content`) back into a label→value map — there
  is no separately-stored structured answers array anywhere in the plugin
  (the CSV exporter has this exact same limitation). Reliable for the
  plugin's own known output format, but not a real structured data source.
- `share_url` is always empty — the plugin has no page-level "this form
  lives at this URL" concept (forms are shortcodes/popup triggers placed
  anywhere), so there's nothing real to return. The Forms list hides the
  copy-share-link button entirely when it's blank rather than copying
  nothing.
- The Android app's `notify_email` flag on a new reply has no effect yet:
  the plugin always emails on reply (`psbdx_srm_reply_added` → `PSBDX_SRM_Emails::notify_reply()`),
  with no per-reply opt-out to wire it to.

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
