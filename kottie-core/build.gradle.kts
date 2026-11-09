dependencies {
    api(project(":kottie-api"))
    api(project(":kottie-coroutines"))
    api(project(":kottie-server-spi"))
    api(project(":kottie-ecs-runtime"))
    api(project(":kottie-config"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.slf4j.api)
}
