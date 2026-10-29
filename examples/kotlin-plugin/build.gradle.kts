dependencies {
    compileOnly(project(":kottie-api"))
    compileOnly(project(":kottie-ecs-runtime"))
    implementation(libs.kotlinx.coroutines.core)
}
