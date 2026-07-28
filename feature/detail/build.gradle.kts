plugins {
    id("idealista.android.feature")
}

android {
    namespace = "dev.mkaminski.idealista.feature.detail"
}

dependencies {
    implementation(libs.androidx.viewpager2)
}
