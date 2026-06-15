package com.example.perbandingan_kinerja_flutter

import android.os.Debug
import android.system.Os
import android.system.OsConstants
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import java.io.BufferedReader
import java.io.FileReader

class MainActivity : FlutterActivity() {
    private val CHANNEL = "com.skripsi/metrics"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "getCpuTimeNanos" -> {
                        result.success(Debug.threadCpuTimeNanos())
                    }
                    "getProcessCpuTimeMs" -> {
                        result.success(getProcessCpuTimeMs())
                    }
                    else -> result.notImplemented()
                }
            }
    }

    private fun getProcessCpuTimeMs(): Double {
        return try {
            val ticksPerSecond = Os.sysconf(OsConstants._SC_CLK_TCK).toDouble()
            BufferedReader(FileReader("/proc/self/stat")).use { reader ->
                val line = reader.readLine() ?: return 0.0
                val closingParen = line.lastIndexOf(')')
                if (closingParen == -1) return 0.0
                val fields = line.substring(closingParen + 2).split(Regex("\\s+"))
                if (fields.size < 13) return 0.0
                val utime = fields[11].toLongOrNull() ?: return 0.0
                val stime = fields[12].toLongOrNull() ?: return 0.0
                (utime + stime) * 1000.0 / ticksPerSecond
            }
        } catch (_: Exception) {
            0.0
        }
    }
}
