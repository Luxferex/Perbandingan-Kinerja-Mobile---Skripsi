import '../models/benchmark_result.dart';
import '../utils/benchmark_utils.dart';

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
    final buffer = StringBuffer(
      'run,scenario,execution_time_ms,timestamp\n',
    );

    for (var i = 0; i < _results.length; i++) {
      final result = _results[i];
      buffer.writeln(
        '${i + 1},'
        '${result.scenario},'
        '${result.executionTimeMs.toStringAsFixed(2)},'
        '${formatCsvTimestamp(result.timestamp)}',
      );
    }

    return buffer.toString();
  }
}
