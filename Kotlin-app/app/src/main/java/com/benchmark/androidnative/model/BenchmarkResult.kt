package com.benchmark.androidnative.model

import java.util.Date

data class BenchmarkResult(
    val run: Int,
    val framework: String = "kotlin",
    val scenario: String,
    val executionTimeMs: Double,
    val cpuPercent: Double,
    val memoryMb: Double,
    val timestamp: Date = Date(),
)
