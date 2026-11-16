dependencies {
    api(project(":kottie-core"))
    api(project(":kottie-ecs-runtime"))
    api(project(":kottie-serialization"))
    api(project(":kottie-storage"))
    api(project(":kottie-packets"))
    api(project(":kottie-network"))
    api(project(":kottie-cluster"))
    api(project(":kottie-adapter-paper"))
    api(project(":kottie-adapter-velocity"))
    api(libs.junit.jupiter.api)
    implementation(libs.mockk)
    implementation(libs.kotlinx.serialization.json)
}
