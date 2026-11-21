pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "kottie"

include(
    "kottie-api",
    "kottie-core",
    "kottie-coroutines",
    "kottie-server-spi",
    "kottie-ecs-api",
    "kottie-ecs-runtime",
    "kottie-config",
    "kottie-serialization",
    "kottie-storage",
    "kottie-packets",
    "kottie-network",
    "kottie-cluster",
    "kottie-adapter-paper",
    "kottie-adapter-velocity",
    "kottie-testkit",
    "examples:kotlin-plugin",
    "examples:java-plugin"
)
