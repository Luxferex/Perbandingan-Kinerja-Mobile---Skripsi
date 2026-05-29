import 'package:flutter/foundation.dart';

import '../models/benchmark_result.dart';
import '../utils/benchmark_utils.dart';

class BenchmarkSummaryProvider extends ChangeNotifier {
  final List<BenchmarkResult> _allResults = [];

  List<BenchmarkResult> get allResults => List.unmodifiable(_allResults);

  void addResult(BenchmarkResult result) {
    _allResults.add(result);
    notifyListeners();
  }

  void clearAll() {
    _allResults.clear();
    notifyListeners();
  }

  String getCsvExport() {
    final buffer = StringBuffer(
      'run,scenario,execution_time_ms,timestamp\n',
    );

    for (var i = 0; i < _allResults.length; i++) {
      final result = _allResults[i];
      buffer.writeln(
        '${i + 1},'
        '${result.scenario},'
        '${result.executionTimeMs.toStringAsFixed(2)},'
        '${formatCsvTimestamp(result.timestamp)}',
      );
    }

    return buffer.toString();
  }

  List<BenchmarkResult> _resultsForScenario(String scenario) {
    return _allResults
        .where((result) => result.scenario == scenario)
        .toList();
  }

  double getAverageForScenario(String scenario) {
    final results = _resultsForScenario(scenario);
    if (results.isEmpty) return 0;

    final total = results.fold<double>(
      0,
      (sum, result) => sum + result.executionTimeMs,
    );
    return total / results.length;
  }

  double getMinForScenario(String scenario) {
    final results = _resultsForScenario(scenario);
    if (results.isEmpty) return 0;

    return results
        .map((result) => result.executionTimeMs)
        .reduce((a, b) => a < b ? a : b);
  }

  double getMaxForScenario(String scenario) {
    final results = _resultsForScenario(scenario);
    if (results.isEmpty) return 0;

    return results
        .map((result) => result.executionTimeMs)
        .reduce((a, b) => a > b ? a : b);
  }
}
