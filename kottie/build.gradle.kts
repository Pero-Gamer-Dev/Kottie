plugins {
    alias(libs.plugins.kotlin.jvm) apply false
}

allprojects {
    group = "io.kottie"
    version = "2.6.0"

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

    // Keep Java + Kotlin on the SAME bytecode target even when the CI JDK is 21.
    // Without this, compileJava defaults to the running JDK (21) while Kotlin stays on 17.
    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }

    configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
        jvmToolchain(17)
        compilerOptions {
            freeCompilerArgs.add("-Xjvm-default=all")
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.release.set(17)
        options.encoding = "UTF-8"
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
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
        // Tests run on the toolchain JVM (17), not necessarily the outer JDK 21
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
