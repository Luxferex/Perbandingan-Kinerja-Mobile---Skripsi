package com.benchmark.androidnative.util

import android.app.ActivityManager
import android.content.Context
import android.os.Process
import java.io.File
import java.io.RandomAccessFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BenchmarkResult(
    val scenario: String,
    val run: Int,
    val executionMs: Double,
    val cpuPercent: Float,
    val memoryMB: Float
)

class BenchmarkLogger {

    val results: MutableList<BenchmarkResult> = mutableListOf()

    private var startNano: Long = 0L

    fun startTimer() {
        startNano = System.nanoTime()
    }

    fun stopTimer(): Double {
        return (System.nanoTime() - startNano) / 1_000_000.0
    }

    fun getCurrentCpuPercent(): Float {
        return try {
            val start = readCpuTimes()
            Thread.sleep(100)
            val end = readCpuTimes()
            val idleDiff = end.first - start.first
            val totalDiff = end.second - start.second
            if (totalDiff <= 0L) 0f
            else ((totalDiff - idleDiff).toFloat() / totalDiff.toFloat()) * 100f
        } catch (_: Exception) {
            0f
        }
    }

    fun getCurrentMemoryMB(context: Context): Float {
        val activityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val pid = Process.myPid()
        val processMemoryInfo = activityManager.getProcessMemoryInfo(intArrayOf(pid))
        return processMemoryInfo[0].totalPss / 1024f
    }

    fun addResult(result: BenchmarkResult) {
        results.add(result)
    }

    fun clearResults() {
        results.clear()
    }

    fun exportToCsv(context: Context): String {
        val directory = context.getExternalFilesDir(null) ?: context.filesDir
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(directory, "benchmark_results_$timestamp.csv")

        file.bufferedWriter().use { writer ->
            writer.appendLine("scenario,run,executionMs,cpuPercent,memoryMB")
            results.forEach { result ->
                writer.appendLine(
                    "${result.scenario},${result.run},${result.executionMs}," +
                        "${result.cpuPercent},${result.memoryMB}"
                )
            }
        }

        return file.absolutePath
    }

    private fun readCpuTimes(): Pair<Long, Long> {
        RandomAccessFile("/proc/stat", "r").use { reader ->
            val line = reader.readLine() ?: return 0L to 0L
            val parts = line.trim().split(Regex("\\s+"))
            if (parts.size < 5 || parts[0] != "cpu") return 0L to 0L

            val idle = parts[4].toLongOrNull() ?: 0L
            val total = parts.drop(1).take(7).sumOf { it.toLongOrNull() ?: 0L }
            return idle to total
        }
    }
}
