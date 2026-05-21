import 'package:flutter/foundation.dart';

import '../models/benchmark_result.dart';
import '../models/post_model.dart';
import '../services/http_service.dart';
import '../utils/benchmark_utils.dart';

class HttpProvider extends ChangeNotifier {
  final HttpService _httpService;
  final void Function(BenchmarkResult)? onResultRecorded;

  List<PostModel> _posts = [];
  bool _isLoading = false;
  double _executionTimeMs = 0;
  int _runCount = 0;
  String? _error;
  final List<BenchmarkResult> _results = [];

  HttpProvider({
    HttpService? httpService,
    this.onResultRecorded,
  }) : _httpService = httpService ?? HttpService();

  List<PostModel> get posts => List.unmodifiable(_posts);
  bool get isLoading => _isLoading;
  double get executionTimeMs => _executionTimeMs;
  int get runCount => _runCount;
  String? get error => _error;
  List<BenchmarkResult> get results => List.unmodifiable(_results);

  String? get lastResultCopyText {
    if (_results.isEmpty) return null;
    final last = _results.last;
    return formatResultLogLine(
      scenarioLabel: 'HTTP',
      runNumber: _runCount,
      executionTimeMs: last.executionTimeMs,
      timestamp: last.timestamp,
    );
  }

  void _recordResult(BenchmarkResult result) {
    _results.add(result);
    onResultRecorded?.call(result);
  }

  Future<void> fetchAndMeasure() async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    final startTime = DateTime.now().microsecondsSinceEpoch;

    try {
      _posts = await _httpService.fetchPosts();
      final endTime = DateTime.now().microsecondsSinceEpoch;
      _executionTimeMs = elapsedMs(startTime, endTime);
      _runCount++;

      _recordResult(
        BenchmarkResult(
          scenario: 'http',
          executionTimeMs: _executionTimeMs,
          timestamp: DateTime.now(),
        ),
      );
    } catch (e) {
      _error = e.toString();
      _posts = [];
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  void reset() {
    _posts = [];
    _isLoading = false;
    _executionTimeMs = 0;
    _runCount = 0;
    _error = null;
    _results.clear();
    notifyListeners();
  }
}
