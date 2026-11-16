dependencies {
    compileOnly(project(":kottie-api"))
    compileOnly(project(":kottie-ecs-runtime"))
    compileOnly(project(":kottie-cluster"))
    compileOnly(project(":kottie-network"))
    compileOnly(project(":kottie-config"))
    compileOnly(libs.kotlinx.coroutines.core)
}
