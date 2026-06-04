package com.benchmark.androidnative.model

import java.util.Date

data class BenchmarkResult(
    val scenario: String,
    val executionTimeMs: Double,
    val timestamp: Date = Date(),
)
