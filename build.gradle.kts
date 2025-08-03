import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import io.gitlab.arturbosch.detekt.Detekt

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.0.0" apply false
    id("com.google.cloud.tools.jib") version "3.4.0" apply false
    id("org.jlleitschuh.gradle.ktlint") version "11.5.1" apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.1" apply false
}

allprojects {
    repositories { mavenCentral() }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "io.gitlab.arturbosch.detekt")

    extensions.configure<KotlinJvmProjectExtension> {
        jvmToolchain(21)
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    tasks.withType<Detekt> {
        jvmTarget = "20"
    }
}
