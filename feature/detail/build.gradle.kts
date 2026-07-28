plugins {
    id("idealista.android.feature")
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "dev.mkaminski.idealista.feature.detail"

    // Compose is enabled here for one embedded component, not for the screen: the detail screen
    // itself stays XML because the challenge requires it (ADR-0006).
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.viewpager2)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)

    // ComponentActivity: the screenshot renderer needs a ViewTree lifecycle owner.
    testImplementation(libs.androidx.activity.compose)
}
