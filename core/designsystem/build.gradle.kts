plugins {
    id("idealista.android.library")
}

android {
    namespace = "dev.mkaminski.idealista.designsystem"
}

dependencies {
    // `api` on purpose: features consume the theme and its Material widgets together.
    api(libs.material)
    api(libs.androidx.appcompat)
    api(libs.androidx.constraintlayout)
}
