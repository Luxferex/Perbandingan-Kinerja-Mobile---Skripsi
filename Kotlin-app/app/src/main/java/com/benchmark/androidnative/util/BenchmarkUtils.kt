package com.benchmark.androidnative.util

import android.os.Process
import com.benchmark.androidnative.database.PostEntity
import com.benchmark.androidnative.model.BenchmarkResult
import java.io.BufferedReader
import java.io.FileReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BenchmarkUtils {
    const val MIN_RUNS = 5
    const val MAX_RUNS = 100
    const val INTER_RUN_DELAY_MS = 500L
    const val BENCHMARK_ITEM_COUNT = 1000

    fun formatMs(ms: Double): String =
        String.format(Locale.US, "%.2f ms", ms)

    fun wallTimeMs(startNanos: Long, endNanos: Long): Double =
        (endNanos - startNanos) / 1_000_000.0

    fun elapsedMs(startMicros: Long, endMicros: Long): Double =
        (endMicros - startMicros) / 1000.0

    fun calculateCpuPercent(
        cpuBeforeNanos: Long,
        cpuAfterNanos: Long,
        wallTimeMs: Double,
    ): Double {
        val cpuTimeDiffNanos = (cpuAfterNanos - cpuBeforeNanos).toDouble()
        val cpuTimeMs = cpuTimeDiffNanos / 1_000_000.0
        return if (wallTimeMs > 1.0) {
            (cpuTimeMs / wallTimeMs * 100.0).coerceIn(0.0, 100.0)
        } else {
            -1.0
        }
    }

    fun getMemoryRssMb(): Double {
        return try {
            val pid = Process.myPid()
            val reader = BufferedReader(FileReader("/proc/$pid/status"))
            var vmRss = 0.0
            reader.useLines { lines ->
                lines.forEach { line ->
                    if (line.startsWith("VmRSS:")) {
                        vmRss = line.replace(Regex("[^0-9]"), "")
                            .toDoubleOrNull() ?: 0.0
                    }
                }
            }
            vmRss / 1024.0
        } catch (_: Exception) {
            0.0
        }
    }

    fun collectMetrics(
        startNanos: Long,
        endNanos: Long,
        cpuBeforeNanos: Long,
        cpuAfterNanos: Long,
    ): Triple<Double, Double, Double> {
        val wallTimeMs = wallTimeMs(startNanos, endNanos)
        val cpuPercent = calculateCpuPercent(cpuBeforeNanos, cpuAfterNanos, wallTimeMs)
        val memoryMb = getMemoryRssMb()
        return Triple(wallTimeMs, cpuPercent, memoryMb)
    }

    fun generateDummyPosts(count: Int): List<PostEntity> {
        return (1..count).map { id ->
            PostEntity(
                id = id,
                userId = 1,
                title = "Item $id",
                body = "Body text for item $id",
            )
        }
    }

    fun formatCsvTimestamp(date: Date): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        return formatter.format(date)
    }

    fun formatResultLogLine(
        scenarioLabel: String,
        runNumber: Int,
        executionTimeMs: Double,
        cpuPercent: Double,
        memoryMb: Double,
        timestamp: Date,
    ): String {
        val time = SimpleDateFormat("H:m:s.S", Locale.US).format(timestamp)
        return "$scenarioLabel | Run $runNumber | " +
            "${String.format(Locale.US, "%.2f", executionTimeMs)} ms | " +
            "CPU ${String.format(Locale.US, "%.1f", cpuPercent)}% | " +
            "Mem ${String.format(Locale.US, "%.1f", memoryMb)} MB | [$time]"
    }

    fun buildCsvExport(results: List<BenchmarkResult>): String {
        val buffer = StringBuilder(
            "run,framework,scenario,execution_time_ms,cpu_percent,memory_mb,timestamp\n",
        )
        results.forEach { result ->
            buffer.append(result.run)
            buffer.append(',')
            buffer.append(result.framework)
            buffer.append(',')
            buffer.append(result.scenario)
            buffer.append(',')
            buffer.append(String.format(Locale.US, "%.2f", result.executionTimeMs))
            buffer.append(',')
            buffer.append(String.format(Locale.US, "%.1f", result.cpuPercent))
            buffer.append(',')
            buffer.append(String.format(Locale.US, "%.1f", result.memoryMb))
            buffer.append(',')
            buffer.append(formatCsvTimestamp(result.timestamp))
            buffer.append('\n')
        }
        return buffer.toString()
    }

    fun clampRuns(value: Int): Int = value.coerceIn(MIN_RUNS, MAX_RUNS)
}
