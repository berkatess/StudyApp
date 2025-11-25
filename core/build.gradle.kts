plugins {
    kotlin("jvm")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit.junit)
    testImplementation(libs.junit.jupiter)
}