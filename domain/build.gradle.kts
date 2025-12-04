plugins {
    kotlin("jvm")
}

dependencies {
    implementation(kotlin("stdlib"))

    api(project(":core"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.javax.inject)
}
