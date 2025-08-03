plugins {
    kotlin("jvm")
}

dependencies {
    api("io.ktor:ktor-client-cio:2.3.5")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.5")
    implementation("io.ktor:ktor-serialization-jackson:2.3.5")

    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("ch.qos.logback:logback-classic:1.4.11")

    implementation("io.opentelemetry:opentelemetry-sdk:1.31.0")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp:1.31.0")
    implementation("io.opentelemetry:opentelemetry-extension-trace-propagators:1.31.0")
    implementation("io.opentelemetry:opentelemetry-exporter-logging:1.31.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-client-mock:2.3.5")
}
