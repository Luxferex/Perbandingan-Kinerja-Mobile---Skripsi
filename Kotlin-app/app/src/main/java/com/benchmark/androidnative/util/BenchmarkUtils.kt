package com.benchmark.androidnative.util

import android.os.Process
import android.system.Os
import android.system.OsConstants
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

    /** Wall-clock time (ms) dari System.nanoTime() sebelum/sesudah operasi. */
    fun wallTimeMs(startNanos: Long, endNanos: Long): Double =
        (endNanos - startNanos) / 1_000_000.0

    fun elapsedMs(startMicros: Long, endMicros: Long): Double =
        (endMicros - startMicros) / 1000.0

    /**
     * Akumulasi waktu CPU proses dari `/proc/self/stat` (utime + stime).
     * Digunakan sebagai dasar hitung CPU%.
     */
    fun getProcessCpuTimeMs(): Double {
        return try {
            val ticksPerSecond = Os.sysconf(OsConstants._SC_CLK_TCK).toDouble()
            BufferedReader(FileReader("/proc/self/stat")).use { reader ->
                val line = reader.readLine() ?: return 0.0
                val closingParen = line.lastIndexOf(')')
                if (closingParen == -1) return 0.0
                val fields = line.substring(closingParen + 2).split(Regex("\\s+"))
                if (fields.size < 13) return 0.0
                // Field 14 = utime, field 15 = stime (0-indexed: 11, 12 setelah nama proses).
                val utime = fields[11].toLongOrNull() ?: return 0.0
                val stime = fields[12].toLongOrNull() ?: return 0.0
                (utime + stime) * 1000.0 / ticksPerSecond
            }
        } catch (_: Exception) {
            0.0
        }
    }

    /**
     * CPU% = (Δ CPU time / wall-clock time) × 100.
     * Nilai >100% wajar di perangkat multi-core (mis. 200% ≈ 2 core penuh).
     */
    fun calculateCpuPercent(
        cpuBeforeMs: Double,
        cpuAfterMs: Double,
        wallTimeMs: Double,
    ): Double {
        val cpuTimeDiffMs = cpuAfterMs - cpuBeforeMs
        return if (wallTimeMs > 1.0) {
            (cpuTimeDiffMs / wallTimeMs * 100.0).coerceAtLeast(0.0)
        } else {
            -1.0
        }
    }

    /**
     * Memori RSS (Resident Set Size) proses dari `/proc/[pid]/status` (VmRSS), dalam MB.
     * Menunjukkan memori fisik yang sedang dipakai aplikasi.
     */
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

    /** Gabungkan 3 metrik: wall-clock (ms), CPU%, memori RSS (MB). */
    fun collectMetrics(
        startNanos: Long,
        endNanos: Long,
        cpuBeforeMs: Double,
        cpuAfterMs: Double,
    ): Triple<Double, Double, Double> {
        val wallTimeMs = wallTimeMs(startNanos, endNanos)
        val cpuPercent = calculateCpuPercent(cpuBeforeMs, cpuAfterMs, wallTimeMs)
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
