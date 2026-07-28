plugins {
    id("idealista.android.library")
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "dev.mkaminski.idealista.designsystem"

    buildFeatures {
        compose = true
    }
}

dependencies {
    // AppLocales speaks in AppLanguage, so every consumer of the toolkit gets the type too.
    api(projects.core.model)

    // `api` on purpose: features consume the theme and its widgets together, in both toolkits.
    api(libs.material)
    api(libs.androidx.appcompat)
    api(libs.androidx.constraintlayout)
    api(libs.androidx.browser)

    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.tooling.preview)
    debugApi(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
