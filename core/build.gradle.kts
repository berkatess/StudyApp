plugins {
    kotlin("jvm")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.annotation.jvm)

    testImplementation(libs.junit.junit)
    testImplementation(libs.junit.jupiter)
}