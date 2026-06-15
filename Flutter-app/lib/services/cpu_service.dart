import 'package:flutter/services.dart';

class CpuService {
  static const _channel = MethodChannel('com.skripsi/metrics');

  static Future<double> getCpuTimeNanos() async {
    try {
      final nanos = await _channel.invokeMethod<int>('getCpuTimeNanos');
      return (nanos ?? 0).toDouble();
    } catch (e) {
      return 0.0;
    }
  }

  static Future<double> getProcessCpuTimeMs() async {
    try {
      final ms = await _channel.invokeMethod<double>('getProcessCpuTimeMs');
      return ms ?? 0.0;
    } catch (e) {
      return 0.0;
    }
  }

  static double calculateCpuPercent(
    double cpuBeforeMs,
    double cpuAfterMs,
    double wallTimeMs,
  ) {
    if (wallTimeMs < 1.0) return -1.0;
    final cpuTimeDiffMs = cpuAfterMs - cpuBeforeMs;
    if (cpuTimeDiffMs <= 0) return 0.0;
    final percent = (cpuTimeDiffMs / wallTimeMs) * 100.0;
    // Values >100% are possible on multi-core devices.
    return percent < 0.0 ? 0.0 : percent;
  }
}
