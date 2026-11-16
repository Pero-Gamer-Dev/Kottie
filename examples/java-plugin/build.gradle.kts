plugins {
    // inherits kotlin.jvm from root subprojects — keep Java sources compiling via java-library
}

dependencies {
    compileOnly(project(":kottie-api"))
    compileOnly(project(":kottie-ecs-runtime"))
}
