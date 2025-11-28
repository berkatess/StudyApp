plugins {
    id("com.android.library")
    kotlin("android")
    id("kotlin-kapt")

}

android {
    namespace = "com.ar.data"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":core"))

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.junit.v121)
    androidTestImplementation(libs.androidx.espresso.core.v361)

    implementation(libs.kotlinx.coroutines.core)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore.ktx)

    //Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
}