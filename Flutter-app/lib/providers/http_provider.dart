import 'dart:io';

import 'package:flutter/foundation.dart';

import '../models/benchmark_result.dart';
import '../models/post_model.dart';
import '../services/cpu_service.dart';
import '../services/http_service.dart';
import '../utils/benchmark_utils.dart';

class HttpProvider extends ChangeNotifier {
  final HttpService _httpService;
  final void Function(BenchmarkResult)? onResultRecorded;

  List<PostModel> _posts = [];
  bool _isLoading = false;
  double _executionTimeMs = 0;
  int _runCount = 0;
  int _progressRun = 0;
  String? _error;
  final List<BenchmarkResult> _results = [];
  bool _isWarmedUp = false;

  HttpProvider({
    HttpService? httpService,
    this.onResultRecorded,
  }) : _httpService = httpService ?? HttpService();

  List<PostModel> get posts => List.unmodifiable(_posts);
  bool get isLoading => _isLoading;
  double get executionTimeMs => _executionTimeMs;
  int get runCount => _runCount;
  int get progressRun => _progressRun;
  String? get error => _error;
  List<BenchmarkResult> get results => List.unmodifiable(_results);
  bool get isWarmedUp => _isWarmedUp;

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

  Future<void> warmUp() async {
    if (_isWarmedUp) return;
    try {
      if (kDebugMode) {
        debugPrint('[HttpProvider] Warm-up request...');
      }
      await _httpService.fetchPosts();
      _isWarmedUp = true;
      if (kDebugMode) {
        debugPrint('[HttpProvider] Warm-up selesai');
      }
    } catch (e) {
      if (kDebugMode) {
        debugPrint('[HttpProvider] Warm-up gagal: $e');
      }
    }
  }

  Future<void> runMultiple(int count) async {
    for (var i = 0; i < count; i++) {
      _progressRun = i + 1;
      notifyListeners();
      await fetchAndMeasure();
      if (i < count - 1) {
        await Future.delayed(const Duration(milliseconds: 500));
      }
    }
    _progressRun = 0;
    notifyListeners();
  }

  Future<void> fetchAndMeasure() async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    final cpuBefore = await CpuService.getCpuTimeNanos();
    final startTime = DateTime.now().microsecondsSinceEpoch;

    try {
      _posts = await _httpService.fetchPosts();
      final endTime = DateTime.now().microsecondsSinceEpoch;
      _executionTimeMs = elapsedMs(startTime, endTime);
      final cpuAfter = await CpuService.getCpuTimeNanos();
      final cpuPercent = CpuService.calculateCpuPercent(
        cpuAfter - cpuBefore,
        _executionTimeMs,
      );
      final memoryMb = ProcessInfo.currentRss / (1024 * 1024);
      _runCount++;

      _recordResult(
        BenchmarkResult(
          scenario: 'http',
          executionTimeMs: _executionTimeMs,
          cpuPercent: cpuPercent,
          memoryMb: memoryMb,
          timestamp: DateTime.now(),
        ),
      );
    } catch (e) {
      _error = e.toString();
      _posts = [];
      if (kDebugMode) {
        debugPrint('[HttpProvider] Benchmark gagal: $e');
      }
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
    _progressRun = 0;
    _error = null;
    _results.clear();
    notifyListeners();
  }
}
