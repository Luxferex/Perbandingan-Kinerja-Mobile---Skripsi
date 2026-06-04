package com.benchmark.androidnative.util

import com.benchmark.androidnative.database.PostEntity
import com.benchmark.androidnative.model.BenchmarkResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BenchmarkUtils {
    const val MIN_RUNS = 5
    const val MAX_RUNS = 100
    const val INTER_RUN_DELAY_MS = 500L
    const val BENCHMARK_ITEM_COUNT = 1000

    fun formatMs(ms: Double): String = "${"%.2f".format(ms)} ms"

    fun elapsedMs(startMicros: Long, endMicros: Long): Double =
        (endMicros - startMicros) / 1000.0

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
        timestamp: Date,
    ): String {
        val time = SimpleDateFormat("H:m:s.S", Locale.US).format(timestamp)
        return "$scenarioLabel | Run $runNumber | " +
            "${"%.2f".format(executionTimeMs)} ms | [$time]"
    }

    fun buildCsvExport(results: List<BenchmarkResult>): String {
        val buffer = StringBuilder("run,scenario,execution_time_ms,timestamp\n")
        results.forEachIndexed { index, result ->
            buffer.append(
                "${index + 1}," +
                    "${result.scenario}," +
                    "${"%.2f".format(result.executionTimeMs)}," +
                    "${formatCsvTimestamp(result.timestamp)}\n",
            )
        }
        return buffer.toString()
    }

    fun clampRuns(value: Int): Int = value.coerceIn(MIN_RUNS, MAX_RUNS)
}
