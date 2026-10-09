// Top-level build file — PSRM Forms
// All plugin/dependency versions are pinned below and resolved from Google's
// and Maven Central's public repositories only (F-Droid build requirement:
// no proprietary Maven repos, e.g. no Firebase/Google Play Services BOMs).
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    id("com.google.devtools.ksp") version "1.9.24-1.0.20" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
