dependencies {
    api(project(":kottie-api"))
    api(project(":kottie-network"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.slf4j.api)
}
