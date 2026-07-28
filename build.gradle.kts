plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
}

/**
 * Screenshot generation for the README.
 *
 * There is no emulator in this project's development container, so screens are rasterised offscreen
 * by Robolectric's native graphics backend (see `Screenshots` in `:core:testing`). This task fetches
 * the real listing photos into a build directory first, then runs the opt-in screenshot tests with
 * the flag they wait for — so `./gradlew testDebugUnitTest` never rewrites committed images or
 * reaches for a CDN.
 *
 * Usage: ./gradlew screenshots
 */
val screenshotDir: Directory = layout.projectDirectory.dir("docs/screenshots")
val photoDir: Provider<Directory> = layout.buildDirectory.dir("screenshot-photos")

val fetchScreenshotPhotos = tasks.register("fetchScreenshotPhotos") {
    description = "Downloads the listing photos the screenshots render."
    val listJson = layout.projectDirectory.file("list.json")
    val target = photoDir
    inputs.file(listJson)
    outputs.dir(target)
    doLast {
        val out = target.get().asFile.apply { mkdirs() }
        // The URLs come from the committed payload, so this cannot drift from what the app loads.
        Regex("\"url\"\\s*:\\s*\"([^\"]+)\"|\"thumbnail\"\\s*:\\s*\"([^\"]+)\"")
            .findAll(listJson.asFile.readText())
            .mapNotNull { it.groupValues.drop(1).firstOrNull(String::isNotEmpty) }
            .distinct()
            .forEach { url ->
                val file = File(out, url.substringAfterLast('/'))
                if (file.exists()) return@forEach
                runCatching { file.writeBytes(java.net.URI(url).toURL().readBytes()) }
                    .onFailure { println("screenshots: could not fetch $url — a swatch will stand in") }
            }
    }
}

tasks.register("screenshots") {
    group = "documentation"
    description = "Renders the app's screens to docs/screenshots without a device."
    dependsOn(fetchScreenshotPhotos)
    dependsOn(subprojects.mapNotNull { it.tasks.findByName("testDebugUnitTest") })
}

subprojects {
    tasks.withType<Test>().configureEach {
        // Only the `screenshots` invocation sets these; every other run skips the generators.
        if (gradle.startParameter.taskNames.any { it.endsWith("screenshots") }) {
            systemProperty("idealista.screenshots", "true")
            systemProperty("idealista.screenshots.dir", screenshotDir.asFile.absolutePath)
            systemProperty("idealista.screenshots.photos", photoDir.get().asFile.absolutePath)
            outputs.upToDateWhen { false }
        }
    }
}
