import 'package:flutter/foundation.dart';

import '../models/benchmark_result.dart';
import '../models/post_model.dart';
import '../utils/benchmark_utils.dart';

class ListProvider extends ChangeNotifier {
  final void Function(BenchmarkResult)? onResultRecorded;

  List<PostModel> _items = [];
  bool _isGenerated = false;
  double _executionTimeMs = 0;
  int _runCount = 0;
  final int _itemCount = 1000;
  final List<BenchmarkResult> _results = [];

  ListProvider({this.onResultRecorded});

  List<PostModel> get items => List.unmodifiable(_items);
  bool get isGenerated => _isGenerated;
  double get executionTimeMs => _executionTimeMs;
  int get runCount => _runCount;
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

  Future<void> generateAndMeasure() async {
    final startTime = DateTime.now().microsecondsSinceEpoch;

    _items = generateDummyPosts(_itemCount);

    final endTime = DateTime.now().microsecondsSinceEpoch;
    _executionTimeMs = elapsedMs(startTime, endTime);
    _isGenerated = true;
    _runCount++;

    _recordResult(
      BenchmarkResult(
        scenario: 'rendering',
        executionTimeMs: _executionTimeMs,
        timestamp: DateTime.now(),
      ),
    );

    notifyListeners();
  }

  void reset() {
    _items = [];
    _isGenerated = false;
    _executionTimeMs = 0;
    _runCount = 0;
    _results.clear();
    notifyListeners();
  }
}
