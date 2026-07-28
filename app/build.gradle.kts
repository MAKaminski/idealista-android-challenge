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
}
