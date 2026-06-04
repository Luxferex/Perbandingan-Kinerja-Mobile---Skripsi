class BenchmarkResult {
  final String scenario;
  final double executionTimeMs;
  final double cpuUsagePercent;
  final double memoryUsageMb;
  final double memoryMb;
  final double cpuPercent;
  final DateTime timestamp;

  BenchmarkResult({
    required this.scenario,
    required this.executionTimeMs,
    this.cpuUsagePercent = 0.0,
    this.memoryUsageMb = 0.0,
    this.memoryMb = 0.0,
    this.cpuPercent = 0.0,
    DateTime? timestamp,
  }) : timestamp = timestamp ?? DateTime.now();
}
