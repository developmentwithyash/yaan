rootProject.name = "yaan"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

// ── App Shell ──────────────────────────────────────────────────────────────
include(":composeApp")

// ── Shared KMP Bridge ──────────────────────────────────────────────────────
include(":shared")

// ── Core Modules ───────────────────────────────────────────────────────────
include(":core:common")
include(":core:domain")
include(":core:data")

// ── Feature Modules ────────────────────────────────────────────────────────
include(":feature:capture")
include(":feature:reminders")
include(":feature:notes")
include(":feature:tasks")
include(":feature:briefing")
include(":feature:memory")
include(":feature:voice")
include(":feature:actions")

// ── Backend Cluster ────────────────────────────────────────────────────────
include(":backend")

