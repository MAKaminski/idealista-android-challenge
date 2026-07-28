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
    // `api` on purpose: features consume the theme and its widgets together, in both toolkits.
    api(libs.material)
    api(libs.androidx.appcompat)
    api(libs.androidx.constraintlayout)

    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.tooling.preview)
    debugApi(libs.androidx.compose.ui.tooling)
}
