dependencies {
    api(project(":kottie-core"))
    api(project(":kottie-ecs-runtime"))
    api(project(":kottie-serialization"))
    api(project(":kottie-storage"))
    api(project(":kottie-packets"))
    api(project(":kottie-network"))
    api(project(":kottie-adapter-paper"))
    api(libs.junit.jupiter.api)
    implementation(libs.mockk)
    implementation(libs.kotlinx.serialization.json)
}
