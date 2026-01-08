plugins {
    kotlin("jvm")
}

dependencies {
    implementation(kotlin("stdlib"))

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    api(project(":core"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.javax.inject)
}
