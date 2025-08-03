plugins {
    kotlin("jvm")
    application
    id("com.google.cloud.tools.jib")
}

application {
    mainClass.set("com.example.app.ServerKt")
}

dependencies {
    implementation(project(":agent"))

    implementation("io.ktor:ktor-server-netty:2.3.5")
    implementation("io.ktor:ktor-server-core:2.3.5")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.5")
    implementation("io.ktor:ktor-serialization-jackson:2.3.5")
    implementation("io.ktor:ktor-server-call-logging:2.3.5")
    implementation("io.ktor:ktor-server-status-pages:2.3.5")

    implementation("ch.qos.logback:logback-classic:1.4.11")

    implementation("io.opentelemetry:opentelemetry-sdk:1.31.0")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp:1.31.0")

    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-tests-jvm:2.3.5")
    testImplementation("io.ktor:ktor-client-cio:2.3.5")
}

jib {
    to {
        image = "ghcr.io/OWNER/koog-voice-bot-demo:local"
    }
    container {
        ports = listOf("8080")
        jvmFlags = listOf("-Xms512m", "-Xmx512m")
    }
}
