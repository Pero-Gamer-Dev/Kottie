plugins {
    alias(libs.plugins.kotlin.jvm) apply false
}

allprojects {
    group = "io.kottie"
    version = "2.0.0"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "java-library")

    configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
        compilerOptions {
            freeCompilerArgs.add("-Xjvm-default=all")
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    dependencies {
        "implementation"(rootProject.libs.kotlin.stdlib)
        "testImplementation"(rootProject.libs.junit.jupiter)
        "testRuntimeOnly"(rootProject.libs.junit.jupiter.engine)
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    val isExample = name.startsWith("java-plugin") || name.startsWith("kotlin-plugin") ||
        path.contains("examples")
    val isTestkit = name == "kottie-testkit"

    // Library modules get maven-publish; examples/testkit stay local-only by default
    if (!isExample) {
        apply(plugin = "maven-publish")

        val sourcesJar by tasks.registering(Jar::class) {
            archiveClassifier.set("sources")
            from(project.extensions.getByType(JavaPluginExtension::class.java).sourceSets.getByName("main").allSource)
        }

        configure<PublishingExtension> {
            publications {
                create<MavenPublication>("maven") {
                    from(components["java"])
                    artifact(sourcesJar.get())
                    pom {
                        name.set(project.name)
                        description.set("Kottie ${project.name}")
                        url.set("https://github.com/example/kottie")
                        licenses {
                            license {
                                name.set("MIT License")
                                url.set("https://opensource.org/licenses/MIT")
                            }
                        }
                    }
                }
            }
            repositories {
                val repoUrl = findProperty("publishRepoUrl") as String?
                if (repoUrl != null) {
                    maven {
                        name = "external"
                        url = uri(repoUrl)
                        val user = findProperty("publishRepoUser") as String?
                        val pass = findProperty("publishRepoPassword") as String?
                        if (user != null && pass != null) {
                            credentials {
                                username = user
                                password = pass
                            }
                        }
                    }
                }
                mavenLocal()
            }
        }
    }

    // Examples should not fail the root build if they are samples only
    if (isExample) {
        tasks.matching { it.name == "test" }.configureEach {
            enabled = false
        }
    }
}
