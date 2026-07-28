plugins {
    id("idealista.android.library")
    id("idealista.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

/**
 * The API fixtures live at the repo root as the single source of truth (they are byte-for-byte the
 * upstream payloads). Syncing them into test resources rather than keeping a second copy means a
 * schema drift shows up as a failing contract test instead of a stale duplicate nobody updated.
 */
val syncApiFixtures = tasks.register<Sync>("syncApiFixtures") {
    from(rootProject.layout.projectDirectory) {
        include("list.json", "detail.json")
    }
    // Synced into the standard test-resources dir (gitignored) rather than registered as an extra
    // source dir: AGP 9's new DSL source-set accessors are not usable from Kotlin DSL yet.
    into(layout.projectDirectory.dir("src/test/resources/fixtures"))
}

android {
    namespace = "dev.mkaminski.idealista.data"
}

// The fixtures must exist before AGP packages test resources.
tasks.matching { it.name.contains("UnitTestJavaRes") }.configureEach {
    dependsOn(syncApiFixtures)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(projects.core.model)

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.okhttp)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.serialization)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.turbine)
}
