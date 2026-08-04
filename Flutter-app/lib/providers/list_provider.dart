import 'dart:io';
import 'dart:async';

import 'package:flutter/widgets.dart';

import '../models/benchmark_result.dart';
import '../models/post_model.dart';
import '../services/cpu_service.dart';
import '../utils/benchmark_utils.dart';

/// Skenario Rendering: generate + render 1000 item ListView, ukur 3 metrik.
class ListProvider extends ChangeNotifier {
  final void Function(BenchmarkResult)? onResultRecorded;

  List<PostModel> _items = [];
  bool _isGenerated = false;
  bool _isLoading = false;
  double _executionTimeMs = 0;
  int _runCount = 0;
  int _progressRun = 0;
  final int _itemCount = 1000;
  final List<BenchmarkResult> _results = [];

  ListProvider({this.onResultRecorded});

  List<PostModel> get items => List.unmodifiable(_items);
  bool get isGenerated => _isGenerated;
  bool get isLoading => _isLoading;
  double get executionTimeMs => _executionTimeMs;
  int get runCount => _runCount;
  int get progressRun => _progressRun;
  int get itemCount => _itemCount;
  List<BenchmarkResult> get results => List.unmodifiable(_results);

  String? get lastResultCopyText {
    if (_results.isEmpty) return null;
    final last = _results.last;
    return formatResultLogLine(
      scenarioLabel: 'Rendering',
      runNumber: _runCount,
      executionTimeMs: last.executionTimeMs,
      timestamp: last.timestamp,
    );
  }

  void _recordResult(BenchmarkResult result) {
    _results.add(result);
    onResultRecorded?.call(result);
  }

  Future<void> runMultiple(int count) async {
    _isLoading = true;
    notifyListeners();

    try {
      for (var i = 0; i < count; i++) {
        _progressRun = i + 1;
        notifyListeners();
        await generateAndMeasure();
        if (i < count - 1) {
          await Future.delayed(const Duration(milliseconds: 500));
        }
      }
    } finally {
      _progressRun = 0;
      _isLoading = false;
      notifyListeners();
    }
  }

  /// Satu run skenario rendering: waktu dihitung sampai frame berikutnya ter-paint.
  Future<void> generateAndMeasure() async {
    final posts = generateDummyPosts(_itemCount);

    // Snapshot CPU sebelum render.
    final cpuBefore = await CpuService.getProcessCpuTimeMs();

    // Wall-clock: dari assign data sampai frame selesai digambar.
    final stopwatch = Stopwatch()..start();
    _items = posts;

    _isGenerated = true;

    // Stop timer hanya setelah frame berikutnya ter-paint (bukan sekadar setState).
    final completer = Completer<void>();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      completer.complete();
    });
    notifyListeners();
    await completer.future;

    stopwatch.stop();
    _executionTimeMs = stopwatch.elapsedMicroseconds / 1000.0;

    // CPU% dari Δ utime+stime / wall-clock.
    final cpuAfter = await CpuService.getProcessCpuTimeMs();
    final cpuPercent = CpuService.calculateCpuPercent(
      cpuBefore,
      cpuAfter,
      _executionTimeMs,
    );

    // RSS proses (MB).
    final memoryMb = ProcessInfo.currentRss / (1024 * 1024);
    _runCount++;

    _recordResult(
      BenchmarkResult(
        scenario: 'rendering',
        executionTimeMs: _executionTimeMs,
        cpuPercent: cpuPercent,
        memoryMb: memoryMb,
        timestamp: DateTime.now(),
      ),
    );

    notifyListeners();
  }

  void reset() {
    _items = [];
    _isGenerated = false;
    _isLoading = false;
    _executionTimeMs = 0;
    _runCount = 0;
    _progressRun = 0;
    _results.clear();
    notifyListeners();
  }
}
