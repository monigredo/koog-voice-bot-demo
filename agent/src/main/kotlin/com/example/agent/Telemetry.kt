package com.example.agent

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor
import io.opentelemetry.sdk.trace.export.SpanExporter
import io.opentelemetry.exporter.logging.LoggingSpanExporter
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter

/**
 * Configures OpenTelemetry with logging and OTLP exporters.
 */
object Telemetry {
    val openTelemetry: OpenTelemetry by lazy {
        val exporters = listOf<SpanExporter>(
            LoggingSpanExporter.create(),
            OtlpGrpcSpanExporter.builder().setEndpoint("http://localhost:4317").build()
        )
        val tracerProvider = SdkTracerProvider.builder().apply {
            for (exporter in exporters) {
                addSpanProcessor(BatchSpanProcessor.builder(exporter).build())
            }
            setResource(
                Resource.create(
                    Attributes.of(
                        AttributeKey.stringKey("service.name"), "koog-voice-bot-demo",
                        AttributeKey.stringKey("service.version"), "0.1.0"
                    )
                )
            )
        }.build()
        val sdk = OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).build()
        GlobalOpenTelemetry.set(sdk)
        sdk
    }

    val tracer: Tracer by lazy { openTelemetry.getTracer("koog-voice-bot-demo") }
}
