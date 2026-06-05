import 'package:flutter/foundation.dart';

import '../models/benchmark_result.dart';
import '../utils/csv_export_helper.dart';

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

  void clearScenario(String scenario) {
    _allResults.removeWhere((result) => result.scenario == scenario);
    notifyListeners();
  }

  String getCsvExport() => buildBenchmarkCsvExport(_allResults);

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
