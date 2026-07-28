pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

// Provisions the JDK the build asks for instead of relying on whatever is installed, so the
// project builds identically on a laptop and in CI.
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google()
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "idealista-challenge"

include(":app")
include(":core:model")
include(":core:data")
include(":core:designsystem")
include(":core:testing")
include(":feature:list")
include(":feature:detail")
include(":feature:favorites")
