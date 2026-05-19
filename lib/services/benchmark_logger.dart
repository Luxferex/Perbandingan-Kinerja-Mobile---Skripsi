import '../models/benchmark_result.dart';

class BenchmarkLogger {
  final List<BenchmarkResult> _results = [];

  List<BenchmarkResult> get results => List.unmodifiable(_results);

  void addResult(BenchmarkResult result) {
    _results.add(result);
  }

  List<BenchmarkResult> getResults() {
    return List.unmodifiable(_results);
  }

  void clearResults() {
    _results.clear();
  }

  String exportAsCsv() {
    final buffer = StringBuffer('scenario,executionTimeMs,timestamp\n');

    for (final result in _results) {
      buffer.writeln(
        '${result.scenario},'
        '${result.executionTimeMs},'
        '${result.timestamp.toIso8601String()}',
      );
    }

    return buffer.toString();
  }
}
