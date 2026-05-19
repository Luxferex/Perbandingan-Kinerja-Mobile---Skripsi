import 'package:flutter/foundation.dart';

import '../models/benchmark_result.dart';

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
    final buffer = StringBuffer('scenario,executionTimeMs,timestamp\n');

    for (final result in _allResults) {
      buffer.writeln(
        '${result.scenario},'
        '${result.executionTimeMs},'
        '${result.timestamp.toIso8601String()}',
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
