plugins {
    id("idealista.android.application")
    id("idealista.android.hilt")
}

android {
    namespace = "dev.mkaminski.idealista"

    defaultConfig {
        applicationId = "dev.mkaminski.idealista"
        versionCode = 1
        versionName = "1.0"
    }
}

/**
 * `installDebug` only installs — it does not start the app, and `adb` is often not on PATH. This
 * task does both, so seeing a change run is one command with no manual step.
 */
val adbPath: String = listOfNotNull(
    rootProject.file("local.properties").takeIf { it.exists() }
        ?.readLines()
        ?.firstOrNull { it.startsWith("sdk.dir=") }
        ?.substringAfter("="),
    System.getenv("ANDROID_HOME"),
    System.getenv("ANDROID_SDK_ROOT"),
).firstOrNull()?.let { "$it/platform-tools/adb" } ?: "adb"

tasks.register<Exec>("runDebug") {
    group = "install"
    description = "Installs the debug APK and launches it on the connected device or emulator."
    dependsOn("installDebug")
    commandLine(
        adbPath,
        "shell",
        "am",
        "start",
        "-n",
        "dev.mkaminski.idealista/.MainActivity",
    )
}

dependencies {
    implementation(projects.core.data)
    implementation(projects.core.designsystem)
    implementation(projects.core.model)
    implementation(projects.feature.detail)
    implementation(projects.feature.favorites)
    implementation(projects.feature.list)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.material)

    // Instrumented only: authored here, executed on a device or emulator (docs/TESTING.md).
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.espresso.contrib)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
}
