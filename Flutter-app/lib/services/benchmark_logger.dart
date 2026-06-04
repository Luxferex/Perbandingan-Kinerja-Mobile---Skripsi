import '../models/benchmark_result.dart';
import '../utils/csv_export_helper.dart';

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

  String exportAsCsv() => buildBenchmarkCsvExport(_results);
}
