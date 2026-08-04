import 'package:flutter/services.dart';

/// Bridge ke native Android untuk metrik CPU proses.
/// Sumber data: `/proc/self/stat` (utime + stime), setara dengan Kotlin.
class CpuService {
  static const _channel = MethodChannel('com.skripsi/metrics');

  /// Waktu CPU thread saat ini (nanodetik) via Debug.threadCpuTimeNanos().
  static Future<double> getCpuTimeNanos() async {
    try {
      final nanos = await _channel.invokeMethod<int>('getCpuTimeNanos');
      return (nanos ?? 0).toDouble();
    } catch (e) {
      return 0.0;
    }
  }

  /// Akumulasi waktu CPU proses (utime+stime) dalam milidetik.
  static Future<double> getProcessCpuTimeMs() async {
    try {
      final ms = await _channel.invokeMethod<double>('getProcessCpuTimeMs');
      return ms ?? 0.0;
    } catch (e) {
      return 0.0;
    }
  }

  /// CPU% = (Δ CPU time / wall-clock time) × 100.
  /// Nilai >100% wajar di perangkat multi-core.
  static double calculateCpuPercent(
    double cpuBeforeMs,
    double cpuAfterMs,
    double wallTimeMs,
  ) {
    if (wallTimeMs < 1.0) return -1.0;
    final cpuTimeDiffMs = cpuAfterMs - cpuBeforeMs;
    if (cpuTimeDiffMs <= 0) return 0.0;
    final percent = (cpuTimeDiffMs / wallTimeMs) * 100.0;
    return percent < 0.0 ? 0.0 : percent;
  }
}
