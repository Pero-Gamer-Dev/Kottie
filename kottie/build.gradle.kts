plugins {
    alias(libs.plugins.kotlin.jvm) apply false
}

allprojects {
    group = "io.kottie"
    version = "2.0.1"

    repositories {
        mavenCentral()
    }
}

subprojects {
    val isExamplesAggregate = path == ":examples"
    val isExampleLeaf = path.startsWith(":examples:")

    if (isExamplesAggregate) {
        // Parent container only — no compile/publish
        return@subprojects
    }

    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "java-library")

    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
        compilerOptions {
            freeCompilerArgs.add("-Xjvm-default=all")
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    // Prefer explicit compatibility over auto toolchain probes (broken partial JREs)
    tasks.withType<JavaCompile>().configureEach {
        options.release.set(17)
    }

    dependencies {
        "implementation"(rootProject.libs.kotlin.stdlib)
        "testImplementation"(rootProject.libs.junit.jupiter)
        "testRuntimeOnly"(rootProject.libs.junit.jupiter.engine)
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    if (!isExampleLeaf) {
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

    if (isExampleLeaf) {
        tasks.matching { it.name == "test" }.configureEach {
            enabled = false
        }
    }
}
