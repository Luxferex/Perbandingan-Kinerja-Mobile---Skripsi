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

  static double calculateCpuPercent(
    double cpuTimeDiffNanos,
    double wallTimeMs,
  ) {
    if (wallTimeMs <= 0) return 0.0;
    final cpuTimeMs = cpuTimeDiffNanos / 1000000.0;
    final percent = (cpuTimeMs / wallTimeMs) * 100.0;
    return percent.clamp(0.0, 100.0);
  }
}
